package io.zonarosa.service.api.services;

import io.zonarosa.core.util.Hex;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.logging.Log;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.profiles.ClientZkProfileOperations;
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredential;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialRequest;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyCredentialRequestContext;
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKeyVersion;
import io.zonarosa.service.api.crypto.SealedSenderAccess;
import io.zonarosa.service.api.profiles.ProfileAndCredential;
import io.zonarosa.service.api.profiles.ZonaRosaServiceProfile;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.MalformedResponseException;
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket;
import io.zonarosa.service.internal.ServiceResponse;
import io.zonarosa.service.internal.ServiceResponseProcessor;
import io.zonarosa.service.internal.push.IdentityCheckRequest;
import io.zonarosa.service.internal.push.IdentityCheckRequest.ServiceIdFingerprintPair;
import io.zonarosa.service.internal.push.IdentityCheckResponse;
import io.zonarosa.service.internal.push.http.AcceptLanguagesUtil;
import io.zonarosa.service.internal.util.JsonUtil;
import io.zonarosa.service.internal.websocket.DefaultResponseMapper;
import io.zonarosa.service.internal.websocket.ResponseMapper;
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import kotlin.Pair;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;

/**
 * Provide Profile-related API services, encapsulating the logic to make the request, parse the response,
 * and fallback to appropriate WebSocket alternatives.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ProfileService {

  private static final String TAG = ProfileService.class.getSimpleName();

  private final ClientZkProfileOperations                clientZkProfileOperations;
  private final ZonaRosaWebSocket.AuthenticatedWebSocket   authWebSocket;
  private final ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket;

  public ProfileService(ClientZkProfileOperations clientZkProfileOperations,
                        ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket,
                        ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket)
  {
    this.clientZkProfileOperations = clientZkProfileOperations;
    this.authWebSocket             = authWebSocket;
    this.unauthWebSocket           = unauthWebSocket;
  }

  public Single<ServiceResponse<ProfileAndCredential>> getProfile(@Nonnull ZonaRosaServiceAddress address,
                                                                  @Nonnull Optional<ProfileKey> profileKey,
                                                                  @Nullable SealedSenderAccess sealedSenderAccess,
                                                                  @Nonnull ZonaRosaServiceProfile.RequestType requestType,
                                                                  @Nonnull Locale locale)
  {
    ServiceId                          serviceId      = address.getServiceId();
    SecureRandom                       random         = new SecureRandom();
    ProfileKeyCredentialRequestContext requestContext = null;

    WebSocketRequestMessage.Builder builder = new WebSocketRequestMessage.Builder()
                                                                         .id(random.nextLong())
                                                                         .verb("GET");

    if (profileKey.isPresent()) {
      if (!(serviceId instanceof ACI)) {
        Log.w(TAG, "ServiceId  must be an ACI if a profile key is available!");
        return Single.just(ServiceResponse.forUnknownError(new IllegalArgumentException("ServiceId  must be an ACI if a profile key is available!")));
      }

      ACI               aci                  = (ACI) serviceId;
      ProfileKeyVersion profileKeyIdentifier = profileKey.get().getProfileKeyVersion(aci.getLibZonaRosaAci());
      String            version              = profileKeyIdentifier.serialize();

      if (requestType == ZonaRosaServiceProfile.RequestType.PROFILE_AND_CREDENTIAL) {
        requestContext = clientZkProfileOperations.createProfileKeyCredentialRequestContext(random, aci.getLibZonaRosaAci(), profileKey.get());

        ProfileKeyCredentialRequest request           = requestContext.getRequest();
        String                      credentialRequest = Hex.toStringCondensed(request.serialize());

        builder.path(String.format("/v1/profile/%s/%s/%s?credentialType=expiringProfileKey", serviceId, version, credentialRequest));
      } else {
        builder.path(String.format("/v1/profile/%s/%s", serviceId, version));
      }
    } else {
      builder.path(String.format("/v1/profile/%s", address.getIdentifier()));
    }

    builder.headers(Collections.singletonList(AcceptLanguagesUtil.getAcceptLanguageHeader(locale)));

    WebSocketRequestMessage requestMessage = builder.build();

    ResponseMapper<ProfileAndCredential> responseMapper = DefaultResponseMapper.extend(ProfileAndCredential.class)
                                                                               .withResponseMapper(new ProfileResponseMapper(requestType, requestContext))
                                                                               .build();

    if (sealedSenderAccess == null) {
      return authWebSocket.request(requestMessage)
                          .map(responseMapper::map)
                          .onErrorReturn(ServiceResponse::forUnknownError);
    } else {
      return unauthWebSocket.request(requestMessage, sealedSenderAccess)
                            .flatMap(response -> {
                              if (response.getStatus() == 401) {
                                return authWebSocket.request(requestMessage);
                              } else {
                                return Single.just(response);
                              }
                            })
                            .map(responseMapper::map)
                            .onErrorReturn(ServiceResponse::forUnknownError);
    }
  }

  public @NonNull Single<ServiceResponse<IdentityCheckResponse>> performIdentityCheck(@Nonnull Map<ServiceId, IdentityKey> serviceIdIdentityKeyMap) {
    List<ServiceIdFingerprintPair> serviceIdKeyPairs = serviceIdIdentityKeyMap.entrySet()
                                                                              .stream()
                                                                              .map(e -> new ServiceIdFingerprintPair(e.getKey(), e.getValue()))
                                                                              .collect(Collectors.toList());

    IdentityCheckRequest request = new IdentityCheckRequest(serviceIdKeyPairs);

    WebSocketRequestMessage.Builder builder = new WebSocketRequestMessage.Builder()
                                                                         .id(new SecureRandom().nextLong())
                                                                         .verb("POST")
                                                                         .path("/v1/profile/identity_check/batch")
                                                                         .headers(Collections.singletonList("content-type:application/json"))
                                                                         .body(JsonUtil.toJsonByteString(request));

    ResponseMapper<IdentityCheckResponse> responseMapper = DefaultResponseMapper.getDefault(IdentityCheckResponse.class);

    return unauthWebSocket.request(builder.build())
                          .map(responseMapper::map)
                          .onErrorReturn(ServiceResponse::forUnknownError);
  }

  /**
   * Maps the API {@link ZonaRosaServiceProfile} model into the desired {@link ProfileAndCredential} domain model.
   */
  private class ProfileResponseMapper implements DefaultResponseMapper.CustomResponseMapper<ProfileAndCredential> {
    private final ZonaRosaServiceProfile.RequestType   requestType;
    private final ProfileKeyCredentialRequestContext requestContext;

    public ProfileResponseMapper(ZonaRosaServiceProfile.RequestType requestType, ProfileKeyCredentialRequestContext requestContext) {
      this.requestType    = requestType;
      this.requestContext = requestContext;
    }

    @Override
    public ServiceResponse<ProfileAndCredential> map(int status, String body, Function<String, String> getHeader, boolean unidentified)
        throws MalformedResponseException
    {
      try {
        ZonaRosaServiceProfile         zonarosaServiceProfile         = JsonUtil.fromJsonResponse(body, ZonaRosaServiceProfile.class);
        ExpiringProfileKeyCredential expiringProfileKeyCredential = null;
        if (requestContext != null && zonarosaServiceProfile.getExpiringProfileKeyCredentialResponse() != null) {
          expiringProfileKeyCredential = clientZkProfileOperations.receiveExpiringProfileKeyCredential(requestContext, zonarosaServiceProfile.getExpiringProfileKeyCredentialResponse());
        }

        return ServiceResponse.forResult(new ProfileAndCredential(zonarosaServiceProfile, requestType, Optional.ofNullable(expiringProfileKeyCredential)), status, body);
      } catch (VerificationFailedException e) {
        return ServiceResponse.forApplicationError(e, status, body);
      }
    }
  }

  /**
   * Response processor for {@link ProfileAndCredential} service response.
   */
  public static final class ProfileResponseProcessor extends ServiceResponseProcessor<ProfileAndCredential> {
    public ProfileResponseProcessor(ServiceResponse<ProfileAndCredential> response) {
      super(response);
    }

    public <T> Pair<T, ProfileAndCredential> getResult(T with) {
      return new Pair<>(with, getResult());
    }

    @Override
    public boolean notFound() {
      return super.notFound();
    }

    @Override
    public boolean genericIoError() {
      return super.genericIoError();
    }

    @Override
    public Throwable getError() {
      return super.getError();
    }
  }
}

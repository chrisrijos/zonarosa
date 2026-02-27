package io.zonarosa.server.grpc;

import io.grpc.stub.StreamObserver;
import io.zonarosa.chat.rpc.GetAuthenticatedDeviceRequest;
import io.zonarosa.chat.rpc.GetAuthenticatedDeviceResponse;
import io.zonarosa.chat.rpc.GetRequestAttributesRequest;
import io.zonarosa.chat.rpc.GetRequestAttributesResponse;
import io.zonarosa.chat.rpc.RequestAttributesGrpc;
import io.zonarosa.server.auth.grpc.AuthenticatedDevice;
import io.zonarosa.server.auth.grpc.AuthenticationUtil;
import io.zonarosa.server.util.UUIDUtil;

public class RequestAttributesServiceImpl extends RequestAttributesGrpc.RequestAttributesImplBase {

  @Override
  public void getRequestAttributes(final GetRequestAttributesRequest request,
      final StreamObserver<GetRequestAttributesResponse> responseObserver) {

    final GetRequestAttributesResponse.Builder responseBuilder = GetRequestAttributesResponse.newBuilder();

    RequestAttributesUtil.getAcceptableLanguages()
        .forEach(languageRange -> responseBuilder.addAcceptableLanguages(languageRange.toString()));

    RequestAttributesUtil.getAvailableAcceptedLocales().forEach(locale ->
        responseBuilder.addAvailableAcceptedLocales(locale.toLanguageTag()));

    responseBuilder.setRemoteAddress(RequestAttributesUtil.getRemoteAddress().getHostAddress());

    RequestAttributesUtil.getUserAgent().ifPresent(responseBuilder::setUserAgent);

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void getAuthenticatedDevice(final GetAuthenticatedDeviceRequest request,
      final StreamObserver<GetAuthenticatedDeviceResponse> responseObserver) {

    final GetAuthenticatedDeviceResponse.Builder responseBuilder = GetAuthenticatedDeviceResponse.newBuilder();

    try {
      final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();

      responseBuilder.setAccountIdentifier(UUIDUtil.toByteString(authenticatedDevice.accountIdentifier()));
      responseBuilder.setDeviceId(authenticatedDevice.deviceId());
    } catch (final Exception ignored) {
    }

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }
}

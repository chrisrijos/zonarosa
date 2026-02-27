/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api;

import io.zonarosa.libzonarosa.net.Network;
import io.zonarosa.service.api.account.AccountApi;
import io.zonarosa.service.api.groupsv2.ClientZkOperations;
import io.zonarosa.service.api.groupsv2.GroupsV2Api;
import io.zonarosa.service.api.groupsv2.GroupsV2Operations;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.core.models.ServiceId.PNI;
import io.zonarosa.service.api.registration.RegistrationApi;
import io.zonarosa.service.api.svr.SecureValueRecoveryV2;
import io.zonarosa.service.api.svr.SecureValueRecoveryV3;
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket;
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration;
import io.zonarosa.service.internal.push.PushServiceSocket;
import io.zonarosa.service.internal.push.WhoAmIResponse;
import io.zonarosa.service.internal.util.StaticCredentialsProvider;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The main interface for creating, registering, and
 * managing a ZonaRosa Service account.
 *
 * @author Moxie Marlinspike
 */
public class ZonaRosaServiceAccountManager {

  private static final String TAG = ZonaRosaServiceAccountManager.class.getSimpleName();

  private final PushServiceSocket                      pushServiceSocket;
  private final GroupsV2Operations                     groupsV2Operations;
  private final ZonaRosaServiceConfiguration             configuration;
  private final ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket;
  private final AccountApi                             accountApi;

  /**
   * Construct a ZonaRosaServiceAccountManager.
   * @param configuration The URL for the ZonaRosa Service.
   * @param aci The ZonaRosa Service ACI.
   * @param pni The ZonaRosa Service PNI.
   * @param e164 The ZonaRosa Service phone number.
   * @param password A ZonaRosa Service password.
   * @param zonarosaAgent A string which identifies the client software.
   */
  public static ZonaRosaServiceAccountManager createWithStaticCredentials(ZonaRosaServiceConfiguration configuration,
                                                                        ACI aci,
                                                                        PNI pni,
                                                                        String e164,
                                                                        int deviceId,
                                                                        String password,
                                                                        String zonarosaAgent,
                                                                        boolean automaticNetworkRetry,
                                                                        int maxGroupSize)
  {
    StaticCredentialsProvider credentialProvider = new StaticCredentialsProvider(aci, pni, e164, deviceId, password);
    GroupsV2Operations        gv2Operations      = new GroupsV2Operations(ClientZkOperations.create(configuration), maxGroupSize);

    return new ZonaRosaServiceAccountManager(
        null,
        null,
        new PushServiceSocket(configuration, credentialProvider, zonarosaAgent, automaticNetworkRetry),
        gv2Operations
    );
  }

  public ZonaRosaServiceAccountManager(@Nullable ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket,
                                     @Nullable AccountApi accountApi,
                                     @Nonnull PushServiceSocket pushServiceSocket,
                                     @Nonnull GroupsV2Operations groupsV2Operations) {
    this.authWebSocket      = authWebSocket;
    this.accountApi         = accountApi;
    this.groupsV2Operations = groupsV2Operations;
    this.pushServiceSocket  = pushServiceSocket;
    this.configuration      = pushServiceSocket.getConfiguration();
  }

  public SecureValueRecoveryV2 getSecureValueRecoveryV2(String mrEnclave) {
    return new SecureValueRecoveryV2(configuration, mrEnclave, authWebSocket);
  }

  public SecureValueRecoveryV3 getSecureValueRecoveryV3(Network network) {
    return new SecureValueRecoveryV3(network, authWebSocket);
  }

  public WhoAmIResponse getWhoAmI() throws IOException {
    return NetworkResultUtil.toBasicLegacy(accountApi.whoAmI());
  }

  /**
   * Request a push challenge. A number will be pushed to the GCM (FCM) id. This can then be used
   * during SMS/call requests to bypass the CAPTCHA.
   *
   * @param gcmRegistrationId The GCM (FCM) id to use.
   * @param sessionId         The session to request a push for.
   * @throws IOException
   */
  public void requestRegistrationPushChallenge(String sessionId, String gcmRegistrationId) throws IOException {
    pushServiceSocket.requestPushChallenge(sessionId, gcmRegistrationId);
  }

  public void checkNetworkConnection() throws IOException {
    this.pushServiceSocket.pingStorageService();
  }

  public void cancelInFlightRequests() {
    this.pushServiceSocket.cancelInFlightRequests();
  }

  public GroupsV2Api getGroupsV2Api() {
    return new GroupsV2Api(authWebSocket, pushServiceSocket, groupsV2Operations);
  }

  public RegistrationApi getRegistrationApi() {
    return new RegistrationApi(pushServiceSocket);
  }
}

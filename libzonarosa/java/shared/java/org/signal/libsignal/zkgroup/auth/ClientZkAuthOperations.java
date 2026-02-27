//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.auth;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.ServiceId.Aci;
import io.zonarosa.libzonarosa.protocol.ServiceId.Pni;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams;

public class ClientZkAuthOperations {

  private final ServerPublicParams serverPublicParams;

  public ClientZkAuthOperations(ServerPublicParams serverPublicParams) {
    this.serverPublicParams = serverPublicParams;
  }

  /**
   * Produces the AuthCredentialWithPni from a server-generated AuthCredentialWithPniResponse.
   *
   * @param redemptionTime This is provided by the server as an integer, and should be passed
   *     through directly.
   */
  public AuthCredentialWithPni receiveAuthCredentialWithPniAsServiceId(
      Aci aci, Pni pni, long redemptionTime, AuthCredentialWithPniResponse authCredentialResponse)
      throws VerificationFailedException {
    byte[] newContents =
        filterExceptions(
            VerificationFailedException.class,
            () ->
                serverPublicParams.guardedMapChecked(
                    (publicParams) ->
                        Native.ServerPublicParams_ReceiveAuthCredentialWithPniAsServiceId(
                            publicParams,
                            aci.toServiceIdFixedWidthBinary(),
                            pni.toServiceIdFixedWidthBinary(),
                            redemptionTime,
                            authCredentialResponse.getInternalContentsForJNI())));

    try {
      return new AuthCredentialWithPni(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public AuthCredentialPresentation createAuthCredentialPresentation(
      GroupSecretParams groupSecretParams, AuthCredentialWithPni authCredential) {
    return createAuthCredentialPresentation(new SecureRandom(), groupSecretParams, authCredential);
  }

  public AuthCredentialPresentation createAuthCredentialPresentation(
      SecureRandom secureRandom,
      GroupSecretParams groupSecretParams,
      AuthCredentialWithPni authCredential) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        serverPublicParams.guardedMap(
            (publicParams) ->
                Native.ServerPublicParams_CreateAuthCredentialWithPniPresentationDeterministic(
                    publicParams,
                    random,
                    groupSecretParams.getInternalContentsForJNI(),
                    authCredential.getInternalContentsForJNI()));

    try {
      return new AuthCredentialPresentation(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}

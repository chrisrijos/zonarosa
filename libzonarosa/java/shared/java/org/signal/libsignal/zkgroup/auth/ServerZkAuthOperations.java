//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.auth;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.ServiceId.Aci;
import io.zonarosa.libzonarosa.protocol.ServiceId.Pni;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupPublicParams;

public class ServerZkAuthOperations {

  private final ServerSecretParams serverSecretParams;

  public ServerZkAuthOperations(ServerSecretParams serverSecretParams) {
    this.serverSecretParams = serverSecretParams;
  }

  public AuthCredentialWithPniResponse issueAuthCredentialWithPniZkc(
      Aci aci, Pni pni, Instant redemptionTime) {
    return issueAuthCredentialWithPniZkc(new SecureRandom(), aci, pni, redemptionTime);
  }

  public AuthCredentialWithPniResponse issueAuthCredentialWithPniZkc(
      SecureRandom secureRandom, Aci aci, Pni pni, Instant redemptionTime) {
    byte[] random = new byte[RANDOM_LENGTH];

    secureRandom.nextBytes(random);

    byte[] newContents =
        serverSecretParams.guardedMap(
            (serverSecretParams) ->
                Native.ServerSecretParams_IssueAuthCredentialWithPniZkcDeterministic(
                    serverSecretParams,
                    random,
                    aci.toServiceIdFixedWidthBinary(),
                    pni.toServiceIdFixedWidthBinary(),
                    redemptionTime.getEpochSecond()));

    try {
      return new AuthCredentialWithPniResponse(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public void verifyAuthCredentialPresentation(
      GroupPublicParams groupPublicParams, AuthCredentialPresentation authCredentialPresentation)
      throws VerificationFailedException {
    verifyAuthCredentialPresentation(groupPublicParams, authCredentialPresentation, Instant.now());
  }

  public void verifyAuthCredentialPresentation(
      GroupPublicParams groupPublicParams,
      AuthCredentialPresentation authCredentialPresentation,
      Instant currentTime)
      throws VerificationFailedException {
    filterExceptions(
        VerificationFailedException.class,
        () ->
            serverSecretParams.guardedRunChecked(
                (secretParams) ->
                    Native.ServerSecretParams_VerifyAuthCredentialPresentation(
                        secretParams,
                        groupPublicParams.getInternalContentsForJNI(),
                        authCredentialPresentation.getInternalContentsForJNI(),
                        currentTime.getEpochSecond())));
  }
}

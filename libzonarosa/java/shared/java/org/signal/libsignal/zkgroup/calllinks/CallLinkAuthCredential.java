//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.calllinks;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.zkgroup.internal.Constants.RANDOM_LENGTH;

import java.security.SecureRandom;
import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.ServiceId.Aci;
import io.zonarosa.libzonarosa.zkgroup.GenericServerPublicParams;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class CallLinkAuthCredential extends ByteArray {

  public CallLinkAuthCredential(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.CallLinkAuthCredential_CheckValidContents(contents));
  }

  public CallLinkAuthCredentialPresentation present(
      Aci userId,
      Instant redemptionTime,
      GenericServerPublicParams serverParams,
      CallLinkSecretParams callLinkParams) {
    return present(userId, redemptionTime, serverParams, callLinkParams, new SecureRandom());
  }

  public CallLinkAuthCredentialPresentation present(
      Aci userId,
      Instant redemptionTime,
      GenericServerPublicParams serverParams,
      CallLinkSecretParams callLinkParams,
      SecureRandom secureRandom) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        filterExceptions(
            () ->
                Native.CallLinkAuthCredential_PresentDeterministic(
                    getInternalContentsForJNI(),
                    userId.toServiceIdFixedWidthBinary(),
                    redemptionTime.getEpochSecond(),
                    serverParams.getInternalContentsForJNI(),
                    callLinkParams.getInternalContentsForJNI(),
                    random));

    try {
      return new CallLinkAuthCredentialPresentation(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}

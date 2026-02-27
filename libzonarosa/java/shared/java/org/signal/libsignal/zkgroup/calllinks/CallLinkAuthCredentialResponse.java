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
import io.zonarosa.libzonarosa.zkgroup.GenericServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class CallLinkAuthCredentialResponse extends ByteArray {
  public CallLinkAuthCredentialResponse(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.CallLinkAuthCredentialResponse_CheckValidContents(contents));
  }

  public static CallLinkAuthCredentialResponse issueCredential(
      Aci userId, Instant redemptionTime, GenericServerSecretParams params) {
    return issueCredential(userId, redemptionTime, params, new SecureRandom());
  }

  public static CallLinkAuthCredentialResponse issueCredential(
      Aci userId,
      Instant redemptionTime,
      GenericServerSecretParams params,
      SecureRandom secureRandom) {
    byte[] random = new byte[RANDOM_LENGTH];
    secureRandom.nextBytes(random);

    byte[] newContents =
        Native.CallLinkAuthCredentialResponse_IssueDeterministic(
            userId.toServiceIdFixedWidthBinary(),
            redemptionTime.getEpochSecond(),
            params.getInternalContentsForJNI(),
            random);

    try {
      return new CallLinkAuthCredentialResponse(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public CallLinkAuthCredential receive(
      Aci userId, Instant redemptionTime, GenericServerPublicParams params)
      throws VerificationFailedException {
    byte[] newContents =
        filterExceptions(
            VerificationFailedException.class,
            () ->
                Native.CallLinkAuthCredentialResponse_Receive(
                    getInternalContentsForJNI(),
                    userId.toServiceIdFixedWidthBinary(),
                    redemptionTime.getEpochSecond(),
                    params.getInternalContentsForJNI()));

    try {
      return new CallLinkAuthCredential(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}

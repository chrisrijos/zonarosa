//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.calllinks;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.protocol.ServiceId.Aci;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.groups.UuidCiphertext;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class CallLinkSecretParams extends ByteArray {

  public static CallLinkSecretParams deriveFromRootKey(byte[] rootKey) {
    byte[] newContents = Native.CallLinkSecretParams_DeriveFromRootKey(rootKey);

    try {
      return new CallLinkSecretParams(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public CallLinkSecretParams(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.CallLinkSecretParams_CheckValidContents(contents));
  }

  public CallLinkPublicParams getPublicParams() {
    byte[] newContents = Native.CallLinkSecretParams_GetPublicParams(contents);

    try {
      return new CallLinkPublicParams(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public Aci decryptUserId(UuidCiphertext ciphertext) throws VerificationFailedException {
    try {
      return Aci.parseFromFixedWidthBinary(
          filterExceptions(
              VerificationFailedException.class,
              () ->
                  Native.CallLinkSecretParams_DecryptUserId(
                      getInternalContentsForJNI(), ciphertext.getInternalContentsForJNI())));
    } catch (ServiceId.InvalidServiceIdException e) {
      throw new VerificationFailedException();
    }
  }
}

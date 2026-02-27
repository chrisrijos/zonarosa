//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.profiles;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.protocol.ServiceId.Aci;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class ProfileKey extends ByteArray {

  public ProfileKey(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () ->
            filterExceptions(
                InvalidInputException.class, () -> Native.ProfileKey_CheckValidContents(contents)));
  }

  public ProfileKeyCommitment getCommitment(Aci userId) {
    byte[] newContents =
        Native.ProfileKey_GetCommitment(contents, userId.toServiceIdFixedWidthBinary());

    try {
      return new ProfileKeyCommitment(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public ProfileKeyVersion getProfileKeyVersion(Aci userId) {
    byte[] newContents =
        Native.ProfileKey_GetProfileKeyVersion(contents, userId.toServiceIdFixedWidthBinary());

    try {
      return new ProfileKeyVersion(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public byte[] deriveAccessKey() {
    return Native.ProfileKey_DeriveAccessKey(contents);
  }
}

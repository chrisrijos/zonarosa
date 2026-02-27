//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.profiles;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.groups.ProfileKeyCiphertext;
import io.zonarosa.libzonarosa.zkgroup.groups.UuidCiphertext;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class ProfileKeyCredentialPresentation extends ByteArray {

  public enum Version {
    V1,
    V2,
    V3,
    UNKNOWN
  };

  public ProfileKeyCredentialPresentation(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.ProfileKeyCredentialPresentation_CheckValidContents(contents));
  }

  public UuidCiphertext getUuidCiphertext() {
    byte[] newContents = Native.ProfileKeyCredentialPresentation_GetUuidCiphertext(contents);

    try {
      return new UuidCiphertext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public ProfileKeyCiphertext getProfileKeyCiphertext() {
    byte[] newContents = Native.ProfileKeyCredentialPresentation_GetProfileKeyCiphertext(contents);

    try {
      return new ProfileKeyCiphertext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public byte[] getStructurallyValidV1PresentationBytes() {
    return Native.ProfileKeyCredentialPresentation_GetStructurallyValidV1PresentationBytes(
        contents);
  }

  public Version getVersion() {
    switch (this.contents[0]) {
      case 0:
        return Version.V1;
      case 1:
        return Version.V2;
      case 2:
        return Version.V3;
      default:
        return Version.UNKNOWN;
    }
  }
}

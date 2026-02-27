//
// Copyright 2020-2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.auth;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.groups.UuidCiphertext;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class AuthCredentialPresentation extends ByteArray {

  public enum Version {
    V1,
    V2,
    V3,
    V4,
    UNKNOWN
  };

  public AuthCredentialPresentation(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.AuthCredentialPresentation_CheckValidContents(contents));
  }

  public UuidCiphertext getUuidCiphertext() {
    byte[] newContents = Native.AuthCredentialPresentation_GetUuidCiphertext(contents);

    try {
      return new UuidCiphertext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  /** Returns the PNI ciphertext for this credential. Will never be {@code null}. */
  public UuidCiphertext getPniCiphertext() {
    byte[] newContents = Native.AuthCredentialPresentation_GetPniCiphertext(contents);

    try {
      return new UuidCiphertext(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }

  public Instant getRedemptionTime() {
    return Instant.ofEpochSecond(Native.AuthCredentialPresentation_GetRedemptionTime(contents));
  }

  public Version getVersion() {
    byte version = this.contents[0];
    final Version[] values = Version.values();
    if (version < values.length) {
      return values[version];
    }
    return Version.UNKNOWN;
  }
}

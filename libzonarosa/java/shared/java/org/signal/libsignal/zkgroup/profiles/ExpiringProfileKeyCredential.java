//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.profiles;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class ExpiringProfileKeyCredential extends ByteArray {
  public ExpiringProfileKeyCredential(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.ExpiringProfileKeyCredential_CheckValidContents(contents));
  }

  public Instant getExpirationTime() {
    return Instant.ofEpochSecond(
        Native.ExpiringProfileKeyCredential_GetExpirationTime(this.contents));
  }
}

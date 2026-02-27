//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.groups;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class UuidCiphertext extends ByteArray {
  public UuidCiphertext(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class, () -> Native.UuidCiphertext_CheckValidContents(contents));
  }

  public static byte[] serializeAndConcatenate(Collection<UuidCiphertext> ciphertexts) {
    ByteArrayOutputStream concatenated = new ByteArrayOutputStream();
    for (UuidCiphertext member : ciphertexts) {
      try {
        concatenated.write(member.getInternalContentsForJNI());
      } catch (IOException e) {
        // ByteArrayOutputStream should never fail.
        throw new AssertionError(e);
      }
    }
    return concatenated.toByteArray();
  }
}

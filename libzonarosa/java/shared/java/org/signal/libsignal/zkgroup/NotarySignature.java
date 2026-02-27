//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup;

import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class NotarySignature extends ByteArray {

  public static final int SIZE = 64;

  public NotarySignature(byte[] contents) throws InvalidInputException {
    super(contents, SIZE);
  }
}

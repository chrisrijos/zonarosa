//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.receipts;

import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class ReceiptSerial extends ByteArray {

  public static final int SIZE = 16;

  public ReceiptSerial(byte[] contents) throws InvalidInputException {
    super(contents, SIZE);
  }
}

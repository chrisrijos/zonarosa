//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.receipts;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class ReceiptCredentialPresentation extends ByteArray {
  public ReceiptCredentialPresentation(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () ->
            filterExceptions(
                InvalidInputException.class,
                () -> Native.ReceiptCredentialPresentation_CheckValidContents(contents)));
  }

  public long getReceiptExpirationTime() {
    return Native.ReceiptCredentialPresentation_GetReceiptExpirationTime(contents);
  }

  public long getReceiptLevel() {
    return Native.ReceiptCredentialPresentation_GetReceiptLevel(contents);
  }

  public ReceiptSerial getReceiptSerial() {
    byte[] newContents = Native.ReceiptCredentialPresentation_GetReceiptSerial(contents);

    try {
      return new ReceiptSerial(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}

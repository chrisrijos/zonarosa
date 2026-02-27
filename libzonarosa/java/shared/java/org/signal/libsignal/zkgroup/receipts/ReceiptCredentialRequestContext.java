//
// Copyright 2020-2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.receipts;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

public final class ReceiptCredentialRequestContext extends ByteArray {

  public static final int SIZE = 177;

  public ReceiptCredentialRequestContext(byte[] contents) throws InvalidInputException {
    super(contents, SIZE);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.ReceiptCredentialRequestContext_CheckValidContents(contents));
  }

  public ReceiptCredentialRequest getRequest() {
    byte[] newContents = Native.ReceiptCredentialRequestContext_GetRequest(contents);

    try {
      return new ReceiptCredentialRequest(newContents);
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }
  }
}

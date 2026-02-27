/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.donations;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialResponse;
import io.zonarosa.core.util.Base64;

import java.io.IOException;

import javax.annotation.Nullable;

class ReceiptCredentialResponseJson {

  private final ReceiptCredentialResponse receiptCredentialResponse;

  ReceiptCredentialResponseJson(@JsonProperty("receiptCredentialResponse") String receiptCredentialResponse) {
    ReceiptCredentialResponse response;
    try {
      response = new ReceiptCredentialResponse(Base64.decode(receiptCredentialResponse));
    } catch (IOException | InvalidInputException e) {
      response = null;
    }

    this.receiptCredentialResponse = response;
  }

  public @Nullable ReceiptCredentialResponse getReceiptCredentialResponse() {
    return receiptCredentialResponse;
  }
}

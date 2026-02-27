/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.donations;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialRequest;
import io.zonarosa.core.util.Base64;
import io.zonarosa.service.internal.push.DonationProcessor;

class BoostReceiptCredentialRequestJson {
  @JsonProperty("paymentIntentId")
  private final String paymentIntentId;

  @JsonProperty("receiptCredentialRequest")
  private final String receiptCredentialRequest;

  @JsonProperty("processor")
  private final String processor;

  BoostReceiptCredentialRequestJson(String paymentIntentId, ReceiptCredentialRequest receiptCredentialRequest, DonationProcessor processor) {
    this.paymentIntentId          = paymentIntentId;
    this.receiptCredentialRequest = Base64.encodeWithPadding(receiptCredentialRequest.serialize());
    this.processor                = processor.getCode();
  }
}

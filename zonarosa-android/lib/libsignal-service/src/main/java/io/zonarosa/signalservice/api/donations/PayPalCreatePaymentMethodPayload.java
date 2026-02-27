/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.donations;

import com.fasterxml.jackson.annotation.JsonProperty;

class PayPalCreatePaymentMethodPayload {
  @JsonProperty
  private String returnUrl;

  @JsonProperty
  private String cancelUrl;

  PayPalCreatePaymentMethodPayload(String returnUrl, String cancelUrl) {
    this.returnUrl = returnUrl;
    this.cancelUrl = cancelUrl;
  }
}

/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.subscriptions;

import java.nio.charset.StandardCharsets;

public record ProcessorCustomer(String customerId, PaymentProvider processor) {

  public byte[] toDynamoBytes() {
    final byte[] customerIdBytes = customerId.getBytes(StandardCharsets.UTF_8);
    final byte[] combinedBytes = new byte[customerIdBytes.length + 1];

    combinedBytes[0] = processor.getId();
    System.arraycopy(customerIdBytes, 0, combinedBytes, 1, customerIdBytes.length);

    return combinedBytes;
  }
}

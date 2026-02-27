/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey;
import io.zonarosa.server.configuration.secrets.SecretBytes;
import io.zonarosa.server.util.ExactlySize;

public record UnidentifiedDeliveryConfiguration(@NotNull @NotEmpty  byte[] certificate,
                                                @ExactlySize(32) SecretBytes privateKey,
                                                int expiresDays,
                                                boolean embedSigner) {
  public ECPrivateKey ecPrivateKey() throws InvalidKeyException {
    return new ECPrivateKey(privateKey.value());
  }
}

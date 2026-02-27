/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.secrets;

import org.apache.commons.lang3.Validate;

public class SecretBytes extends Secret<byte[]> {

  public SecretBytes(final byte[] value) {
    super(requireNotEmpty(value));
  }

  private static byte[] requireNotEmpty(final byte[] value) {
    Validate.isTrue(value.length > 0, "SecretBytes value must not be empty");
    return value;
  }
}

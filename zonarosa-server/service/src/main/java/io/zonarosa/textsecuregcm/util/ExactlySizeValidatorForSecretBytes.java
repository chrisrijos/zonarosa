/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import io.zonarosa.server.configuration.secrets.SecretBytes;

public class ExactlySizeValidatorForSecretBytes extends ExactlySizeValidator<SecretBytes> {
  @Override
  protected int size(final SecretBytes value) {
    return value == null ? 0 : value.value().length;
  }
}

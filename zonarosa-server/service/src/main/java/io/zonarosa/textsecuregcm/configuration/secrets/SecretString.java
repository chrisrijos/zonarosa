/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.secrets;

import org.apache.commons.lang3.Validate;

public class SecretString extends Secret<String> {
  public SecretString(final String value) {
    super(Validate.notBlank(value, "SecretString value must not be blank"));
  }
}

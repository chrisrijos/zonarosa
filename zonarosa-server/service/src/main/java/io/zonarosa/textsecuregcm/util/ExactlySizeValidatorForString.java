/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;


public class ExactlySizeValidatorForString extends ExactlySizeValidator<String> {

  @Override
  protected int size(final String value) {
    return value == null ? 0 : value.length();
  }
}

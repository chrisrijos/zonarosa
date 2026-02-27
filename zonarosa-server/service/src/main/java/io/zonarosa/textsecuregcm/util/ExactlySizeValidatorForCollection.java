/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import java.util.Collection;

public class ExactlySizeValidatorForCollection extends ExactlySizeValidator<Collection<?>> {

  @Override
  protected int size(final Collection<?> value) {
    return value == null ? 0 : value.size();
  }
}

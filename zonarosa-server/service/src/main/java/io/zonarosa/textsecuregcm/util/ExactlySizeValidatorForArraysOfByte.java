/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

public class ExactlySizeValidatorForArraysOfByte extends ExactlySizeValidator<byte[]> {

  @Override
  protected int size(final byte[] value) {
    return value == null ? 0 : value.length;
  }
}

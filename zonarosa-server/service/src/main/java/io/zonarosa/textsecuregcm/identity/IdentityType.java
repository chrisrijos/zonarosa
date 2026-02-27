/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.identity;

public enum IdentityType {
  ACI((byte) 0x00, "ACI:"),
  PNI((byte) 0x01, "PNI:");

  private final byte bytePrefix;
  private final String stringPrefix;

  IdentityType(final byte bytePrefix, final String stringPrefix) {
    this.bytePrefix = bytePrefix;
    this.stringPrefix = stringPrefix;
  }

  byte getBytePrefix() {
    return bytePrefix;
  }

  String getStringPrefix() {
    return stringPrefix;
  }
}

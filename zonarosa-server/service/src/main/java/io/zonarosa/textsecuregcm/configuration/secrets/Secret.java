/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration.secrets;

public class Secret<T> {

  private final T value;


  public Secret(final T value) {
    this.value = value;
  }

  public T value() {
    return value;
  }

  @Override
  public String toString() {
    return "[REDACTED]";
  }
}

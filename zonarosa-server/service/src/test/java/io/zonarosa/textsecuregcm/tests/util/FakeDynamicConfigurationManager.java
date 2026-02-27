/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.tests.util;

import io.zonarosa.server.storage.DynamicConfigurationManager;

public class FakeDynamicConfigurationManager<T> extends DynamicConfigurationManager<T> {

  T staticConfiguration;

  public FakeDynamicConfigurationManager(T staticConfiguration) {
    super(null, (Class<T>) staticConfiguration.getClass());
    this.staticConfiguration = staticConfiguration;
  }

  @Override
  public T getConfiguration() {
    return staticConfiguration;
  }

}

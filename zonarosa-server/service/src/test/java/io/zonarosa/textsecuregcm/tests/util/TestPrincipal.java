/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.tests.util;

import java.security.Principal;
import java.util.Optional;

public class TestPrincipal implements Principal {

  private final String name;

  private TestPrincipal(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  public static Optional<TestPrincipal> authenticatedTestPrincipal(final String name) {
    return Optional.of(new TestPrincipal(name));
  }
}

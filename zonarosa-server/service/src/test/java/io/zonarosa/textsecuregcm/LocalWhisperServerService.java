/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server;

import io.dropwizard.util.Resources;
import java.util.Optional;

/**
 * This class may be run directly from a correctly configured IDE, or using the command line:
 * <p>
 * <code>./mvnw clean integration-test -DskipTests=true -Ptest-server</code>
 * <p>
 * <strong>NOTE: many features are non-functional, especially those that depend on external services</strong>
 * <p>
 * By default, it will use {@code config/test.yml}, but this may be overridden by setting an environment variable,
 * {@value ZONAROSA_SERVER_CONFIG_ENV_VAR}, with a custom path.
 */
public class LocalWhisperServerService {

  private static final String ZONAROSA_SERVER_CONFIG_ENV_VAR = "ZONAROSA_SERVER_CONFIG";

  public static void main(String[] args) throws Exception {

    System.setProperty("secrets.bundle.filename",
        Resources.getResource("config/test-secrets-bundle.yml").getPath());

    final String config = Optional.ofNullable(System.getenv(ZONAROSA_SERVER_CONFIG_ENV_VAR))
        .orElse(Resources.getResource("config/test.yml").getPath());

    new WhisperServerService().run("server", config);
  }
}

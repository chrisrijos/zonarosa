/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.backup;

import com.google.common.annotations.VisibleForTesting;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.configuration.SecureValueRecoveryConfiguration;
import java.time.Clock;

public class SecureValueRecoveryBCredentialsGeneratorFactory {
  private SecureValueRecoveryBCredentialsGeneratorFactory() {}


  @VisibleForTesting
  static ExternalServiceCredentialsGenerator svrbCredentialsGenerator(
      final SecureValueRecoveryConfiguration cfg,
      final Clock clock) {
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .withUserDerivationKey(cfg.userIdTokenSharedSecret().value())
        .prependUsername(false)
        .withDerivedUsernameTruncateLength(16)
        .withClock(clock)
        .build();
  }

  public static ExternalServiceCredentialsGenerator svrbCredentialsGenerator(final SecureValueRecoveryConfiguration cfg) {
    return svrbCredentialsGenerator(cfg, Clock.systemUTC());
  }
}

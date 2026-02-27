/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static java.util.Objects.requireNonNull;

import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import io.zonarosa.chat.credentials.ExternalServiceType;
import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.configuration.DirectoryV2ClientConfiguration;
import io.zonarosa.server.configuration.PaymentsServiceConfiguration;
import io.zonarosa.server.configuration.SecureValueRecoveryConfiguration;

enum ExternalServiceDefinitions {
  DIRECTORY(ExternalServiceType.EXTERNAL_SERVICE_TYPE_DIRECTORY, (chatConfig, clock) -> {
    final DirectoryV2ClientConfiguration cfg = chatConfig.getDirectoryV2Configuration().getDirectoryV2ClientConfiguration();
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .withUserDerivationKey(cfg.userIdTokenSharedSecret())
        .prependUsername(false)
        .withClock(clock)
        .build();
  }),
  PAYMENTS(ExternalServiceType.EXTERNAL_SERVICE_TYPE_PAYMENTS, (chatConfig, clock) -> {
    final PaymentsServiceConfiguration cfg = chatConfig.getPaymentsServiceConfiguration();
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .prependUsername(true)
        .build();
  }),
  SVR(ExternalServiceType.EXTERNAL_SERVICE_TYPE_SVR, (chatConfig, clock) -> {
    final SecureValueRecoveryConfiguration cfg = chatConfig.getSvr2Configuration();
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .withUserDerivationKey(cfg.userIdTokenSharedSecret().value())
        .prependUsername(false)
        .withDerivedUsernameTruncateLength(16)
        .withClock(clock)
        .build();
  }),
  STORAGE(ExternalServiceType.EXTERNAL_SERVICE_TYPE_STORAGE, (chatConfig, clock) -> {
    final PaymentsServiceConfiguration cfg = chatConfig.getPaymentsServiceConfiguration();
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .prependUsername(true)
        .build();
  }),
  ;

  private final ExternalServiceType externalService;

  private final BiFunction<WhisperServerConfiguration, Clock, ExternalServiceCredentialsGenerator> generatorFactory;

  ExternalServiceDefinitions(
      final ExternalServiceType externalService,
      final BiFunction<WhisperServerConfiguration, Clock, ExternalServiceCredentialsGenerator> factory) {
    this.externalService = requireNonNull(externalService);
    this.generatorFactory = requireNonNull(factory);
  }

  public static Map<ExternalServiceType, ExternalServiceCredentialsGenerator> createExternalServiceList(
      final WhisperServerConfiguration chatConfiguration,
      final Clock clock) {
    return Arrays.stream(values())
        .map(esd -> Pair.of(esd.externalService, esd.generatorFactory().apply(chatConfiguration, clock)))
        .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
  }

  public BiFunction<WhisperServerConfiguration, Clock, ExternalServiceCredentialsGenerator> generatorFactory() {
    return generatorFactory;
  }

  ExternalServiceType externalService() {
    return externalService;
  }
}

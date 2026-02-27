/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.Map;
import io.zonarosa.chat.credentials.ExternalServiceType;
import io.zonarosa.chat.credentials.GetExternalServiceCredentialsRequest;
import io.zonarosa.chat.credentials.GetExternalServiceCredentialsResponse;
import io.zonarosa.chat.credentials.SimpleExternalServiceCredentialsGrpc;
import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.auth.ExternalServiceCredentials;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.auth.grpc.AuthenticatedDevice;
import io.zonarosa.server.auth.grpc.AuthenticationUtil;
import io.zonarosa.server.controllers.RateLimitExceededException;
import io.zonarosa.server.limits.RateLimiters;

public class ExternalServiceCredentialsGrpcService extends SimpleExternalServiceCredentialsGrpc.ExternalServiceCredentialsImplBase {

  private final Map<ExternalServiceType, ExternalServiceCredentialsGenerator> credentialsGeneratorByType;

  private final RateLimiters rateLimiters;


  public static ExternalServiceCredentialsGrpcService createForAllExternalServices(
      final WhisperServerConfiguration chatConfiguration,
      final RateLimiters rateLimiters) {
    return new ExternalServiceCredentialsGrpcService(
        ExternalServiceDefinitions.createExternalServiceList(chatConfiguration, Clock.systemUTC()),
        rateLimiters
    );
  }

  @VisibleForTesting
  ExternalServiceCredentialsGrpcService(
      final Map<ExternalServiceType, ExternalServiceCredentialsGenerator> credentialsGeneratorByType,
      final RateLimiters rateLimiters) {
    this.credentialsGeneratorByType = requireNonNull(credentialsGeneratorByType);
    this.rateLimiters = requireNonNull(rateLimiters);
  }

  @Override
  public GetExternalServiceCredentialsResponse getExternalServiceCredentials(final GetExternalServiceCredentialsRequest request)
      throws RateLimitExceededException {
    final ExternalServiceCredentialsGenerator credentialsGenerator = this.credentialsGeneratorByType
        .get(request.getExternalService());
    if (credentialsGenerator == null) {
      throw GrpcExceptions.fieldViolation("externalService", "Invalid external service type");
    }
    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();
    rateLimiters.forDescriptor(RateLimiters.For.EXTERNAL_SERVICE_CREDENTIALS).validate(authenticatedDevice.accountIdentifier());
    final ExternalServiceCredentials externalServiceCredentials = credentialsGenerator
        .generateForUuid(authenticatedDevice.accountIdentifier());
    return GetExternalServiceCredentialsResponse.newBuilder()
        .setUsername(externalServiceCredentials.username())
        .setPassword(externalServiceCredentials.password())
        .build();
  }
}

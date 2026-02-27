/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static io.zonarosa.server.grpc.GrpcTestUtils.assertRateLimitExceeded;
import static io.zonarosa.server.grpc.GrpcTestUtils.assertStatusException;
import static io.zonarosa.server.grpc.GrpcTestUtils.assertStatusUnauthenticated;

import io.grpc.Status;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import io.zonarosa.chat.credentials.ExternalServiceCredentialsGrpc;
import io.zonarosa.chat.credentials.ExternalServiceType;
import io.zonarosa.chat.credentials.GetExternalServiceCredentialsRequest;
import io.zonarosa.chat.credentials.GetExternalServiceCredentialsResponse;
import io.zonarosa.server.auth.ExternalServiceCredentials;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.limits.RateLimiter;
import io.zonarosa.server.limits.RateLimiters;
import io.zonarosa.server.util.MockUtils;
import io.zonarosa.server.util.TestRandomUtil;
import reactor.core.publisher.Mono;

public class ExternalServiceCredentialsGrpcServiceTest
    extends SimpleBaseGrpcTest<ExternalServiceCredentialsGrpcService, ExternalServiceCredentialsGrpc.ExternalServiceCredentialsBlockingStub> {

  private static final ExternalServiceCredentialsGenerator DIRECTORY_CREDENTIALS_GENERATOR = Mockito.spy(ExternalServiceCredentialsGenerator
      .builder(TestRandomUtil.nextBytes(32))
      .withUserDerivationKey(TestRandomUtil.nextBytes(32))
      .prependUsername(false)
      .truncateSignature(false)
      .build());

  private static final ExternalServiceCredentialsGenerator PAYMENTS_CREDENTIALS_GENERATOR = Mockito.spy(ExternalServiceCredentialsGenerator
      .builder(TestRandomUtil.nextBytes(32))
      .prependUsername(true)
      .build());

  @Mock
  private RateLimiters rateLimiters;


  @Override
  protected ExternalServiceCredentialsGrpcService createServiceBeforeEachTest() {
    return new ExternalServiceCredentialsGrpcService(Map.of(
        ExternalServiceType.EXTERNAL_SERVICE_TYPE_DIRECTORY, DIRECTORY_CREDENTIALS_GENERATOR,
        ExternalServiceType.EXTERNAL_SERVICE_TYPE_PAYMENTS, PAYMENTS_CREDENTIALS_GENERATOR
    ), rateLimiters);
  }

  static Stream<Arguments> testSuccess() {
    return Stream.of(
        Arguments.of(ExternalServiceType.EXTERNAL_SERVICE_TYPE_DIRECTORY, DIRECTORY_CREDENTIALS_GENERATOR),
        Arguments.of(ExternalServiceType.EXTERNAL_SERVICE_TYPE_PAYMENTS, PAYMENTS_CREDENTIALS_GENERATOR)
    );
  }

  @ParameterizedTest
  @MethodSource
  public void testSuccess(
      final ExternalServiceType externalServiceType,
      final ExternalServiceCredentialsGenerator credentialsGenerator) throws Exception {
    final RateLimiter limiter = mock(RateLimiter.class);
    doReturn(limiter).when(rateLimiters).forDescriptor(eq(RateLimiters.For.EXTERNAL_SERVICE_CREDENTIALS));
    doReturn(Mono.fromFuture(CompletableFuture.completedFuture(null))).when(limiter).validateReactive(eq(AUTHENTICATED_ACI));
    final GetExternalServiceCredentialsResponse artResponse = authenticatedServiceStub().getExternalServiceCredentials(
        GetExternalServiceCredentialsRequest.newBuilder()
            .setExternalService(externalServiceType)
            .build());
    final Optional<Long> artValidation = credentialsGenerator.validateAndGetTimestamp(
        new ExternalServiceCredentials(artResponse.getUsername(), artResponse.getPassword()));
    assertTrue(artValidation.isPresent());
  }

  @ParameterizedTest
  @ValueSource(ints = { -1, 0, 1000 })
  public void testUnrecognizedService(final int externalServiceTypeValue) throws Exception {
    assertStatusException(Status.INVALID_ARGUMENT, () -> authenticatedServiceStub().getExternalServiceCredentials(
        GetExternalServiceCredentialsRequest.newBuilder()
            .setExternalServiceValue(externalServiceTypeValue)
            .build()));
  }

  @Test
  public void testInvalidRequest() throws Exception {
    assertStatusException(Status.INVALID_ARGUMENT, () -> authenticatedServiceStub().getExternalServiceCredentials(
        GetExternalServiceCredentialsRequest.newBuilder()
            .build()));
  }

  @Test
  public void testRateLimitExceeded() throws Exception {
    final Duration retryAfter = MockUtils.updateRateLimiterResponseToFail(
        rateLimiters, RateLimiters.For.EXTERNAL_SERVICE_CREDENTIALS, AUTHENTICATED_ACI, Duration.ofSeconds(100));
    Mockito.reset(DIRECTORY_CREDENTIALS_GENERATOR);
    assertRateLimitExceeded(
        retryAfter,
        () -> authenticatedServiceStub().getExternalServiceCredentials(
            GetExternalServiceCredentialsRequest.newBuilder()
                .setExternalService(ExternalServiceType.EXTERNAL_SERVICE_TYPE_DIRECTORY)
                .build()),
        DIRECTORY_CREDENTIALS_GENERATOR
    );
  }

  /**
   * `ExternalServiceDefinitions` enum is supposed to have entries for all values in `ExternalServiceType`,
   * except for the `EXTERNAL_SERVICE_TYPE_UNSPECIFIED` and `UNRECOGNIZED`.
   * This test makes sure that is the case.
   */
  @ParameterizedTest
  @EnumSource(mode = EnumSource.Mode.EXCLUDE, names = { "UNRECOGNIZED", "EXTERNAL_SERVICE_TYPE_UNSPECIFIED" })
  public void testHaveExternalServiceDefinitionForServiceTypes(final ExternalServiceType externalServiceType) throws Exception {
    assertTrue(
        Arrays.stream(ExternalServiceDefinitions.values()).anyMatch(v -> v.externalService() == externalServiceType),
        "`ExternalServiceDefinitions` enum entry is missing for the `%s` value of `ExternalServiceType`".formatted(externalServiceType)
    );
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.integration;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import io.zonarosa.integration.config.Config;
import io.zonarosa.server.metrics.NoopAwsSdkMetricPublisher;
import io.zonarosa.server.registration.VerificationSession;
import io.zonarosa.server.storage.PhoneNumberIdentifiers;
import io.zonarosa.server.storage.RegistrationRecoveryPasswords;
import io.zonarosa.server.storage.RegistrationRecoveryPasswordsManager;
import io.zonarosa.server.storage.VerificationSessionManager;
import io.zonarosa.server.storage.VerificationSessions;
import io.zonarosa.server.util.Util;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class IntegrationTools {

  private final RegistrationRecoveryPasswordsManager registrationRecoveryPasswordsManager;

  private final VerificationSessionManager verificationSessionManager;

  private final PhoneNumberIdentifiers phoneNumberIdentifiers;


  public static IntegrationTools create(final Config config) {
    final AwsCredentialsProvider credentialsProvider = DefaultCredentialsProvider.builder().build();

    final DynamoDbAsyncClient dynamoDbAsyncClient =
        config.dynamoDbClient().buildAsyncClient(credentialsProvider, new NoopAwsSdkMetricPublisher());

    final RegistrationRecoveryPasswords registrationRecoveryPasswords = new RegistrationRecoveryPasswords(
        config.dynamoDbTables().registrationRecovery(), Duration.ofDays(1), dynamoDbAsyncClient, Clock.systemUTC());

    final VerificationSessions verificationSessions = new VerificationSessions(
        dynamoDbAsyncClient, config.dynamoDbTables().verificationSessions(), Clock.systemUTC());

    return new IntegrationTools(
        new RegistrationRecoveryPasswordsManager(registrationRecoveryPasswords),
        new VerificationSessionManager(verificationSessions),
        new PhoneNumberIdentifiers(dynamoDbAsyncClient, config.dynamoDbTables().phoneNumberIdentifiers())
    );
  }

  private IntegrationTools(
      final RegistrationRecoveryPasswordsManager registrationRecoveryPasswordsManager,
      final VerificationSessionManager verificationSessionManager,
      final PhoneNumberIdentifiers phoneNumberIdentifiers) {
    this.registrationRecoveryPasswordsManager = registrationRecoveryPasswordsManager;
    this.verificationSessionManager = verificationSessionManager;
    this.phoneNumberIdentifiers = phoneNumberIdentifiers;
  }

  public CompletableFuture<Void> populateRecoveryPassword(final String phoneNumber, final byte[] password) {
    return phoneNumberIdentifiers
        .getPhoneNumberIdentifier(phoneNumber)
        .thenCompose(pni -> registrationRecoveryPasswordsManager.store(pni, password))
        .thenRun(Util.NOOP);
  }

  public CompletableFuture<Optional<String>> peekVerificationSessionPushChallenge(final String sessionId) {
    return verificationSessionManager.findForId(sessionId)
        .thenApply(maybeSession -> maybeSession.map(VerificationSession::pushChallenge));
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static java.util.Objects.requireNonNull;

import com.google.common.annotations.VisibleForTesting;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import io.zonarosa.chat.credentials.AuthCheckResult;
import io.zonarosa.chat.credentials.CheckSvrCredentialsRequest;
import io.zonarosa.chat.credentials.CheckSvrCredentialsResponse;
import io.zonarosa.chat.credentials.SimpleExternalServiceCredentialsAnonymousGrpc;
import io.zonarosa.server.WhisperServerConfiguration;
import io.zonarosa.server.auth.ExternalServiceCredentials;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.auth.ExternalServiceCredentialsSelector;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;

public class ExternalServiceCredentialsAnonymousGrpcService extends
    SimpleExternalServiceCredentialsAnonymousGrpc.ExternalServiceCredentialsAnonymousImplBase {

  private static final long MAX_SVR_PASSWORD_AGE_SECONDS = TimeUnit.DAYS.toSeconds(30);

  private final ExternalServiceCredentialsGenerator svrCredentialsGenerator;

  private final AccountsManager accountsManager;


  public static ExternalServiceCredentialsAnonymousGrpcService create(
      final AccountsManager accountsManager,
      final WhisperServerConfiguration chatConfiguration) {
    return new ExternalServiceCredentialsAnonymousGrpcService(
        accountsManager,
        ExternalServiceDefinitions.SVR.generatorFactory().apply(chatConfiguration, Clock.systemUTC())
    );
  }

  @VisibleForTesting
  ExternalServiceCredentialsAnonymousGrpcService(
      final AccountsManager accountsManager,
      final ExternalServiceCredentialsGenerator svrCredentialsGenerator) {
    this.accountsManager = requireNonNull(accountsManager);
    this.svrCredentialsGenerator = requireNonNull(svrCredentialsGenerator);
  }

  @Override
  public CheckSvrCredentialsResponse checkSvrCredentials(final CheckSvrCredentialsRequest request) {
    final List<String> tokens = request.getPasswordsList();
    if (tokens.size() > 10) {
      throw GrpcExceptions.fieldViolation("passwordsList", "At most 10 passwords may be provided");
    }
    final List<ExternalServiceCredentialsSelector.CredentialInfo> credentials = ExternalServiceCredentialsSelector.check(
        tokens,
        svrCredentialsGenerator,
        MAX_SVR_PASSWORD_AGE_SECONDS);
    // the username associated with the provided number
    final Optional<String> maybeUsername = accountsManager.getByE164(request.getNumber())
        .map(Account::getUuid)
        .map(svrCredentialsGenerator::generateForUuid)
        .map(ExternalServiceCredentials::username);
    final CheckSvrCredentialsResponse.Builder builder = CheckSvrCredentialsResponse.newBuilder();
    for (ExternalServiceCredentialsSelector.CredentialInfo credentialInfo : credentials) {
      final AuthCheckResult authCheckResult;
      if (!credentialInfo.valid()) {
        authCheckResult = AuthCheckResult.AUTH_CHECK_RESULT_INVALID;
      } else {
        final String username = credentialInfo.credentials().username();
        // does this credential match the account id for the e164 provided in the request?
        authCheckResult = maybeUsername.map(username::equals).orElse(false)
            ? AuthCheckResult.AUTH_CHECK_RESULT_MATCH
            : AuthCheckResult.AUTH_CHECK_RESULT_NO_MATCH;
      }
      builder.putMatches(credentialInfo.token(), authCheckResult);
    }
    return builder.build();
  }
}

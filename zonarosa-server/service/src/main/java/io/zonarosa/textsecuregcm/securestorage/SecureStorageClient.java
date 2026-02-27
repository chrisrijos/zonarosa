/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.securestorage;

import static io.zonarosa.server.util.HeaderUtils.basicAuthHeader;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import io.zonarosa.server.auth.ExternalServiceCredentials;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.configuration.SecureStorageServiceConfiguration;
import io.zonarosa.server.http.FaultTolerantHttpClient;
import io.zonarosa.server.util.HttpUtils;

/**
 * A client for sending requests to ZonaRosa's secure storage service on behalf of authenticated users.
 */
public class SecureStorageClient {

  private final ExternalServiceCredentialsGenerator storageServiceCredentialsGenerator;
  private final URI deleteUri;
  private final FaultTolerantHttpClient httpClient;

  @VisibleForTesting
  static final String DELETE_PATH = "/v1/storage";

  public SecureStorageClient(final ExternalServiceCredentialsGenerator storageServiceCredentialsGenerator,
      final Executor executor, final
  ScheduledExecutorService retryExecutor, final SecureStorageServiceConfiguration configuration)
      throws CertificateException {
    this.storageServiceCredentialsGenerator = storageServiceCredentialsGenerator;
    this.deleteUri = URI.create(configuration.uri()).resolve(DELETE_PATH);
    this.httpClient = FaultTolerantHttpClient.newBuilder("secure-storage", executor)
        .withCircuitBreaker(configuration.circuitBreakerConfigurationName())
        .withRetry(configuration.retryConfigurationName(), retryExecutor)
        .withVersion(HttpClient.Version.HTTP_1_1)
        .withConnectTimeout(Duration.ofSeconds(10))
        .withRedirect(HttpClient.Redirect.NEVER)
        .withSecurityProtocol(FaultTolerantHttpClient.SECURITY_PROTOCOL_TLS_1_3)
        .withTrustedServerCertificates(configuration.storageCaCertificates().toArray(new String[0]))
        .build();
  }

  public CompletableFuture<Void> deleteStoredData(final UUID accountUuid) {
    final ExternalServiceCredentials credentials = storageServiceCredentialsGenerator.generateForUuid(accountUuid);

    final HttpRequest request = HttpRequest.newBuilder()
        .uri(deleteUri)
        .DELETE()
        .header(HttpHeaders.AUTHORIZATION, basicAuthHeader(credentials))
        .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApply(response -> {
            if (HttpUtils.isSuccessfulResponse(response.statusCode())) {
                return null;
            }

            throw new SecureStorageException("Failed to delete storage service data: " + response.statusCode());
        });
    }
}

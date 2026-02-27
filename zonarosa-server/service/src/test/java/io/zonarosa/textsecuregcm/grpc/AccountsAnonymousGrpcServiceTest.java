/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.net.InetAddresses;
import com.google.protobuf.ByteString;
import io.grpc.Status;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import io.zonarosa.chat.account.AccountsAnonymousGrpc;
import io.zonarosa.chat.account.CheckAccountExistenceRequest;
import io.zonarosa.chat.account.LookupUsernameHashRequest;
import io.zonarosa.chat.account.LookupUsernameHashResponse;
import io.zonarosa.chat.account.LookupUsernameLinkRequest;
import io.zonarosa.chat.account.LookupUsernameLinkResponse;
import io.zonarosa.chat.common.IdentityType;
import io.zonarosa.chat.errors.NotFound;
import io.zonarosa.chat.common.ServiceIdentifier;
import io.zonarosa.server.controllers.AccountController;
import io.zonarosa.server.controllers.RateLimitExceededException;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.limits.RateLimiter;
import io.zonarosa.server.limits.RateLimiters;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.util.TestRandomUtil;
import io.zonarosa.server.util.UUIDUtil;
import reactor.core.publisher.Mono;

class AccountsAnonymousGrpcServiceTest extends
    SimpleBaseGrpcTest<AccountsAnonymousGrpcService, AccountsAnonymousGrpc.AccountsAnonymousBlockingStub> {

  @Mock
  private AccountsManager accountsManager;

  @Mock
  private RateLimiters rateLimiters;

  @Mock
  private RateLimiter rateLimiter;

  @Override
  protected AccountsAnonymousGrpcService createServiceBeforeEachTest() {
    when(accountsManager.getByServiceIdentifierAsync(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    when(accountsManager.getByUsernameHash(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    when(accountsManager.getByUsernameLinkHandle(any()))
        .thenReturn(CompletableFuture.completedFuture(Optional.empty()));

    when(rateLimiters.getCheckAccountExistenceLimiter()).thenReturn(rateLimiter);
    when(rateLimiters.getUsernameLookupLimiter()).thenReturn(rateLimiter);
    when(rateLimiters.getUsernameLinkLookupLimiter()).thenReturn(rateLimiter);

    when(rateLimiter.validateReactive(anyString())).thenReturn(Mono.empty());

    getMockRequestAttributesInterceptor().setRequestAttributes(
        new RequestAttributes(InetAddresses.forString("127.0.0.1"), null, null));

    return new AccountsAnonymousGrpcService(accountsManager, rateLimiters);
  }

  @Test
  void checkAccountExistence() {
    final AciServiceIdentifier serviceIdentifier = new AciServiceIdentifier(UUID.randomUUID());

    when(accountsManager.getByServiceIdentifierAsync(serviceIdentifier))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(mock(Account.class))));

    assertTrue(unauthenticatedServiceStub().checkAccountExistence(CheckAccountExistenceRequest.newBuilder()
        .setServiceIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(serviceIdentifier))
        .build()).getAccountExists());

    assertFalse(unauthenticatedServiceStub().checkAccountExistence(CheckAccountExistenceRequest.newBuilder()
        .setServiceIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(new AciServiceIdentifier(UUID.randomUUID())))
        .build()).getAccountExists());
  }

  @ParameterizedTest
  @MethodSource
  void checkAccountExistenceIllegalRequest(final CheckAccountExistenceRequest request) {
    //noinspection ResultOfMethodCallIgnored
    GrpcTestUtils.assertStatusException(Status.INVALID_ARGUMENT,
        () -> unauthenticatedServiceStub().checkAccountExistence(request));
  }

  private static Stream<Arguments> checkAccountExistenceIllegalRequest() {
    return Stream.of(
        // No service identifier
        Arguments.of(CheckAccountExistenceRequest.newBuilder().build()),

        // Bad service identifier
        Arguments.of(CheckAccountExistenceRequest.newBuilder()
            .setServiceIdentifier(ServiceIdentifier.newBuilder()
                .setIdentityType(IdentityType.IDENTITY_TYPE_ACI)
                .setUuid(ByteString.copyFrom(new byte[15]))
                .build())
            .build())
    );
  }

  @Test
  void checkAccountExistenceRateLimited() {
    final Duration retryAfter = Duration.ofSeconds(11);

    when(rateLimiter.validateReactive(anyString()))
        .thenReturn(Mono.error(new RateLimitExceededException(retryAfter)));

    //noinspection ResultOfMethodCallIgnored
    GrpcTestUtils.assertRateLimitExceeded(retryAfter,
        () -> unauthenticatedServiceStub().checkAccountExistence(CheckAccountExistenceRequest.newBuilder()
            .setServiceIdentifier(ServiceIdentifierUtil.toGrpcServiceIdentifier(new AciServiceIdentifier(UUID.randomUUID())))
            .build()),
        accountsManager);
  }

  @Test
  void lookupUsernameHash() {
    final UUID accountIdentifier = UUID.randomUUID();

    final byte[] usernameHash = TestRandomUtil.nextBytes(AccountController.USERNAME_HASH_LENGTH);

    final Account account = mock(Account.class);
    when(account.getUuid()).thenReturn(accountIdentifier);

    when(accountsManager.getByUsernameHash(usernameHash))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

    assertEquals(ServiceIdentifierUtil.toGrpcServiceIdentifier(new AciServiceIdentifier(accountIdentifier)),
        unauthenticatedServiceStub().lookupUsernameHash(LookupUsernameHashRequest.newBuilder()
                .setUsernameHash(ByteString.copyFrom(usernameHash))
                .build())
            .getServiceIdentifier());

    //noinspection ResultOfMethodCallIgnored
    assertEquals(LookupUsernameHashResponse.newBuilder().setNotFound(NotFound.getDefaultInstance()).build(),
        unauthenticatedServiceStub().lookupUsernameHash(LookupUsernameHashRequest.newBuilder()
            .setUsernameHash(ByteString.copyFrom(new byte[AccountController.USERNAME_HASH_LENGTH]))
            .build()));
  }

  @ParameterizedTest
  @MethodSource
  void lookupUsernameHashIllegalHash(final LookupUsernameHashRequest request) {
    //noinspection ResultOfMethodCallIgnored
    GrpcTestUtils.assertStatusException(Status.INVALID_ARGUMENT,
        () -> unauthenticatedServiceStub().lookupUsernameHash(request));
  }

  private static Stream<Arguments> lookupUsernameHashIllegalHash() {
    return Stream.of(
        // No username hash
        Arguments.of(LookupUsernameHashRequest.newBuilder().build()),

        // Hash too long
        Arguments.of(LookupUsernameHashRequest.newBuilder()
            .setUsernameHash(ByteString.copyFrom(new byte[AccountController.USERNAME_HASH_LENGTH + 1]))
            .build()),

        // Hash too short
        Arguments.of(LookupUsernameHashRequest.newBuilder()
            .setUsernameHash(ByteString.copyFrom(new byte[AccountController.USERNAME_HASH_LENGTH - 1]))
            .build())
    );
  }

  @Test
  void lookupUsernameHashRateLimited() {
    final Duration retryAfter = Duration.ofSeconds(13);

    when(rateLimiter.validateReactive(anyString()))
        .thenReturn(Mono.error(new RateLimitExceededException(retryAfter)));

    //noinspection ResultOfMethodCallIgnored
    GrpcTestUtils.assertRateLimitExceeded(retryAfter,
        () -> unauthenticatedServiceStub().lookupUsernameHash(LookupUsernameHashRequest.newBuilder()
            .setUsernameHash(ByteString.copyFrom(new byte[AccountController.USERNAME_HASH_LENGTH]))
            .build()),
        accountsManager);
  }

  @Test
  void lookupUsernameLink() {
    final UUID linkHandle = UUID.randomUUID();

    final byte[] usernameCiphertext = TestRandomUtil.nextBytes(32);

    final Account account = mock(Account.class);
    when(account.getEncryptedUsername()).thenReturn(Optional.of(usernameCiphertext));

    when(accountsManager.getByUsernameLinkHandle(linkHandle))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));

    assertEquals(ByteString.copyFrom(usernameCiphertext),
        unauthenticatedServiceStub().lookupUsernameLink(LookupUsernameLinkRequest.newBuilder()
                .setUsernameLinkHandle(UUIDUtil.toByteString(linkHandle))
                .build())
            .getUsernameCiphertext());

    when(account.getEncryptedUsername()).thenReturn(Optional.empty());

    final LookupUsernameLinkResponse notFoundResponse = LookupUsernameLinkResponse.newBuilder()
        .setNotFound(NotFound.getDefaultInstance())
        .build();

    assertEquals(notFoundResponse,
        unauthenticatedServiceStub().lookupUsernameLink(LookupUsernameLinkRequest.newBuilder()
            .setUsernameLinkHandle(UUIDUtil.toByteString(linkHandle))
            .build()));
    assertEquals(notFoundResponse,
        unauthenticatedServiceStub().lookupUsernameLink(LookupUsernameLinkRequest.newBuilder()
            .setUsernameLinkHandle(UUIDUtil.toByteString(UUID.randomUUID()))
            .build()));
  }

  @ParameterizedTest
  @MethodSource
  void lookupUsernameLinkIllegalHandle(final LookupUsernameLinkRequest request) {
    //noinspection ResultOfMethodCallIgnored
    GrpcTestUtils.assertStatusException(Status.INVALID_ARGUMENT,
        () -> unauthenticatedServiceStub().lookupUsernameLink(request));
  }

  private static Stream<Arguments> lookupUsernameLinkIllegalHandle() {
    return Stream.of(
        // No handle
        Arguments.of(LookupUsernameLinkRequest.newBuilder().build()),

        // Bad handle length
        Arguments.of(LookupUsernameLinkRequest.newBuilder()
            .setUsernameLinkHandle(ByteString.copyFrom(new byte[15]))
            .build())
    );
  }

  @Test
  void lookupUsernameLinkRateLimited() {
    final Duration retryAfter = Duration.ofSeconds(17);

    when(rateLimiter.validateReactive(anyString()))
        .thenReturn(Mono.error(new RateLimitExceededException(retryAfter)));

    //noinspection ResultOfMethodCallIgnored
    GrpcTestUtils.assertRateLimitExceeded(retryAfter,
        () -> unauthenticatedServiceStub().lookupUsernameLink(LookupUsernameLinkRequest.newBuilder()
            .setUsernameLinkHandle(UUIDUtil.toByteString(UUID.randomUUID()))
            .build()),
        accountsManager);
  }
}

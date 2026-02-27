/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import io.zonarosa.chat.backup.BackupsGrpc;
import io.zonarosa.chat.backup.GetBackupAuthCredentialsRequest;
import io.zonarosa.chat.backup.GetBackupAuthCredentialsResponse;
import io.zonarosa.chat.backup.RedeemReceiptRequest;
import io.zonarosa.chat.backup.RedeemReceiptResponse;
import io.zonarosa.chat.backup.SetBackupIdRequest;
import io.zonarosa.chat.common.ZkCredential;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupAuthCredentialRequest;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupCredentialType;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupLevel;
import io.zonarosa.libzonarosa.zkgroup.receipts.ClientZkReceiptOperations;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredential;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialRequestContext;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialResponse;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptSerial;
import io.zonarosa.libzonarosa.zkgroup.receipts.ServerZkReceiptOperations;
import io.zonarosa.server.auth.RedemptionRange;
import io.zonarosa.server.backup.BackupAuthManager;
import io.zonarosa.server.backup.BackupAuthTestUtil;
import io.zonarosa.server.backup.BackupBadReceiptException;
import io.zonarosa.server.backup.BackupException;
import io.zonarosa.server.backup.BackupInvalidArgumentException;
import io.zonarosa.server.backup.BackupMissingIdCommitmentException;
import io.zonarosa.server.backup.BackupNotFoundException;
import io.zonarosa.server.backup.BackupPermissionException;
import io.zonarosa.server.controllers.RateLimitExceededException;
import io.zonarosa.server.metrics.BackupMetrics;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.Device;
import io.zonarosa.server.util.EnumMapUtil;
import io.zonarosa.server.util.TestRandomUtil;
import javax.annotation.Nullable;

class BackupsGrpcServiceTest extends SimpleBaseGrpcTest<BackupsGrpcService, BackupsGrpc.BackupsBlockingStub> {

  private final byte[] messagesBackupKey = TestRandomUtil.nextBytes(32);
  private final byte[] mediaBackupKey = TestRandomUtil.nextBytes(32);
  private final BackupAuthTestUtil backupAuthTestUtil = new BackupAuthTestUtil(Clock.systemUTC());
  final BackupAuthCredentialRequest mediaAuthCredRequest =
      backupAuthTestUtil.getRequest(mediaBackupKey, AUTHENTICATED_ACI);
  final BackupAuthCredentialRequest messagesAuthCredRequest =
      backupAuthTestUtil.getRequest(messagesBackupKey, AUTHENTICATED_ACI);
  private Account account;
  private Device device;

  @Mock
  private BackupAuthManager backupAuthManager;
  @Mock
  private AccountsManager accountsManager;

  @Override
  protected BackupsGrpcService createServiceBeforeEachTest() {
    return new BackupsGrpcService(accountsManager, backupAuthManager, new BackupMetrics());
  }

  @BeforeEach
  void setup() {
    account = mock(Account.class);
    device = mock(Device.class);
    when(device.isPrimary()).thenReturn(true);
    when(accountsManager.getByAccountIdentifierAsync(AUTHENTICATED_ACI))
        .thenReturn(CompletableFuture.completedFuture(Optional.of(account)));
    when(accountsManager.getByAccountIdentifier(AUTHENTICATED_ACI))
        .thenReturn(Optional.of(account));
    when(account.getDevice(AUTHENTICATED_DEVICE_ID)).thenReturn(Optional.of(device));
  }


  @Test
  void setBackupId() throws RateLimitExceededException, BackupInvalidArgumentException, BackupPermissionException {
    authenticatedServiceStub().setBackupId(
        SetBackupIdRequest.newBuilder()
            .setMediaBackupAuthCredentialRequest(ByteString.copyFrom(mediaAuthCredRequest.serialize()))
            .setMessagesBackupAuthCredentialRequest(ByteString.copyFrom(messagesAuthCredRequest.serialize()))
            .build());

    verify(backupAuthManager)
        .commitBackupId(account, device, Optional.of(messagesAuthCredRequest), Optional.of(mediaAuthCredRequest));
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void setBackupIdPartial(boolean media)
      throws RateLimitExceededException, BackupInvalidArgumentException, BackupPermissionException {
    final SetBackupIdRequest.Builder builder = SetBackupIdRequest.newBuilder();
    if (media) {
      builder.setMediaBackupAuthCredentialRequest(ByteString.copyFrom(mediaAuthCredRequest.serialize()));
    } else {
      builder.setMessagesBackupAuthCredentialRequest(ByteString.copyFrom(messagesAuthCredRequest.serialize()));
    }
    authenticatedServiceStub().setBackupId(builder.build());
    verify(backupAuthManager)
        .commitBackupId(account, device,
            Optional.ofNullable(media ? null : messagesAuthCredRequest),
            Optional.ofNullable(media ? mediaAuthCredRequest: null));
  }

  @Test
  void setBackupIdInvalid() {
    // invalid serialization
    GrpcTestUtils.assertStatusException(
        Status.INVALID_ARGUMENT, () -> authenticatedServiceStub().setBackupId(
            SetBackupIdRequest.newBuilder()
                .setMessagesBackupAuthCredentialRequest(ByteString.fromHex("FF"))
                .setMediaBackupAuthCredentialRequest(ByteString.fromHex("FF"))
                .build())
    );

  }

  public static Stream<Arguments> setBackupIdException() {
    return Stream.of(
        Arguments.of(new RateLimitExceededException(null), Status.RESOURCE_EXHAUSTED),
        Arguments.of(new BackupPermissionException("test"), Status.INVALID_ARGUMENT),
        Arguments.of(new BackupInvalidArgumentException("test"), Status.INVALID_ARGUMENT));
  }

  @ParameterizedTest
  @MethodSource
  void setBackupIdException(final Exception ex, final Status expected)
      throws RateLimitExceededException, BackupInvalidArgumentException, BackupPermissionException {
    doThrow(ex).when(backupAuthManager).commitBackupId(any(), any(), any(), any());

    GrpcTestUtils.assertStatusException(
        expected, () -> authenticatedServiceStub().setBackupId(SetBackupIdRequest.newBuilder()
            .setMediaBackupAuthCredentialRequest(ByteString.copyFrom(mediaAuthCredRequest.serialize()))
            .setMessagesBackupAuthCredentialRequest(ByteString.copyFrom(messagesAuthCredRequest.serialize()))
            .build())
    );
  }

  public static Stream<Arguments> redeemReceipt() {
    return Stream.of(
        Arguments.of(null, RedeemReceiptResponse.OutcomeCase.SUCCESS),
        Arguments.of(new BackupBadReceiptException("test"), RedeemReceiptResponse.OutcomeCase.INVALID_RECEIPT),
        Arguments.of(new BackupMissingIdCommitmentException(), RedeemReceiptResponse.OutcomeCase.ACCOUNT_MISSING_COMMITMENT));
  }

  @ParameterizedTest
  @MethodSource
  void redeemReceipt(@Nullable final BackupException exception, final RedeemReceiptResponse.OutcomeCase expectedOutcome)
      throws InvalidInputException, VerificationFailedException, BackupInvalidArgumentException, BackupMissingIdCommitmentException, BackupBadReceiptException {

    final ServerSecretParams params = ServerSecretParams.generate();
    final ServerZkReceiptOperations serverOps = new ServerZkReceiptOperations(params);
    final ClientZkReceiptOperations clientOps = new ClientZkReceiptOperations(params.getPublicParams());
    final ReceiptCredentialRequestContext rcrc = clientOps
        .createReceiptCredentialRequestContext(new ReceiptSerial(TestRandomUtil.nextBytes(ReceiptSerial.SIZE)));
    final ReceiptCredentialResponse rcr = serverOps.issueReceiptCredential(rcrc.getRequest(), 0L, 3L);
    final ReceiptCredential receiptCredential = clientOps.receiveReceiptCredential(rcrc, rcr);
    final ReceiptCredentialPresentation presentation = clientOps.createReceiptCredentialPresentation(receiptCredential);

    if (exception != null) {
      doThrow(exception).when(backupAuthManager).redeemReceipt(any(), any());
    }

    final RedeemReceiptResponse redeemReceiptResponse = authenticatedServiceStub().redeemReceipt(
        RedeemReceiptRequest.newBuilder()
            .setPresentation(ByteString.copyFrom(presentation.serialize()))
            .build());
    assertThat(redeemReceiptResponse.getOutcomeCase()).isEqualTo(expectedOutcome);

    verify(backupAuthManager).redeemReceipt(account, presentation);
  }


  @Test
  void getCredentials() throws BackupNotFoundException {
    final Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
    final Instant end = start.plus(Duration.ofDays(1));
    final RedemptionRange expectedRange = RedemptionRange.inclusive(Clock.systemUTC(), start, end);

    final Map<BackupCredentialType, List<BackupAuthManager.Credential>> expectedCredentialsByType =
        EnumMapUtil.toEnumMap(BackupCredentialType.class, credentialType -> backupAuthTestUtil.getCredentials(
            BackupLevel.PAID, backupAuthTestUtil.getRequest(messagesBackupKey, AUTHENTICATED_ACI), credentialType,
            start, end));

    when(backupAuthManager.getBackupAuthCredentials(any(), eq(expectedRange)))
        .thenReturn(expectedCredentialsByType);

    final GetBackupAuthCredentialsResponse credentialResponse = authenticatedServiceStub().getBackupAuthCredentials(
        GetBackupAuthCredentialsRequest.newBuilder()
            .setRedemptionStart(start.getEpochSecond()).setRedemptionStop(end.getEpochSecond())
            .build());

    expectedCredentialsByType.forEach((credentialType, expectedCredentials) -> {

      final Map<Long, ZkCredential> creds = switch (credentialType) {
        case MESSAGES -> credentialResponse.getCredentials().getMessageCredentialsMap();
        case MEDIA -> credentialResponse.getCredentials().getMediaCredentialsMap();
      };
      assertThat(creds).hasSize(expectedCredentials.size()).containsKey(start.getEpochSecond());

      for (BackupAuthManager.Credential expectedCred : expectedCredentials) {
        assertThat(creds)
            .extractingByKey(expectedCred.redemptionTime().getEpochSecond())
            .isNotNull()
            .extracting(ZkCredential::getCredential)
            .extracting(ByteString::toByteArray)
            .isEqualTo(expectedCred.credential().serialize());
      }
    });
  }

  @ParameterizedTest
  @CsvSource({
      "true, false",
      "false, true",
      "true, true"
  })
  void getCredentialsBadInput(final boolean missingStart, final boolean missingEnd) {
    final Instant start = Instant.now().truncatedTo(ChronoUnit.DAYS);
    final Instant end = start.plus(Duration.ofDays(1));

    final GetBackupAuthCredentialsRequest.Builder builder = GetBackupAuthCredentialsRequest.newBuilder();
    if (!missingStart) {
      builder.setRedemptionStart(start.getEpochSecond());
    }
    if (!missingEnd) {
      builder.setRedemptionStop(end.getEpochSecond());
    }

    GrpcTestUtils.assertStatusException(Status.INVALID_ARGUMENT,
        () -> authenticatedServiceStub().getBackupAuthCredentials(builder.build()));
  }

}

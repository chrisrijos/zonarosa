/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.grpc;

import com.google.protobuf.ByteString;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.micrometer.core.instrument.Tag;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import io.zonarosa.chat.backup.GetBackupAuthCredentialsRequest;
import io.zonarosa.chat.backup.GetBackupAuthCredentialsResponse;
import io.zonarosa.chat.backup.RedeemReceiptRequest;
import io.zonarosa.chat.backup.RedeemReceiptResponse;
import io.zonarosa.chat.backup.SetBackupIdRequest;
import io.zonarosa.chat.backup.SetBackupIdResponse;
import io.zonarosa.chat.backup.SimpleBackupsGrpc;
import io.zonarosa.chat.common.ZkCredential;
import io.zonarosa.chat.errors.FailedPrecondition;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupAuthCredentialRequest;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupCredentialType;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialPresentation;
import io.zonarosa.server.auth.RedemptionRange;
import io.zonarosa.server.auth.grpc.AuthenticatedDevice;
import io.zonarosa.server.auth.grpc.AuthenticationUtil;
import io.zonarosa.server.backup.BackupAuthManager;
import io.zonarosa.server.backup.BackupBadReceiptException;
import io.zonarosa.server.backup.BackupInvalidArgumentException;
import io.zonarosa.server.backup.BackupMissingIdCommitmentException;
import io.zonarosa.server.backup.BackupNotFoundException;
import io.zonarosa.server.backup.BackupPermissionException;
import io.zonarosa.server.backup.BackupWrongCredentialTypeException;
import io.zonarosa.server.controllers.RateLimitExceededException;
import io.zonarosa.server.metrics.BackupMetrics;
import io.zonarosa.server.metrics.UserAgentTagUtil;
import io.zonarosa.server.storage.Account;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.Device;

public class BackupsGrpcService extends SimpleBackupsGrpc.BackupsImplBase {

  private final AccountsManager accountManager;
  private final BackupAuthManager backupAuthManager;
  private final BackupMetrics backupMetrics;

  public BackupsGrpcService(final AccountsManager accountManager, final BackupAuthManager backupAuthManager, final BackupMetrics backupMetrics) {
    this.accountManager = accountManager;
    this.backupAuthManager = backupAuthManager;
    this.backupMetrics = backupMetrics;
  }

  @Override
  public SetBackupIdResponse setBackupId(SetBackupIdRequest request)
      throws RateLimitExceededException, BackupInvalidArgumentException, BackupPermissionException {

    final Optional<BackupAuthCredentialRequest> messagesCredentialRequest = deserializeWithEmptyPresenceCheck(
        BackupAuthCredentialRequest::new,
        request.getMessagesBackupAuthCredentialRequest());

    final Optional<BackupAuthCredentialRequest> mediaCredentialRequest = deserializeWithEmptyPresenceCheck(
        BackupAuthCredentialRequest::new,
        request.getMediaBackupAuthCredentialRequest());

    final AuthenticatedDevice authenticatedDevice = AuthenticationUtil.requireAuthenticatedDevice();
    final Account account = authenticatedAccount();
    final Device device = account
        .getDevice(authenticatedDevice.deviceId())
        .orElseThrow(Status.UNAUTHENTICATED::asRuntimeException);
    backupAuthManager.commitBackupId(account, device, messagesCredentialRequest, mediaCredentialRequest);
    return SetBackupIdResponse.getDefaultInstance();
  }

  public RedeemReceiptResponse redeemReceipt(RedeemReceiptRequest request) throws BackupInvalidArgumentException {
    final ReceiptCredentialPresentation receiptCredentialPresentation = deserialize(
        ReceiptCredentialPresentation::new,
        request.getPresentation().toByteArray());
    final Account account = authenticatedAccount();
    final RedeemReceiptResponse.Builder builder = RedeemReceiptResponse.newBuilder();
    try {
      backupAuthManager.redeemReceipt(account, receiptCredentialPresentation);
      builder.setSuccess(Empty.getDefaultInstance());
    } catch (BackupBadReceiptException e) {
      builder.setInvalidReceipt(FailedPrecondition.newBuilder().setDescription(e.getMessage()).build());
    } catch (BackupMissingIdCommitmentException e) {
      builder.setAccountMissingCommitment(FailedPrecondition.newBuilder().build());
    }
    return builder.build();
  }

  @Override
  public GetBackupAuthCredentialsResponse getBackupAuthCredentials(GetBackupAuthCredentialsRequest request) {
    final Tag platformTag = UserAgentTagUtil.getPlatformTag(RequestAttributesUtil.getUserAgent().orElse(null));
    final RedemptionRange redemptionRange;
    try {
      redemptionRange = RedemptionRange.inclusive(Clock.systemUTC(),
          Instant.ofEpochSecond(request.getRedemptionStart()),
          Instant.ofEpochSecond(request.getRedemptionStop()));
    } catch (IllegalArgumentException e) {
      throw Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException();
    }
    final Account account = authenticatedAccount();
    try {

      final Map<BackupCredentialType, List<BackupAuthManager.Credential>> credentials =
          backupAuthManager.getBackupAuthCredentials(account, redemptionRange);

      credentials.forEach((type, credentialList) ->
          backupMetrics.updateGetCredentialCounter(platformTag, type, credentialList.size()));
      final List<BackupAuthManager.Credential> messageCredentials = credentials.get(BackupCredentialType.MESSAGES);
      final List<BackupAuthManager.Credential> mediaCredentials = credentials.get(BackupCredentialType.MEDIA);

      return GetBackupAuthCredentialsResponse.newBuilder()
          .setCredentials(GetBackupAuthCredentialsResponse.Credentials.newBuilder()
              .putAllMessageCredentials(messageCredentials.stream().collect(Collectors.toMap(
                  c -> c.redemptionTime().getEpochSecond(),
                  c -> ZkCredential.newBuilder()
                      .setCredential(ByteString.copyFrom(c.credential().serialize()))
                      .setRedemptionTime(c.redemptionTime().getEpochSecond())
                      .build())))
              .putAllMediaCredentials(mediaCredentials.stream().collect(Collectors.toMap(
                  c -> c.redemptionTime().getEpochSecond(),
                  c -> ZkCredential.newBuilder()
                      .setCredential(ByteString.copyFrom(c.credential().serialize()))
                      .setRedemptionTime(c.redemptionTime().getEpochSecond())
                      .build())))
              .build())
          .build();
    } catch (BackupNotFoundException _) {
      // Return an empty response to indicate that the authenticated account had no associated blinded backup-id
      return GetBackupAuthCredentialsResponse.getDefaultInstance();
    }
  }

  @Override
  public Throwable mapException(final Throwable throwable) {
    return switch (throwable) {
      case BackupInvalidArgumentException e -> GrpcExceptions.invalidArguments(e.getMessage());
      case BackupPermissionException e -> GrpcExceptions.badAuthentication(e.getMessage());
      case BackupWrongCredentialTypeException e -> GrpcExceptions.badAuthentication(e.getMessage());
      default -> throwable;
    };
  }

  private Account authenticatedAccount() {
    return accountManager
        .getByAccountIdentifier(AuthenticationUtil.requireAuthenticatedDevice().accountIdentifier())
        .orElseThrow(Status.UNAUTHENTICATED::asRuntimeException);
  }

  private interface Deserializer<T> {

    T deserialize(byte[] bytes) throws InvalidInputException;
  }

  private <T> Optional<T> deserializeWithEmptyPresenceCheck(Deserializer<T> deserializer, ByteString byteString) {
    if (byteString.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(deserialize(deserializer, byteString.toByteArray()));
  }

  private <T> T deserialize(Deserializer<T> deserializer, byte[] bytes) {
    try {
      return deserializer.deserialize(bytes);
    } catch (InvalidInputException e) {
      throw GrpcExceptions.invalidArguments("invalid serialization");
    }
  }

}

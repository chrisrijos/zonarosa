/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.backup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import io.zonarosa.libzonarosa.zkgroup.GenericServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupAuthCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupAuthCredentialRequest;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupAuthCredentialRequestContext;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupCredentialType;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupLevel;
import io.zonarosa.server.auth.RedemptionRange;
import io.zonarosa.server.experiment.ExperimentEnrollmentManager;
import io.zonarosa.server.storage.Account;

public class BackupAuthTestUtil {

  final GenericServerSecretParams params = GenericServerSecretParams.generate();
  final Clock clock;

  public BackupAuthTestUtil(final Clock clock) {
    this.clock = clock;
  }

  public BackupAuthCredentialRequest getRequest(final byte[] backupKey, final UUID aci) {
    return BackupAuthCredentialRequestContext.create(backupKey, aci).getRequest();
  }

  public BackupAuthCredentialPresentation getPresentation(
      final BackupLevel backupLevel, final byte[] backupKey, final UUID aci)
      throws VerificationFailedException {
    return getPresentation(params, backupLevel, backupKey, aci);
  }

  public BackupAuthCredentialPresentation getPresentation(
      GenericServerSecretParams params, final BackupLevel backupLevel, final byte[] backupKey, final UUID aci)
      throws VerificationFailedException {
    final Instant redemptionTime = clock.instant().truncatedTo(ChronoUnit.DAYS);
    final BackupAuthCredentialRequestContext ctx = BackupAuthCredentialRequestContext.create(backupKey, aci);
    return ctx.receiveResponse(
            ctx.getRequest()
                .issueCredential(clock.instant().truncatedTo(ChronoUnit.DAYS), backupLevel, BackupCredentialType.MESSAGES, params),
            redemptionTime,
            params.getPublicParams())
        .present(params.getPublicParams());
  }

  public List<BackupAuthManager.Credential> getCredentials(
      final BackupLevel backupLevel,
      final BackupAuthCredentialRequest request,
      final BackupCredentialType credentialType,
      final Instant redemptionStart,
      final Instant redemptionEnd) {
    final UUID aci = UUID.randomUUID();

    final BackupAuthManager issuer = new BackupAuthManager(
        mock(ExperimentEnrollmentManager.class), null, null, null, null, params, clock);
    Account account = mock(Account.class);
    when(account.getUuid()).thenReturn(aci);
    when(account.getBackupCredentialRequest(any())).thenReturn(Optional.of(request.serialize()));
    when(account.getBackupVoucher()).thenReturn(switch (backupLevel) {
      case FREE -> null;
      case PAID -> new Account.BackupVoucher(201L, redemptionEnd.plus(1, ChronoUnit.SECONDS));
    });
    final RedemptionRange redemptionRange;
    redemptionRange = RedemptionRange.inclusive(clock, redemptionStart, redemptionEnd);
    try {
      return issuer.getBackupAuthCredentials(account, redemptionRange).get(credentialType);
    } catch (BackupNotFoundException e) {
      return Assertions.fail("Backup credential request not found even though we set one");
    }
  }
}

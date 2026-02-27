/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.metrics;

import static io.zonarosa.server.metrics.MetricsUtil.name;

import com.google.common.annotations.VisibleForTesting;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.zonarosa.libzonarosa.zkgroup.backups.BackupCredentialType;
import io.zonarosa.server.auth.AuthenticatedBackupUser;
import io.zonarosa.server.backup.CopyResult;
import java.util.Optional;

public class BackupMetrics {

  private final static String COPY_MEDIA_COUNTER_NAME = name(BackupMetrics.class, "copyMedia");
  private final static String GET_BACKUP_CREDENTIALS_NAME = name(BackupMetrics.class, "getBackupCredentials");
  private final static String MESSAGE_BACKUP_SIZE_NAME = name(BackupMetrics.class, "messageBackupSize");


  private MeterRegistry registry;

  public BackupMetrics() {
    this(Metrics.globalRegistry);
  }

  @VisibleForTesting
  BackupMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void updateCopyCounter(final CopyResult copyResult, final Tag platformTag) {
    registry.counter(COPY_MEDIA_COUNTER_NAME, Tags.of(
            platformTag,
            Tag.of("outcome", copyResult.outcome().name().toLowerCase())))
        .increment();
  }

  public void updateGetCredentialCounter(final Tag platformTag, BackupCredentialType credentialType,
      final int numCredentials) {
    Metrics.counter(GET_BACKUP_CREDENTIALS_NAME, Tags.of(
            platformTag,
            Tag.of("num", Integer.toString(numCredentials)),
            Tag.of("type", credentialType.name().toLowerCase())))
        .increment();
  }

  public void updateMessageBackupSizeDistribution(
      AuthenticatedBackupUser authenticatedBackupUser,
      final boolean oversize,
      final Optional<Long> backupLength) {
    DistributionSummary.builder(MESSAGE_BACKUP_SIZE_NAME)
        .tags(Tags.of(
            UserAgentTagUtil.getPlatformTag(authenticatedBackupUser.userAgent()),
            Tag.of("tier", authenticatedBackupUser.backupLevel().name().toLowerCase()),
            Tag.of("oversize", Boolean.toString(oversize)),
            Tag.of("hasBackupLength", Boolean.toString(backupLength.isPresent()))))
        .register(Metrics.globalRegistry)
        .record(backupLength.orElse(0L));
  }

}

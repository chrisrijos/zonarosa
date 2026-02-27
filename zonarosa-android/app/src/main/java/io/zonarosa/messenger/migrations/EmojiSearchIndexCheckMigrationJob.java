package io.zonarosa.messenger.migrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.SqlUtil;
import io.zonarosa.messenger.database.EmojiSearchTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobs.EmojiSearchIndexDownloadJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;

/**
 * Schedules job to get the latest emoji search index if it's empty.
 */
public final class EmojiSearchIndexCheckMigrationJob extends MigrationJob {

  public static final String KEY = "EmojiSearchIndexCheckMigrationJob";

  EmojiSearchIndexCheckMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private EmojiSearchIndexCheckMigrationJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public boolean isUiBlocking() {
    return false;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void performMigration() {
    if (SqlUtil.isEmpty(ZonaRosaDatabase.getRawDatabase(), EmojiSearchTable.TABLE_NAME)) {
      ZonaRosaStore.emoji().clearSearchIndexMetadata();
      EmojiSearchIndexDownloadJob.scheduleImmediately();
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<EmojiSearchIndexCheckMigrationJob> {
    @Override
    public @NonNull EmojiSearchIndexCheckMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new EmojiSearchIndexCheckMigrationJob(parameters);
    }
  }
}

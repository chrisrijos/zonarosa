package io.zonarosa.messenger.migrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobs.StorageSyncJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;

/**
 * Marks the AccountRecord as dirty and runs a storage sync. Can be enqueued when we've added a new
 * attribute to the AccountRecord.
 */
public class AccountRecordMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(AccountRecordMigrationJob.class);

  public static final String KEY = "AccountRecordMigrationJob";

  AccountRecordMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private AccountRecordMigrationJob(@NonNull Parameters parameters) {
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
    if (!ZonaRosaStore.account().isRegistered() || ZonaRosaStore.account().getAci() == null) {
      Log.w(TAG, "Not registered!");
      return;
    }

    ZonaRosaDatabase.recipients().markNeedsSync(Recipient.self().getId());
    AppDependencies.getJobManager().add(StorageSyncJob.forLocalChange());
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<AccountRecordMigrationJob> {
    @Override
    public @NonNull AccountRecordMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new AccountRecordMigrationJob(parameters);
    }
  }
}

package io.zonarosa.messenger.migrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.contacts.sync.ContactDiscovery;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;

import java.io.IOException;

/**
 * Does a full directory refresh.
 */
public final class DirectoryRefreshMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(DirectoryRefreshMigrationJob.class);

  public static final String KEY = "DirectoryRefreshMigrationJob";

  DirectoryRefreshMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private DirectoryRefreshMigrationJob(@NonNull Parameters parameters) {
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
  public void performMigration() throws IOException {
    if (!ZonaRosaStore.account().isRegistered() ||
        !ZonaRosaStore.registration().isRegistrationComplete() ||
        ZonaRosaStore.account().getAci() == null)
    {
      Log.w(TAG, "Not registered! Skipping.");
      return;
    }

    ContactDiscovery.refreshAll(context, true);
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return e instanceof IOException;
  }

  public static class Factory implements Job.Factory<DirectoryRefreshMigrationJob> {
    @Override
    public @NonNull DirectoryRefreshMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new DirectoryRefreshMigrationJob(parameters);
    }
  }
}

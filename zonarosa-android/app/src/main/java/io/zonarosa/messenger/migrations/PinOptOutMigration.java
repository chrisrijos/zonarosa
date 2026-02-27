package io.zonarosa.messenger.migrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobs.RefreshAttributesJob;
import io.zonarosa.messenger.jobs.RefreshOwnProfileJob;
import io.zonarosa.messenger.jobs.StorageForcePushJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;

/**
 * We changed some details of what it means to opt-out of a PIN. This ensures that users who went
 * through the previous opt-out flow are now in the same state as users who went through the new
 * opt-out flow.
 */
public final class PinOptOutMigration extends MigrationJob {

  private static final String TAG = Log.tag(PinOptOutMigration.class);

  public static final String KEY = "PinOptOutMigration";

  PinOptOutMigration() {
    this(new Parameters.Builder().build());
  }

  private PinOptOutMigration(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  boolean isUiBlocking() {
    return false;
  }

  @Override
  void performMigration() {
    if (ZonaRosaStore.svr().hasOptedOut() && ZonaRosaStore.svr().hasPin()) {
      Log.w(TAG, "Discovered a legacy opt-out user! Resetting the state.");

      ZonaRosaStore.svr().optOut();
      AppDependencies.getJobManager().startChain(new RefreshAttributesJob())
                     .then(new RefreshOwnProfileJob())
                     .then(new StorageForcePushJob())
                     .enqueue();
    } else if (ZonaRosaStore.svr().hasOptedOut()) {
      Log.i(TAG, "Discovered an opt-out user, but they're already in a good state. No action required.");
    } else {
      Log.i(TAG, "Discovered a normal PIN user. No action required.");
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  public static class Factory implements Job.Factory<PinOptOutMigration> {
    @Override
    public @NonNull PinOptOutMigration create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new PinOptOutMigration(parameters);
    }
  }
}

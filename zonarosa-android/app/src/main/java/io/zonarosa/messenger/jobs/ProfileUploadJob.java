package io.zonarosa.messenger.jobs;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.util.ProfileUtil;

import java.util.concurrent.TimeUnit;

public final class ProfileUploadJob extends BaseJob {

  private static final String TAG = Log.tag(ProfileUploadJob.class);

  public static final String KEY = "ProfileUploadJob";

  public static final String QUEUE = "ProfileAlteration";

  public ProfileUploadJob() {
    this(new Job.Parameters.Builder()
                            .addConstraint(NetworkConstraint.KEY)
                            .setQueue(QUEUE)
                            .setLifespan(TimeUnit.DAYS.toMillis(30))
                            .setMaxAttempts(Parameters.UNLIMITED)
                            .setMaxInstancesForFactory(2)
                            .build());
  }

  private ProfileUploadJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  protected void onRun() throws Exception {
    if (!ZonaRosaStore.account().isRegistered()) {
      Log.w(TAG, "Not registered. Skipping.");
      return;
    }

    if (ZonaRosaStore.account().isLinkedDevice() && !ZonaRosaStore.registration().hasDownloadedProfile()) {
      Log.w(TAG, "Attempting to upload profile before downloading, forcing download first");
      AppDependencies.getJobManager()
                     .startChain(new RefreshOwnProfileJob())
                     .then(new ProfileUploadJob())
                     .enqueue();

      return;
    }

    ProfileUtil.uploadProfile(context);
    Log.i(TAG, "Profile uploaded.");
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return true;
  }

  @Override
  public @Nullable byte[] serialize() {
    return null;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onFailure() {
  }

  public static class Factory implements Job.Factory<ProfileUploadJob> {

    @Override
    public @NonNull ProfileUploadJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new ProfileUploadJob(parameters);
    }
  }
}

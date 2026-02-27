package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.DecryptionsDrainedConstraint;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Schedules a {@link ForceUpdateGroupV2WorkerJob} to happen after message queues are drained.
 */
public final class ForceUpdateGroupV2Job extends BaseJob {

  public static final String KEY = "ForceUpdateGroupV2Job";

  private static final long   FORCE_UPDATE_INTERVAL = TimeUnit.DAYS.toMillis(7);
  private static final String KEY_GROUP_ID          = "group_id";

  private final GroupId.V2 groupId;

  public static void enqueueIfNecessary(@NonNull GroupId.V2 groupId) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      Optional<GroupRecord> group = ZonaRosaDatabase.groups().getGroup(groupId);
      if (group.isPresent() &&
          group.get().isV2Group() &&
          group.get().getLastForceUpdateTimestamp() + FORCE_UPDATE_INTERVAL < System.currentTimeMillis()
      ) {
        AppDependencies.getJobManager().add(new ForceUpdateGroupV2Job(groupId));
      }
    });
  }

  private ForceUpdateGroupV2Job(@NonNull GroupId.V2 groupId) {
    this(new Parameters.Builder().setQueue("ForceUpdateGroupV2Job_" + groupId)
                                 .setMaxInstancesForQueue(1)
                                 .addConstraint(DecryptionsDrainedConstraint.KEY)
                                 .setMaxAttempts(Parameters.UNLIMITED)
                                 .build(),
         groupId);
  }

  private ForceUpdateGroupV2Job(@NonNull Parameters parameters, @NonNull GroupId.V2 groupId) {
    super(parameters);
    this.groupId = groupId;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putString(KEY_GROUP_ID, groupId.toString()).serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() {
    AppDependencies.getJobManager().add(new ForceUpdateGroupV2WorkerJob(groupId));
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return false;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<ForceUpdateGroupV2Job> {

    @Override
    public @NonNull ForceUpdateGroupV2Job create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);
      return new ForceUpdateGroupV2Job(parameters, GroupId.parseOrThrow(data.getString(KEY_GROUP_ID)).requireV2());
    }
  }
}

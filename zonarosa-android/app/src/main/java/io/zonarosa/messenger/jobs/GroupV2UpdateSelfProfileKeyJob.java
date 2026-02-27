package io.zonarosa.messenger.jobs;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedMember;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.groups.GroupChangeBusyException;
import io.zonarosa.messenger.groups.GroupChangeFailedException;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.groups.GroupInsufficientRightsException;
import io.zonarosa.messenger.groups.GroupManager;
import io.zonarosa.messenger.groups.GroupNotAMemberException;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.DecryptionsDrainedConstraint;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.service.api.groupsv2.NoCredentialForRedemptionTimeException;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import okio.ByteString;

/**
 * When your profile key changes, this job can be used to update it on a single given group.
 * <p>
 * Your membership is confirmed first, so safe to run against any known {@link GroupId.V2}
 */
public final class GroupV2UpdateSelfProfileKeyJob extends BaseJob {

  public static final String KEY = "GroupV2UpdateSelfProfileKeyJob";

  private static final String QUEUE = "GroupV2UpdateSelfProfileKeyJob";

  @SuppressWarnings("unused")
  private static final String TAG = Log.tag(GroupV2UpdateSelfProfileKeyJob.class);

  private static final String KEY_GROUP_ID = "group_id";

  private final GroupId.V2 groupId;

  /**
   * Job will run regardless of how many times you enqueue it.
   */
  public static @NonNull GroupV2UpdateSelfProfileKeyJob withoutLimits(@NonNull GroupId.V2 groupId) {
    return new GroupV2UpdateSelfProfileKeyJob(new Parameters.Builder()
                                                            .addConstraint(NetworkConstraint.KEY)
                                                            .setLifespan(TimeUnit.DAYS.toMillis(1))
                                                            .setMaxAttempts(Parameters.UNLIMITED)
                                                            .setQueue(QUEUE)
                                                            .build(),
                                              groupId);
  }

  /**
   * Only one instance will be enqueued per group, and it won't run until after decryptions are
   * drained.
   */
  public static @NonNull GroupV2UpdateSelfProfileKeyJob withQueueLimits(@NonNull GroupId.V2 groupId) {
    return new GroupV2UpdateSelfProfileKeyJob(new Parameters.Builder()
                                                            .addConstraint(NetworkConstraint.KEY)
                                                            .addConstraint(DecryptionsDrainedConstraint.KEY)
                                                            .setLifespan(TimeUnit.DAYS.toMillis(1))
                                                            .setMaxAttempts(Parameters.UNLIMITED)
                                                            .setQueue(QUEUE + "_" + groupId.toString())
                                                            .setMaxInstancesForQueue(1)
                                                            .build(),
                                              groupId);
  }

  /**
   * Updates GV2 groups with the correct profile key if we find any that are out of date. Will run at most once per day.
   */
  @AnyThread
  public static void enqueueForGroupsIfNecessary() {
    if (!ZonaRosaStore.account().isRegistered() || ZonaRosaStore.account().getAci() == null || !Recipient.self().isRegistered()) {
      Log.w(TAG, "Not yet registered!");
      return;
    }

    if (ZonaRosaStore.account().isLinkedDevice()) {
      Log.i(TAG, "Linked device, skipping");
      return;
    }

    byte[] rawProfileKey = Recipient.self().getProfileKey();

    if (rawProfileKey == null) {
      Log.w(TAG, "No profile key set!");
      return;
    }

    ByteString selfProfileKey = ByteString.of(rawProfileKey);

    long timeSinceLastCheck = System.currentTimeMillis() - ZonaRosaStore.misc().getLastGv2ProfileCheckTime();

    if (timeSinceLastCheck < TimeUnit.DAYS.toMillis(1)) {
      Log.d(TAG, "Too soon. Last check was " + timeSinceLastCheck + " ms ago.");
      return;
    }

    Log.i(TAG, "Running routine check.");

    ZonaRosaStore.misc().setLastGv2ProfileCheckTime(System.currentTimeMillis());

    ZonaRosaExecutors.BOUNDED.execute(() -> {
      boolean foundMismatch = false;

      for (GroupId.V2 id : ZonaRosaDatabase.groups().getAllGroupV2Ids()) {
        Optional<GroupRecord> group = ZonaRosaDatabase.groups().getGroup(id);
        if (!group.isPresent()) {
          Log.w(TAG, "Group " + group + " no longer exists?");
          continue;
        }

        ByteString      selfUuidBytes = Recipient.self().requireAci().toByteString();
        boolean         isActive      = group.get().isActive();
        DecryptedMember selfMember    = group.get().requireV2GroupProperties().getDecryptedGroup().members
                                                                                                  .stream()
                                                                                                  .filter(m -> m.aciBytes.equals(selfUuidBytes))
                                                                                                  .findFirst()
                                                                                                  .orElse(null);

        if (isActive && selfMember != null && !selfMember.profileKey.equals(selfProfileKey)) {
          Log.w(TAG, "Profile key mismatch for group " + id + " -- enqueueing job");
          foundMismatch = true;
          AppDependencies.getJobManager().add(GroupV2UpdateSelfProfileKeyJob.withQueueLimits(id));
        }
      }

      if (!foundMismatch) {
        Log.i(TAG, "No mismatches found.");
      }
    });
  }


  private GroupV2UpdateSelfProfileKeyJob(@NonNull Parameters parameters, @NonNull GroupId.V2 groupId) {
    super(parameters);
    this.groupId = groupId;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putString(KEY_GROUP_ID, groupId.toString())
                                    .serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun()
      throws IOException, GroupNotAMemberException, GroupChangeFailedException, GroupInsufficientRightsException, GroupChangeBusyException
  {
    if (ZonaRosaStore.account().isLinkedDevice()) {
      Log.i(TAG, "Linked device, skipping");
      return;
    }

    Log.i(TAG, "Ensuring profile key up to date on group " + groupId);
    GroupManager.updateSelfProfileKeyInGroup(context, groupId);
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException ||
           e instanceof NoCredentialForRedemptionTimeException||
           e instanceof GroupChangeBusyException;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<GroupV2UpdateSelfProfileKeyJob> {

    @Override
    public @NonNull GroupV2UpdateSelfProfileKeyJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      return new GroupV2UpdateSelfProfileKeyJob(parameters,
                                                GroupId.parseOrThrow(data.getString(KEY_GROUP_ID)).requireV2());
    }
  }
}

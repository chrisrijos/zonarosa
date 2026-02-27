package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobs.protos.GroupCallPeekJobData;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.service.webrtc.WebRtcUtil;

import java.io.IOException;

/**
 * Runs in the same queue as messages for the group.
 */
final class GroupCallPeekWorkerJob extends BaseJob {

  public static final String KEY = "GroupCallPeekWorkerJob";

  private static final String KEY_GROUP_CALL_JOB_DATA = "group_call_job_data";

  @NonNull private final GroupCallPeekJobData groupCallPeekJobData;

  public GroupCallPeekWorkerJob(@NonNull GroupCallPeekJobData groupCallPeekJobData) {
    this(new Parameters.Builder()
                       .setQueue(PushProcessMessageJob.getQueueName(RecipientId.from(groupCallPeekJobData.groupRecipientId)))
                       .setMaxInstancesForQueue(2)
                       .build(),
         groupCallPeekJobData);
  }

  private GroupCallPeekWorkerJob(@NonNull Parameters parameters, @NonNull GroupCallPeekJobData groupCallPeekJobData) {
    super(parameters);
    this.groupCallPeekJobData = groupCallPeekJobData;
  }

  @Override
  protected void onRun() {
    RecipientId groupRecipientId = RecipientId.from(groupCallPeekJobData.groupRecipientId);

    AppDependencies.getZonaRosaCallManager().peekGroupCall(groupRecipientId, (peekInfo) -> {
      if (groupCallPeekJobData.senderRecipientId == RecipientId.UNKNOWN.toLong()) {
        return;
      }

      RecipientId senderRecipientId = RecipientId.from(groupCallPeekJobData.senderRecipientId);
      ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromLocalEvent(
          groupRecipientId,
          senderRecipientId,
          groupCallPeekJobData.serverTimestamp,
          peekInfo.getEraId(),
          peekInfo.getJoinedMembers(),
          WebRtcUtil.isCallFull(peekInfo)
      );
    });
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return false;
  }

  @Override
  public @NonNull byte[] serialize() {
    return groupCallPeekJobData.encode();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<GroupCallPeekWorkerJob> {

    @Override
    public @NonNull GroupCallPeekWorkerJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      try {
        GroupCallPeekJobData jobData = GroupCallPeekJobData.ADAPTER.decode(serializedData);
        return new GroupCallPeekWorkerJob(parameters, jobData);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}

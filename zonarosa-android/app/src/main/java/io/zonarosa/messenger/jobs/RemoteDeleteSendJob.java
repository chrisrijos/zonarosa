package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import io.zonarosa.core.util.SetUtil;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.DistributionListId;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.database.model.MmsMessageRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.messages.GroupSendUtil;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.messenger.util.GroupUtil;
import io.zonarosa.core.util.Util;
import io.zonarosa.service.api.crypto.ContentHint;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RemoteDeleteSendJob extends BaseJob {

  public static final String KEY = "RemoteDeleteSendJob";

  private static final String TAG = Log.tag(RemoteDeleteSendJob.class);

  private static final String KEY_MESSAGE_ID              = "message_id";
  private static final String KEY_RECIPIENTS              = "recipients";
  private static final String KEY_INITIAL_RECIPIENT_COUNT = "initial_recipient_count";

  private final long              messageId;
  private final List<RecipientId> recipients;
  private final int               initialRecipientCount;


  @WorkerThread
  public static @NonNull JobManager.Chain create(long messageId)
      throws NoSuchMessageException
  {
    MessageRecord message = ZonaRosaDatabase.messages().getMessageRecord(messageId);

    Recipient conversationRecipient = ZonaRosaDatabase.threads().getRecipientForThreadId(message.getThreadId());

    if (conversationRecipient == null) {
      throw new AssertionError("We have a message, but couldn't find the thread!");
    }

    List<RecipientId> recipients;
    if (conversationRecipient.isDistributionList()) {
      recipients = ZonaRosaDatabase.storySends().getRemoteDeleteRecipients(message.getId(), message.getTimestamp());
      if (recipients.isEmpty()) {
        return AppDependencies.getJobManager().startChain(MultiDeviceStorySendSyncJob.create(message.getDateSent(), messageId));
      }
    } else {
      recipients = conversationRecipient.isGroup() ? Stream.of(conversationRecipient.getParticipantIds()).toList()
                                                   : Stream.of(conversationRecipient.getId()).toList();
    }

    recipients.remove(Recipient.self().getId());

    RemoteDeleteSendJob sendJob = new RemoteDeleteSendJob(messageId,
                                                          recipients,
                                                          recipients.size(),
                                                          new Parameters.Builder()
                                                                        .setQueue(conversationRecipient.getId().toQueueKey())
                                                                        .setLifespan(TimeUnit.DAYS.toMillis(1))
                                                                        .setMaxAttempts(Parameters.UNLIMITED)
                                                                        .build());

    if (conversationRecipient.isDistributionList()) {
      return AppDependencies.getJobManager()
                            .startChain(sendJob)
                            .then(MultiDeviceStorySendSyncJob.create(message.getDateSent(), messageId));
    } else {
      return AppDependencies.getJobManager().startChain(sendJob);
    }
  }

  private RemoteDeleteSendJob(long messageId,
                              @NonNull List<RecipientId> recipients,
                              int initialRecipientCount,
                              @NonNull Parameters parameters)
  {
    super(parameters);

    this.messageId             = messageId;
    this.recipients            = recipients;
    this.initialRecipientCount = initialRecipientCount;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putLong(KEY_MESSAGE_ID, messageId)
                                    .putString(KEY_RECIPIENTS, RecipientId.toSerializedList(recipients))
                                    .putInt(KEY_INITIAL_RECIPIENT_COUNT, initialRecipientCount)
                                    .serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    MessageTable  db      = ZonaRosaDatabase.messages();
    MessageRecord message = ZonaRosaDatabase.messages().getMessageRecord(messageId);

    long      targetSentTimestamp   = message.getDateSent();
    Recipient conversationRecipient = ZonaRosaDatabase.threads().getRecipientForThreadId(message.getThreadId());

    if (conversationRecipient == null) {
      throw new AssertionError("We have a message, but couldn't find the thread!");
    }

    if (!message.isOutgoing()) {
      throw new IllegalStateException("Cannot delete a message that isn't yours!");
    }

    if (!conversationRecipient.isRegistered() || conversationRecipient.isMmsGroup()) {
      Log.w(TAG, "Unable to remote delete non-push messages");
      return;
    }

    if (conversationRecipient.isPushV1Group()) {
      Log.w(TAG, "Unable to remote delete messages in GV1 groups");
      return;
    }

    List<Recipient>   possible = Stream.of(recipients).map(Recipient::resolved).toList();
    List<Recipient>   eligible = RecipientUtil.getEligibleForSending(Stream.of(recipients).map(Recipient::resolved).filter(Recipient::getHasServiceId).toList());
    List<RecipientId> skipped  = Stream.of(SetUtil.difference(possible, eligible)).map(Recipient::getId).toList();

    boolean            isForStory         = message.isMms() && (((MmsMessageRecord) message).getStoryType().isStory() || ((MmsMessageRecord) message).getParentStoryId() != null);
    DistributionListId distributionListId = isForStory ? message.getToRecipient().getDistributionListId().orElse(null) : null;

    GroupSendJobHelper.SendResult sendResult = deliver(conversationRecipient, eligible, targetSentTimestamp, isForStory, distributionListId);

    for (Recipient completion : sendResult.completed) {
      recipients.remove(completion.getId());
    }

    for (RecipientId unregistered : sendResult.unregistered) {
      ZonaRosaDatabase.recipients().markUnregistered(unregistered);
    }

    for (RecipientId skip : skipped) {
      recipients.remove(skip);
    }

    List<RecipientId> totalSkips = Util.join(skipped, sendResult.skipped);

    Log.i(TAG, "Completed now: " + sendResult.completed.size() + ", Skipped: " + totalSkips.size() + ", Remaining: " + recipients.size());

    if (totalSkips.size() > 0 && message.getToRecipient().isGroup()) {
      ZonaRosaDatabase.groupReceipts().setSkipped(totalSkips, messageId);
    }

    if (recipients.isEmpty()) {
      db.markAsSent(messageId, true);
    } else {
      Log.w(TAG, "Still need to send to " + recipients.size() + " recipients. Retrying.");
      throw new RetryLaterException();
    }
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    if (e instanceof NotPushRegisteredException) return false;
    return e instanceof IOException ||
           e instanceof RetryLaterException;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to send remote delete to all recipients! (" + (initialRecipientCount - recipients.size() + "/" + initialRecipientCount + ")") );
  }

  private @NonNull GroupSendJobHelper.SendResult deliver(@NonNull Recipient conversationRecipient,
                                                         @NonNull List<Recipient> destinations,
                                                         long targetSentTimestamp,
                                                         boolean isForStory,
                                                         @Nullable DistributionListId distributionListId)
      throws IOException, UntrustedIdentityException
  {
    ZonaRosaServiceDataMessage.Builder dataMessageBuilder = ZonaRosaServiceDataMessage.newBuilder()
                                                                                  .withTimestamp(System.currentTimeMillis())
                                                                                  .withRemoteDelete(new ZonaRosaServiceDataMessage.RemoteDelete(targetSentTimestamp));

    if (conversationRecipient.isGroup()) {
      GroupUtil.setDataMessageGroupContext(context, dataMessageBuilder, conversationRecipient.requireGroupId().requirePush());
    }

    ZonaRosaServiceDataMessage dataMessage = dataMessageBuilder.build();
    List<SendMessageResult>  results     = GroupSendUtil.sendResendableDataMessage(context,
                                                                                   conversationRecipient.getGroupId().map(GroupId::requireV2).orElse(null),
                                                                                   distributionListId,
                                                                                   destinations,
                                                                                   false,
                                                                                   ContentHint.RESENDABLE,
                                                                                   new MessageId(messageId),
                                                                                   dataMessage,
                                                                                   true,
                                                                                   isForStory,
                                                                                   null,
                                                                                   null);

    if (conversationRecipient.isSelf()) {
      AppDependencies.getZonaRosaServiceMessageSender().sendSyncMessage(dataMessage);
    }

    return GroupSendJobHelper.getCompletedSends(destinations, results);
  }

  public static class Factory implements Job.Factory<RemoteDeleteSendJob> {

    @Override
    public @NonNull RemoteDeleteSendJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      long              messageId             = data.getLong(KEY_MESSAGE_ID);
      List<RecipientId> recipients            = RecipientId.fromSerializedList(data.getString(KEY_RECIPIENTS));
      int               initialRecipientCount = data.getInt(KEY_INITIAL_RECIPIENT_COUNT);

      return new RemoteDeleteSendJob(messageId,  recipients, initialRecipientCount, parameters);
    }
  }
}
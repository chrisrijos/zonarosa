package io.zonarosa.messenger.jobs;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import io.zonarosa.core.util.ListUtil;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.crypto.SealedSenderAccessUtil;
import io.zonarosa.messenger.database.MessageTable.MarkedMessageInfo;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.transport.UndeliverableMessageException;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.core.util.Util;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.crypto.ContentHint;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceReceiptMessage;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.PushNetworkException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class SendReadReceiptJob extends BaseJob {

  public static final String KEY = "SendReadReceiptJob";

  private static final String TAG = Log.tag(SendReadReceiptJob.class);

  static final int MAX_TIMESTAMPS = 500;

  private static final String KEY_THREAD                  = "thread";
  private static final String KEY_ADDRESS                 = "address";
  private static final String KEY_RECIPIENT               = "recipient";
  private static final String KEY_MESSAGE_SENT_TIMESTAMPS = "message_ids";
  private static final String KEY_MESSAGE_IDS             = "message_db_ids";
  private static final String KEY_TIMESTAMP               = "timestamp";

  private final long            threadId;
  private final RecipientId     recipientId;
  private final List<Long>      messageSentTimestamps;
  private final long            timestamp;
  private final List<MessageId> messageIds;

  @VisibleForTesting
  public SendReadReceiptJob(long threadId, @NonNull RecipientId recipientId, List<Long> messageSentTimestamps, List<MessageId> messageIds) {
    this(new Job.Parameters.Builder()
                           .addConstraint(NetworkConstraint.KEY)
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .setQueue(recipientId.toQueueKey())
                           .build(),
         threadId,
         recipientId,
         ensureSize(messageSentTimestamps, MAX_TIMESTAMPS),
         ensureSize(messageIds, MAX_TIMESTAMPS),
         System.currentTimeMillis());
  }

  private SendReadReceiptJob(@NonNull Job.Parameters parameters,
                             long threadId,
                             @NonNull RecipientId recipientId,
                             @NonNull List<Long> messageSentTimestamps,
                             @NonNull List<MessageId> messageIds,
                             long timestamp)
  {
    super(parameters);

    this.threadId              = threadId;
    this.recipientId           = recipientId;
    this.messageSentTimestamps = messageSentTimestamps;
    this.messageIds            = messageIds;
    this.timestamp             = timestamp;
  }

  /**
   * Enqueues all the necessary jobs for read receipts, ensuring that they're all within the
   * maximum size.
   */
  public static void enqueue(long threadId, @NonNull RecipientId recipientId, List<MarkedMessageInfo> markedMessageInfos) {
    if (!ZonaRosaPreferences.isReadReceiptsEnabled(AppDependencies.getApplication())) {
      return;
    }

    if (recipientId.equals(Recipient.self().getId())) {
      return;
    }

    JobManager                    jobManager      = AppDependencies.getJobManager();
    List<List<MarkedMessageInfo>> messageIdChunks = ListUtil.chunk(markedMessageInfos, MAX_TIMESTAMPS);

    if (messageIdChunks.size() > 1) {
      Log.w(TAG, "Large receipt count! Had to break into multiple chunks. Total count: " + markedMessageInfos.size());
    }

    for (List<MarkedMessageInfo> chunk : messageIdChunks) {
      List<Long>      sentTimestamps = chunk.stream().map(info -> info.getSyncMessageId().getTimetamp()).collect(Collectors.toList());
      List<MessageId> messageIds     = chunk.stream().map(MarkedMessageInfo::getMessageId).collect(Collectors.toList());

      jobManager.add(new SendReadReceiptJob(threadId, recipientId, sentTimestamps, messageIds));
    }
  }

  @Override
  public @Nullable byte[] serialize() {
    long[] sentTimestamps = new long[messageSentTimestamps.size()];
    for (int i = 0; i < sentTimestamps.length; i++) {
      sentTimestamps[i] = messageSentTimestamps.get(i);
    }

    List<String> serializedMessageIds = messageIds.stream().map(MessageId::serialize).collect(Collectors.toList());

    return new JsonJobData.Builder().putString(KEY_RECIPIENT, recipientId.serialize())
                                    .putLongArray(KEY_MESSAGE_SENT_TIMESTAMPS, sentTimestamps)
                                    .putStringListAsArray(KEY_MESSAGE_IDS, serializedMessageIds)
                                    .putLong(KEY_TIMESTAMP, timestamp)
                                    .putLong(KEY_THREAD, threadId)
                                    .serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws IOException, UntrustedIdentityException, UndeliverableMessageException {
    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    if (!ZonaRosaPreferences.isReadReceiptsEnabled(context) || messageSentTimestamps.isEmpty()) return;

    if (!RecipientUtil.isMessageRequestAccepted(context, threadId)) {
      Log.w(TAG, "Refusing to send receipts to untrusted recipient");
      return;
    }

    Recipient recipient = Recipient.resolved(recipientId);

    if (recipient.isSelf()) {
      Log.i(TAG, "Not sending to self, aborting.");
    }

    if (recipient.isBlocked()) {
      Log.w(TAG, "Refusing to send receipts to blocked recipient");
      return;
    }

    if (recipient.isGroup()) {
      Log.w(TAG, "Refusing to send receipts to group");
      return;
    }

    if (recipient.isDistributionList()) {
      Log.w(TAG, "Refusing to send receipts to distribution list");
      return;
    }

    if (recipient.isUnregistered()) {
      Log.w(TAG, recipient.getId() + " not registered!");
      return;
    }

    if (!recipient.getHasServiceId() && !recipient.getHasE164()) {
      Log.w(TAG, "No serviceId or e164!");
      return;
    }

    ZonaRosaServiceMessageSender  messageSender  = AppDependencies.getZonaRosaServiceMessageSender();
    ZonaRosaServiceAddress        remoteAddress  = RecipientUtil.toZonaRosaServiceAddress(context, recipient);
    ZonaRosaServiceReceiptMessage receiptMessage = new ZonaRosaServiceReceiptMessage(ZonaRosaServiceReceiptMessage.Type.READ, messageSentTimestamps, timestamp);

    SendMessageResult result = messageSender.sendReceipt(remoteAddress,
                                                         SealedSenderAccessUtil.getSealedSenderAccessFor(recipient,
                                                                                                         () -> ZonaRosaDatabase.groups().getGroupSendFullToken(threadId, recipientId)),
                                                         receiptMessage,
                                                         recipient.getNeedsPniSignature());

    if (Util.hasItems(messageIds)) {
      ZonaRosaDatabase.messageLog().insertIfPossible(recipientId, timestamp, result, ContentHint.IMPLICIT, messageIds, false);
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    if (e instanceof ServerRejectedException) return false;
    if (e instanceof PushNetworkException) return true;
    return false;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to send read receipts to: " + recipientId);
  }

  static <E> List<E> ensureSize(@NonNull List<E> list, int maxSize) {
    if (list.size() > maxSize) {
      throw new IllegalArgumentException("Too large! Size: " + list.size() + ", maxSize: " + maxSize);
    }
    return list;
  }

  public static final class Factory implements Job.Factory<SendReadReceiptJob> {

    @Override
    public @NonNull SendReadReceiptJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      long            timestamp      = data.getLong(KEY_TIMESTAMP);
      long[]          ids            = data.hasLongArray(KEY_MESSAGE_SENT_TIMESTAMPS) ? data.getLongArray(KEY_MESSAGE_SENT_TIMESTAMPS) : new long[0];
      List<Long>      sentTimestamps = new ArrayList<>(ids.length);
      List<String>    rawMessageIds  = data.hasStringArray(KEY_MESSAGE_IDS) ? data.getStringArrayAsList(KEY_MESSAGE_IDS) : Collections.emptyList();
      List<MessageId> messageIds     = rawMessageIds.stream().map(MessageId::deserialize).collect(Collectors.toList());
      long            threadId       = data.getLong(KEY_THREAD);
      RecipientId     recipientId    = RecipientId.from(data.getString(KEY_RECIPIENT));

      for (long id : ids) {
        sentTimestamps.add(id);
      }

      return new SendReadReceiptJob(parameters, threadId, recipientId, sentTimestamps, messageIds, timestamp);
    }
  }
}

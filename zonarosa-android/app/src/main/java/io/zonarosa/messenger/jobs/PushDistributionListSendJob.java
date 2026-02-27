package io.zonarosa.messenger.jobs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.attachments.Attachment;
import io.zonarosa.messenger.attachments.DatabaseAttachment;
import io.zonarosa.messenger.database.GroupReceiptTable;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.SentStorySyncManifest;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.documents.IdentityKeyMismatch;
import io.zonarosa.messenger.database.documents.NetworkFailure;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobLogger;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.messages.GroupSendUtil;
import io.zonarosa.messenger.messages.StorySendUtil;
import io.zonarosa.messenger.mms.MmsException;
import io.zonarosa.messenger.mms.OutgoingMessage;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.stories.Stories;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.messenger.transport.UndeliverableMessageException;
import io.zonarosa.core.util.Util;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessageRecipient;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;
import io.zonarosa.service.internal.push.BodyRange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A job that lets us send a message to a distribution list. Currently the only supported message type is a story.
 */
public final class PushDistributionListSendJob extends PushSendJob {

  public static final String KEY = "PushDistributionListSendJob";

  private static final String TAG = Log.tag(PushDistributionListSendJob.class);

  private static final String KEY_MESSAGE_ID             = "message_id";
  private static final String KEY_FILTERED_RECIPIENT_IDS = "filtered_recipient_ids";

  private final long             messageId;
  private final Set<RecipientId> filterRecipientIds;

  public PushDistributionListSendJob(long messageId, @NonNull RecipientId destination, boolean hasMedia, @NonNull Set<RecipientId> filterRecipientIds) {
    this(new Parameters.Builder()
             .setQueue(destination.toQueueKey(hasMedia))
             .addConstraint(NetworkConstraint.KEY)
             .setLifespan(TimeUnit.DAYS.toMillis(1))
             .setMaxAttempts(Parameters.UNLIMITED)
             .build(),
         messageId,
         filterRecipientIds
    );
  }

  private PushDistributionListSendJob(@NonNull Parameters parameters, long messageId, @NonNull Set<RecipientId> filterRecipientIds) {
    super(parameters);
    this.messageId          = messageId;
    this.filterRecipientIds = filterRecipientIds;
  }

  @WorkerThread
  public static void enqueue(@NonNull Context context,
                             @NonNull JobManager jobManager,
                             long messageId,
                             @NonNull RecipientId destination,
                             @NonNull Set<RecipientId> filterRecipientIds)
  {
    try {
      Recipient listRecipient = Recipient.resolved(destination);

      if (!listRecipient.isDistributionList()) {
        throw new AssertionError("Not a distribution list! MessageId: " + messageId);
      }

      OutgoingMessage message = ZonaRosaDatabase.messages().getOutgoingMessage(messageId);

      if (!message.getStoryType().isStory()) {
        throw new AssertionError("Only story messages are currently supported! MessageId: " + messageId);
      }

      if (!message.getStoryType().isTextStory()) {
        if (message.getAttachments().isEmpty()) {
          Log.w(TAG, "No attachments found for message " + messageId + ". Ignoring.");
          return;
        }
        
        DatabaseAttachment storyAttachment = (DatabaseAttachment) message.getAttachments().get(0);
        ZonaRosaDatabase.attachments().updateAttachmentCaption(storyAttachment.attachmentId, message.getBody());
      }

      Set<String> attachmentUploadIds = enqueueCompressingAndUploadAttachmentsChains(jobManager, message);

      jobManager.add(new PushDistributionListSendJob(messageId, destination, !attachmentUploadIds.isEmpty(), filterRecipientIds), attachmentUploadIds, attachmentUploadIds.isEmpty() ? null : destination.toQueueKey());
    } catch (NoSuchMessageException | MmsException e) {
      Log.w(TAG, "Failed to enqueue message.", e);
      ZonaRosaDatabase.messages().markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    }
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putLong(KEY_MESSAGE_ID, messageId)
                                    .putString(KEY_FILTERED_RECIPIENT_IDS, RecipientId.toSerializedList(filterRecipientIds))
                                    .serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onAdded() {
    ZonaRosaDatabase.messages().markAsSending(messageId);
  }

  @Override
  public void onPushSend()
      throws IOException, MmsException, NoSuchMessageException, RetryLaterException
  {
    MessageTable             database                   = ZonaRosaDatabase.messages();
    OutgoingMessage          message                    = database.getOutgoingMessage(messageId);
    Set<NetworkFailure>      existingNetworkFailures    = new HashSet<>(message.getNetworkFailures());
    Set<IdentityKeyMismatch> existingIdentityMismatches = new HashSet<>(message.getIdentityKeyMismatches());

    if (!message.getStoryType().isStory()) {
      throw new MmsException("Only story sends are currently supported!");
    }

    if (database.isSent(messageId)) {
      log(TAG, String.valueOf(message.getSentTimeMillis()), "Message " + messageId + " was already sent. Ignoring.");
      return;
    }

    Recipient listRecipient = message.getThreadRecipient().resolve();

    if (!listRecipient.isDistributionList()) {
      throw new MmsException("Message recipient isn't a distribution list!");
    }

    try {
      log(TAG, String.valueOf(message.getSentTimeMillis()), "Sending message: " + messageId + ", Recipient: " + message.getThreadRecipient().getId() + ", Attachments: " + buildAttachmentString(message.getAttachments()));

      List<Recipient> targets;
      List<RecipientId> skipped = Collections.emptyList();

      if (Util.hasItems(filterRecipientIds)) {
        targets = new ArrayList<>(filterRecipientIds.size() + existingNetworkFailures.size());
        targets.addAll(filterRecipientIds.stream().map(Recipient::resolved).collect(Collectors.toList()));
        targets.addAll(existingNetworkFailures.stream().map(NetworkFailure::getRecipientId).distinct().map(Recipient::resolved).collect(Collectors.toList()));
      } else if (!existingNetworkFailures.isEmpty()) {
        targets = Stream.of(existingNetworkFailures).map(NetworkFailure::getRecipientId).distinct().map(Recipient::resolved).toList();
      } else {
        Stories.SendData data = Stories.getRecipientsToSendTo(messageId, message.getSentTimeMillis(), message.getStoryType().isStoryWithReplies());
        targets = data.getTargets();
        skipped = data.getSkipped();
      }

      List<SendMessageResult> results = deliver(message, targets);
      Log.i(TAG, JobLogger.format(this, "Finished send."));

      PushGroupSendJob.processGroupMessageResults(context, messageId, -1, null, message, results, targets, skipped, existingNetworkFailures, existingIdentityMismatches);

    } catch (UntrustedIdentityException | UndeliverableMessageException e) {
      warn(TAG, String.valueOf(message.getSentTimeMillis()), e);
      database.markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    }
  }

  @Override
  public void onFailure() {
    ZonaRosaDatabase.messages().markAsSentFailed(messageId);
  }

  private List<SendMessageResult> deliver(@NonNull OutgoingMessage message, @NonNull List<Recipient> destinations)
      throws IOException, UntrustedIdentityException, UndeliverableMessageException
  {
    try {
      rotateSenderCertificateIfNecessary();

      List<Attachment>                    attachments        = Stream.of(message.getAttachments()).filterNot(Attachment::isSticker).toList();
      List<ZonaRosaServiceAttachment> attachmentPointers = getAttachmentPointersFor(attachments);
      List<BodyRange>               bodyRanges         = getBodyRanges(message);
      boolean                             isRecipientUpdate  = Stream.of(ZonaRosaDatabase.groupReceipts().getGroupReceiptInfo(messageId))
                                                                     .anyMatch(info -> info.getStatus() > GroupReceiptTable.STATUS_UNDELIVERED);

      final ZonaRosaServiceStoryMessage storyMessage;
      if (message.getStoryType().isTextStory()) {
        storyMessage = ZonaRosaServiceStoryMessage.forTextAttachment(Recipient.self().getProfileKey(), null, StorySendUtil.deserializeBodyToStoryTextAttachment(message, this::getPreviewsFor), message.getStoryType().isStoryWithReplies(), bodyRanges);
      } else if (!attachmentPointers.isEmpty()) {
        storyMessage = ZonaRosaServiceStoryMessage.forFileAttachment(Recipient.self().getProfileKey(), null, attachmentPointers.get(0), message.getStoryType().isStoryWithReplies(), bodyRanges);
      } else {
        throw new UndeliverableMessageException("No attachment on non-text story.");
      }

      SentStorySyncManifest                   manifest           = ZonaRosaDatabase.storySends().getFullSentStorySyncManifest(messageId, message.getSentTimeMillis());
      Set<ZonaRosaServiceStoryMessageRecipient> manifestCollection = manifest != null ? manifest.toRecipientsSet() : Collections.emptySet();

      Log.d(TAG, "[" + messageId + "] Sending a story message with a manifest of size " + manifestCollection.size());

      return GroupSendUtil.sendStoryMessage(context, message.getThreadRecipient().requireDistributionListId(), destinations, isRecipientUpdate, new MessageId(messageId), message.getSentTimeMillis(), storyMessage, manifestCollection);
    } catch (ServerRejectedException e) {
      throw new UndeliverableMessageException(e);
    }
  }

  public static class Factory implements Job.Factory<PushDistributionListSendJob> {
    @Override
    public @NonNull PushDistributionListSendJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);

      Set<RecipientId> recipientIds = new HashSet<>(RecipientId.fromSerializedList(data.getStringOrDefault(KEY_FILTERED_RECIPIENT_IDS, "")));
      return new PushDistributionListSendJob(parameters, data.getLong(KEY_MESSAGE_ID), recipientIds);
    }
  }
}

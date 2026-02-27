/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zonarosa.messenger.sms;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import org.greenrobot.eventbus.EventBus;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.attachments.Attachment;
import io.zonarosa.messenger.attachments.AttachmentId;
import io.zonarosa.messenger.attachments.DatabaseAttachment;
import io.zonarosa.messenger.backup.v2.BackupRepository;
import io.zonarosa.messenger.contacts.sync.ContactDiscovery;
import io.zonarosa.messenger.database.AttachmentTable;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.MessageTable.InsertResult;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.RecipientTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.ThreadTable;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.database.model.MmsMessageRecord;
import io.zonarosa.messenger.database.model.ReactionRecord;
import io.zonarosa.messenger.database.model.StoryType;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobs.AdminDeleteSendJob;
import io.zonarosa.messenger.jobs.AttachmentCompressionJob;
import io.zonarosa.messenger.jobs.AttachmentCopyJob;
import io.zonarosa.messenger.jobs.AttachmentUploadJob;
import io.zonarosa.messenger.jobs.CopyAttachmentToArchiveJob;
import io.zonarosa.messenger.jobs.IndividualSendJob;
import io.zonarosa.messenger.jobs.ProfileKeySendJob;
import io.zonarosa.messenger.jobs.PushDistributionListSendJob;
import io.zonarosa.messenger.jobs.PushGroupSendJob;
import io.zonarosa.messenger.jobs.ReactionSendJob;
import io.zonarosa.messenger.jobs.RemoteDeleteSendJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.core.models.media.Media;
import io.zonarosa.messenger.mms.MmsException;
import io.zonarosa.messenger.mms.OutgoingMessage;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.util.ParcelUtil;
import io.zonarosa.messenger.util.ZonaRosaLocalMetrics;
import io.zonarosa.service.api.push.DistributionId;
import io.zonarosa.service.api.util.Preconditions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MessageSender {

  private static final String TAG = Log.tag(MessageSender.class);

  /**
   * Suitable for a 1:1 conversation or a GV1 group only.
   */
  @WorkerThread
  public static void sendProfileKey(final long threadId) {
    ProfileKeySendJob job = ProfileKeySendJob.create(threadId, false);
    if (job != null) {
      AppDependencies.getJobManager().add(job);
    }
  }

  public static void sendStories(@NonNull final Context context,
                                 @NonNull final List<OutgoingMessage> messages,
                                 @Nullable final String metricId,
                                 @Nullable final MessageTable.InsertListener insertListener)
  {
    Log.i(TAG, "Sending story messages to " + messages.size() + " targets.");
    ThreadTable  threadTable = ZonaRosaDatabase.threads();
    MessageTable database    = ZonaRosaDatabase.messages();
    List<Long>   messageIds  = new ArrayList<>(messages.size());
    List<Long>   threads     = new ArrayList<>(messages.size());
    UploadDependencyGraph dependencyGraph;

    try {
      database.beginTransaction();

      for (OutgoingMessage message : messages) {
        long allocatedThreadId = threadTable.getOrCreateValidThreadId(message.getThreadRecipient(), -1L, message.getDistributionType());
        long messageId         = database.insertMessageOutbox(message.stripAttachments(), allocatedThreadId, false, insertListener).getMessageId();

        messageIds.add(messageId);
        threads.add(allocatedThreadId);

        if (message.getThreadRecipient().isGroup() && message.getAttachments().isEmpty() && message.getLinkPreviews().isEmpty() && message.getSharedContacts().isEmpty()) {
          ZonaRosaLocalMetrics.GroupMessageSend.onInsertedIntoDatabase(messageId, metricId);
        } else {
          ZonaRosaLocalMetrics.GroupMessageSend.cancel(metricId);
        }
      }

      for (int i = 0; i < messageIds.size(); i++) {
        long            messageId = messageIds.get(i);
        OutgoingMessage message   = messages.get(i);
        Recipient       recipient = message.getThreadRecipient();

        if (recipient.isDistributionList()) {
          DistributionId    distributionId = Objects.requireNonNull(ZonaRosaDatabase.distributionLists().getDistributionId(recipient.requireDistributionListId()));
          List<RecipientId> members        = ZonaRosaDatabase.distributionLists().getMembers(recipient.requireDistributionListId());
          ZonaRosaDatabase.storySends().insert(messageId, members, message.getSentTimeMillis(), message.getStoryType().isStoryWithReplies(), distributionId);
        }
      }

      dependencyGraph = UploadDependencyGraph.create(
          messages,
          AppDependencies.getJobManager(),
          attachment -> {
            try {
              return ZonaRosaDatabase.attachments().insertAttachmentForPreUpload(attachment);
            } catch (MmsException e) {
              Log.e(TAG, e);
              throw new IllegalStateException(e);
            }
          }
      );

      for (int i = 0; i < messageIds.size(); i++) {
        long                             messageId = messageIds.get(i);
        OutgoingMessage                  message   = messages.get(i);
        List<UploadDependencyGraph.Node> nodes     = dependencyGraph.getDependencyMap().get(message);

        if (nodes == null || nodes.isEmpty()) {
          if (message.getStoryType().isTextStory()) {
            Log.d(TAG, "No attachments for given text story. Skipping.");
            continue;
          } else {
            Log.e(TAG, "No attachments for given media story. Aborting.");
            throw new MmsException("No attachment for story.");
          }
        }

        List<AttachmentId> attachmentIds = nodes.stream().map(UploadDependencyGraph.Node::getAttachmentId).collect(Collectors.toList());
        ZonaRosaDatabase.attachments().updateMessageId(attachmentIds, messageId, true);
        for (final AttachmentId attachmentId : attachmentIds) {
          ZonaRosaDatabase.attachments().updateAttachmentCaption(attachmentId, message.getBody());
        }
      }

      database.setTransactionSuccessful();
    } catch (MmsException e) {
      Log.w(TAG, "Failed to send stories.", e);
      return;
    } finally {
      database.endTransaction();
    }

    List<JobManager.Chain> chains = dependencyGraph.consumeDeferredQueue();
    for (final JobManager.Chain chain : chains) {
      chain.enqueue();
    }

    for (int i = 0; i < messageIds.size(); i++) {
      long            messageId = messageIds.get(i);
      OutgoingMessage message   = messages.get(i);
      Recipient       recipient = message.getThreadRecipient();
      List<UploadDependencyGraph.Node> dependencies = dependencyGraph.getDependencyMap().get(message);

      List<String> jobDependencyIds = (dependencies != null) ? dependencies.stream().map(UploadDependencyGraph.Node::getJobId).collect(Collectors.toList())
                                                             : Collections.emptyList();

      sendMessageInternal(context,
                          recipient,
                          SendType.ZONAROSA,
                          messageId,
                          null,
                          jobDependencyIds
      );
    }

    onMessageSent();

    for (long threadId : threads) {
      threadTable.update(threadId, true, true);
    }
  }

  public static long send(final Context context,
                          final OutgoingMessage message,
                          final long threadId,
                          @NonNull SendType sendType,
                          @Nullable final String metricId,
                          @Nullable final MessageTable.InsertListener insertListener)
  {
    Log.i(TAG, "Sending media message to " + message.getThreadRecipient().getId() + ", thread: " + threadId);
    try {
      ThreadTable  threadTable = ZonaRosaDatabase.threads();
      MessageTable database    = ZonaRosaDatabase.messages();

      long         allocatedThreadId = threadTable.getOrCreateValidThreadId(message.getThreadRecipient(), threadId, message.getDistributionType());
      Recipient    recipient         = message.getThreadRecipient();
      InsertResult insertResult      = database.insertMessageOutbox(applyUniversalExpireTimerIfNecessary(context, recipient, message, allocatedThreadId), allocatedThreadId, sendType != SendType.ZONAROSA, insertListener);
      long         messageId         = insertResult.getMessageId();


      if (message.getThreadRecipient().isGroup()) {
        if (message.getAttachments().isEmpty() && message.getLinkPreviews().isEmpty() && message.getSharedContacts().isEmpty()) {
          ZonaRosaLocalMetrics.GroupMessageSend.onInsertedIntoDatabase(messageId, metricId);
        } else {
          ZonaRosaLocalMetrics.GroupMessageSend.cancel(messageId);
        }
      } else {
        ZonaRosaLocalMetrics.IndividualMessageSend.onInsertedIntoDatabase(messageId, metricId);
      }

      sendMessageInternal(context, recipient, sendType, messageId, insertResult.getQuoteAttachmentId(), Collections.emptyList());
      onMessageSent();
      threadTable.update(allocatedThreadId, true, true);

      return allocatedThreadId;
    } catch (MmsException e) {
      Log.w(TAG, e);
      return threadId;
    }
  }

  public static long sendPollAction(final Context context,
                              final OutgoingMessage message,
                              final long threadId,
                              @NonNull SendType sendType,
                              @Nullable final String metricId,
                              @Nullable final MessageTable.InsertListener insertListener)
  {
    try {
      Recipient    recipient         = message.getThreadRecipient();
      long         allocatedThreadId = ZonaRosaDatabase.threads().getOrCreateValidThreadId(recipient, threadId, message.getDistributionType());
      InsertResult insertResult      = ZonaRosaDatabase.messages().insertMessageOutbox(applyUniversalExpireTimerIfNecessary(context, recipient, message, allocatedThreadId), allocatedThreadId, sendType != SendType.ZONAROSA, insertListener);
      long         messageId         = insertResult.getMessageId();

      if (!recipient.isPushV2Group()) {
        ZonaRosaLocalMetrics.IndividualMessageSend.onInsertedIntoDatabase(messageId, metricId);
      } else {
        ZonaRosaLocalMetrics.GroupMessageSend.onInsertedIntoDatabase(messageId, metricId);
      }

      sendMessageInternal(context, recipient, sendType, messageId, insertResult.getQuoteAttachmentId(), Collections.emptyList());
      onMessageSent();
      ZonaRosaDatabase.threads().update(allocatedThreadId, true, true);

      return allocatedThreadId;
    } catch (MmsException e) {
      Log.w(TAG, e);
      return threadId;
    }
  }

  public static boolean sendPushWithPreUploadedMedia(final Context context,
                                                     final OutgoingMessage message,
                                                     final Collection<PreUploadResult> preUploadResults,
                                                     final long threadId,
                                                     final MessageTable.InsertListener insertListener)  {
    Log.i(TAG, "Sending media message with pre-uploads to " + message.getThreadRecipient().getId() + ", thread: " + threadId + ", pre-uploads: " + preUploadResults);
    Preconditions.checkArgument(message.getAttachments().isEmpty(), "If the media is pre-uploaded, there should be no attachments on the message.");

    try {
      ThreadTable     threadTable        = ZonaRosaDatabase.threads();
      MessageTable    mmsDatabase        = ZonaRosaDatabase.messages();
      AttachmentTable attachmentDatabase = ZonaRosaDatabase.attachments();

      Recipient recipient         = message.getThreadRecipient();
      long      allocatedThreadId = threadTable.getOrCreateValidThreadId(message.getThreadRecipient(), threadId);

      List<AttachmentId> attachmentIds = Stream.of(preUploadResults).map(PreUploadResult::getAttachmentId).toList();
      List<String>       jobIds        = Stream.of(preUploadResults).map(PreUploadResult::getJobIds).flatMap(Stream::of).toList();

      if (!attachmentDatabase.hasAttachments(attachmentIds)) {
        Log.w(TAG, "Attachments not found in database for " + message.getThreadRecipient().getId() + ", thread: " + threadId + ", pre-uploads: " + preUploadResults);
        return false;
      }

      InsertResult insertResult = mmsDatabase.insertMessageOutbox(applyUniversalExpireTimerIfNecessary(context, recipient, message, allocatedThreadId),
                                                                  allocatedThreadId,
                                                                  false,
                                                                  insertListener);
      long         messageId    = insertResult.getMessageId();

      for (AttachmentId attachmentId: attachmentIds) {
        boolean wasPreuploaded = ZonaRosaDatabase.attachments().getMessageId(attachmentId) == AttachmentTable.PREUPLOAD_MESSAGE_ID;
        if (wasPreuploaded && BackupRepository.shouldCopyAttachmentToArchive(attachmentId, messageId)) {
          Log.i(TAG, "[" + attachmentId + "] Was previously preuploaded and should now be copied to the archive.");
          AppDependencies.getJobManager().add(new CopyAttachmentToArchiveJob(attachmentId));
        }
      }

      attachmentDatabase.updateMessageId(attachmentIds, messageId, message.getStoryType().isStory());

      sendMessageInternal(context, recipient, SendType.ZONAROSA, messageId, insertResult.getQuoteAttachmentId(), jobIds);
      onMessageSent();
      threadTable.update(allocatedThreadId, true, true);

      return true;
    } catch (MmsException e) {
      Log.w(TAG, e);
      return false;
    }
  }

  public static void sendMediaBroadcast(@NonNull Context context,
                                        @NonNull List<OutgoingMessage> messages,
                                        @NonNull Collection<PreUploadResult> preUploadResults,
                                        boolean overwritePreUploadMessageIds)
  {
    Log.i(TAG, "Sending media broadcast (overwrite: " + overwritePreUploadMessageIds + ") to " + Stream.of(messages).map(m -> m.getThreadRecipient().getId()).toList());
    Preconditions.checkArgument(messages.size() > 0, "No messages!");
    Preconditions.checkArgument(Stream.of(messages).allMatch(m -> m.getAttachments().isEmpty()), "Messages can't have attachments! They should be pre-uploaded.");

    JobManager         jobManager                 = AppDependencies.getJobManager();
    AttachmentTable    attachmentDatabase         = ZonaRosaDatabase.attachments();
    MessageTable       mmsDatabase                = ZonaRosaDatabase.messages();
    ThreadTable        threadTable                = ZonaRosaDatabase.threads();
    List<AttachmentId> preUploadAttachmentIds     = Stream.of(preUploadResults).map(PreUploadResult::getAttachmentId).toList();
    List<String>       preUploadJobIds            = Stream.of(preUploadResults).map(PreUploadResult::getJobIds).flatMap(Stream::of).toList();
    List<Long>         messageIds                 = new ArrayList<>(messages.size());
    List<String>       messageDependsOnIds        = new ArrayList<>(preUploadJobIds);
    OutgoingMessage    primaryMessage             = messages.get(0);
    List<AttachmentId> attachmentsWithPreuploadId = preUploadAttachmentIds.stream().filter(id -> ZonaRosaDatabase.attachments().getMessageId(id) == AttachmentTable.PREUPLOAD_MESSAGE_ID).collect(Collectors.toList());

    mmsDatabase.beginTransaction();
    try {
      if (overwritePreUploadMessageIds) {
        long primaryThreadId  = threadTable.getOrCreateThreadIdFor(primaryMessage.getThreadRecipient(), primaryMessage.getDistributionType());
        long primaryMessageId = mmsDatabase.insertMessageOutbox(applyUniversalExpireTimerIfNecessary(context, primaryMessage.getThreadRecipient(), primaryMessage, primaryThreadId),
                                                                primaryThreadId,
                                                                false,
                                                                null).getMessageId();

        attachmentDatabase.updateMessageId(preUploadAttachmentIds, primaryMessageId, primaryMessage.getStoryType().isStory());
        if (primaryMessage.getStoryType() != StoryType.NONE) {
          for (final AttachmentId preUploadAttachmentId : preUploadAttachmentIds) {
            attachmentDatabase.updateAttachmentCaption(preUploadAttachmentId, primaryMessage.getBody());
          }
        }
        messageIds.add(primaryMessageId);
      }

      List<DatabaseAttachment> preUploadAttachments = Stream.of(preUploadAttachmentIds)
                                                            .map(attachmentDatabase::getAttachment)
                                                            .toList();

      if (messages.size() > 0) {
        List<OutgoingMessage>    secondaryMessages = overwritePreUploadMessageIds ? messages.subList(1, messages.size()) : messages;
        List<List<AttachmentId>> attachmentCopies  = new ArrayList<>();

        for (int i = 0; i < preUploadAttachmentIds.size(); i++) {
          attachmentCopies.add(new ArrayList<>(messages.size()));
        }

        for (OutgoingMessage secondaryMessage : secondaryMessages) {
          long               allocatedThreadId = threadTable.getOrCreateThreadIdFor(secondaryMessage.getThreadRecipient(), secondaryMessage.getDistributionType());
          long               messageId         = mmsDatabase.insertMessageOutbox(applyUniversalExpireTimerIfNecessary(context, secondaryMessage.getThreadRecipient(), secondaryMessage, allocatedThreadId),
                                                                                 allocatedThreadId,
                                                                                 false,
                                                                                 null).getMessageId();
          List<AttachmentId> attachmentIds     = new ArrayList<>(preUploadAttachmentIds.size());

          for (int i = 0; i < preUploadAttachments.size(); i++) {
            AttachmentId attachmentId = attachmentDatabase.insertAttachmentForPreUpload(preUploadAttachments.get(i)).attachmentId;
            attachmentCopies.get(i).add(attachmentId);
            attachmentIds.add(attachmentId);
          }

          attachmentDatabase.updateMessageId(attachmentIds, messageId, secondaryMessage.getStoryType().isStory());
          if (primaryMessage.getStoryType() != StoryType.NONE) {
            for (final AttachmentId preUploadAttachmentId : attachmentIds) {
              attachmentDatabase.updateAttachmentCaption(preUploadAttachmentId, primaryMessage.getBody());
            }
          }

          messageIds.add(messageId);
        }

        for (int i = 0; i < attachmentCopies.size(); i++) {
          Job copyJob = new AttachmentCopyJob(preUploadAttachmentIds.get(i), attachmentCopies.get(i));
          jobManager.add(copyJob, preUploadJobIds);
          messageDependsOnIds.add(copyJob.getId());
        }
      }

      for (int i = 0; i < messageIds.size(); i++) {
        long            messageId = messageIds.get(i);
        OutgoingMessage message   = messages.get(i);
        Recipient       recipient = message.getThreadRecipient();

        if (recipient.isDistributionList()) {
          List<RecipientId> members        = ZonaRosaDatabase.distributionLists().getMembers(recipient.requireDistributionListId());
          DistributionId    distributionId = Objects.requireNonNull(ZonaRosaDatabase.distributionLists().getDistributionId(recipient.requireDistributionListId()));
          ZonaRosaDatabase.storySends().insert(messageId, members, message.getSentTimeMillis(), message.getStoryType().isStoryWithReplies(), distributionId);
        }
      }

      for (AttachmentId attachmentId : attachmentsWithPreuploadId) {
        long messageId = ZonaRosaDatabase.attachments().getMessageId(attachmentId);
        if (BackupRepository.shouldCopyAttachmentToArchive(attachmentId, messageId)) {
          Log.i(TAG, "[" + attachmentId + "] Was previously preuploaded and should now be copied to the archive.");
          jobManager.add(new CopyAttachmentToArchiveJob(attachmentId));
        }
      }

      onMessageSent();
      mmsDatabase.setTransactionSuccessful();
    } catch (MmsException e) {
      Log.w(TAG, "Failed to send messages.", e);
      return;
    } finally {
      mmsDatabase.endTransaction();
    }

    for (int i = 0; i < messageIds.size(); i++) {
      long      messageId = messageIds.get(i);
      Recipient recipient = messages.get(i).getThreadRecipient();

      if (recipient.isPushGroup()) {
        jobManager.add(new PushGroupSendJob(messageId, recipient.getId(), Collections.emptySet(), true, false), messageDependsOnIds, recipient.getId().toQueueKey());
      } else if (recipient.isDistributionList()) {
        jobManager.add(new PushDistributionListSendJob(messageId, recipient.getId(), true, Collections.emptySet()), messageDependsOnIds, recipient.getId().toQueueKey());
      } else {
        jobManager.add(IndividualSendJob.create(messageId, recipient, true, false), messageDependsOnIds, recipient.getId().toQueueKey());
      }
    }
  }

  /**
   * @return A result if the attachment was enqueued, or null if it failed to enqueue or shouldn't
   *         be enqueued (like in the case of a local self-send).
   */
  public static @Nullable PreUploadResult preUploadPushAttachment(@NonNull Context context, @NonNull Attachment attachment, @Nullable Recipient recipient, @NonNull Media media) {
    Log.i(TAG, "Pre-uploading attachment for " + (recipient != null ? recipient.getId() : "null"));

    try {
      AttachmentTable    attachmentDatabase = ZonaRosaDatabase.attachments();
      DatabaseAttachment databaseAttachment = attachmentDatabase.insertAttachmentForPreUpload(attachment);

      Job compressionJob = AttachmentCompressionJob.fromAttachment(databaseAttachment, false, -1);
      Job uploadJob      = new AttachmentUploadJob(databaseAttachment.attachmentId);

      AppDependencies.getJobManager()
                     .startChain(compressionJob)
                     .then(uploadJob)
                     .enqueue();

      return new PreUploadResult(media, databaseAttachment.attachmentId, Arrays.asList(compressionJob.getId(), uploadJob.getId()));
    } catch (MmsException e) {
      Log.w(TAG, "preUploadPushAttachment() - Failed to upload!", e);
      return null;
    }
  }

  public static void sendNewReaction(@NonNull Context context, @NonNull MessageId messageId, @NonNull String emoji) {
    ReactionRecord reaction = new ReactionRecord(emoji, Recipient.self().getId(), System.currentTimeMillis(), System.currentTimeMillis());
    ZonaRosaDatabase.reactions().addReaction(messageId, reaction);

    try {
      AppDependencies.getJobManager().add(ReactionSendJob.create(context, messageId, reaction, false));
      onMessageSent();
    } catch (NoSuchMessageException e) {
      Log.w(TAG, "[sendNewReaction] Could not find message! Ignoring.");
    }
  }

  public static void sendReactionRemoval(@NonNull Context context, @NonNull MessageId messageId, @NonNull ReactionRecord reaction) {
    ZonaRosaDatabase.reactions().deleteReaction(messageId, reaction.getAuthor());

    try {
      AppDependencies.getJobManager().add(ReactionSendJob.create(context, messageId, reaction, true));
      onMessageSent();
    } catch (NoSuchMessageException e) {
      Log.w(TAG, "[sendReactionRemoval] Could not find message! Ignoring.");
    }
  }

  public static void sendRemoteDelete(long messageId) {
    MessageTable db = ZonaRosaDatabase.messages();
    db.markAsDeleteBySelf(messageId);
    db.markAsSending(messageId);

    try {
      RemoteDeleteSendJob.create(messageId).enqueue();
      onMessageSent();
    } catch (NoSuchMessageException e) {
      Log.w(TAG, "[sendRemoteDelete] Could not find message! Ignoring.");
    }
  }

  public static void sendAdminDelete(long messageId) {
    // TODO(michelle): Update with failure states
    ZonaRosaDatabase.messages().markAsDeleteBySelf(messageId);
    AdminDeleteSendJob job = AdminDeleteSendJob.create(messageId);
    if (job != null) {
      AppDependencies.getJobManager().add(job);
    } else {
      Log.w(TAG, "[sendAdminDelete] Could not create the admin delete job.");
    }
  }

  public static void resendGroupMessage(@NonNull Context context, @NonNull MessageRecord messageRecord, @NonNull Set<RecipientId> filterRecipientIds) {
    if (!messageRecord.isMms()) throw new AssertionError("Not Group");
    sendGroupPush(context, messageRecord.getToRecipient(), messageRecord.getId(), filterRecipientIds, Collections.emptyList());
    onMessageSent();
  }

  public static void resendDistributionList(@NonNull Context context, @NonNull MessageRecord messageRecord, @NonNull Set<RecipientId> filterRecipientIds) {
    if (!messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getStoryType().isStory()) {
      throw new AssertionError("Not a story");
    }
    sendDistributionList(context, messageRecord.getToRecipient(), messageRecord.getId(), filterRecipientIds, Collections.emptyList());
    onMessageSent();
  }

  @WorkerThread
  public static void resend(Context context, MessageRecord messageRecord) {
    long       messageId   = messageRecord.getId();
    boolean    forceSms    = messageRecord.isForcedSms();
    Recipient  recipient   = messageRecord.getToRecipient();

    SendType sendType;

    if (forceSms) {
      Recipient threadRecipient = ZonaRosaDatabase.threads().getRecipientForThreadId(messageRecord.getThreadId());

      if ((threadRecipient != null && threadRecipient.isGroup()) || ZonaRosaDatabase.attachments().getAttachmentsForMessage(messageId).size() > 0) {
        sendType = SendType.MMS;
      } else {
        sendType = SendType.SMS;
      }
    } else {
      sendType = SendType.ZONAROSA;
    }

    AttachmentId quoteAttachmentId = ZonaRosaDatabase.attachments()
                                                   .getAttachmentsForMessage(messageId)
                                                   .stream()
                                                   .filter(it -> it.quote)
                                                   .findFirst()
                                                   .map(it -> it.attachmentId)
                                                   .orElse(null);

    sendMessageInternal(context, recipient, sendType, messageId, quoteAttachmentId, Collections.emptyList());

    onMessageSent();
  }

  public static void onMessageSent() {
    EventBus.getDefault().postSticky(MessageSentEvent.INSTANCE);
  }

  private static @NonNull OutgoingMessage applyUniversalExpireTimerIfNecessary(@NonNull Context context, @NonNull Recipient recipient, @NonNull OutgoingMessage outgoingMessage, long threadId) {
    if (!outgoingMessage.isExpirationUpdate() && outgoingMessage.getExpiresIn() == 0) {
      Integer expireTimerVersion = RecipientUtil.setAndSendUniversalExpireTimerIfNecessary(context, recipient, threadId);

      if (expireTimerVersion != null) {
        return outgoingMessage.withExpiry(TimeUnit.SECONDS.toMillis(ZonaRosaStore.settings().getUniversalExpireTimer()), expireTimerVersion);
      }
    }
    return outgoingMessage;
  }

  @WorkerThread
  private static void sendMessageInternal(Context context,
                                          Recipient recipient,
                                          SendType sendType,
                                          long messageId,
                                          @Nullable AttachmentId quoteAttachmentId,
                                          @NonNull Collection<String> uploadJobIds)
  {
    Set<String> finalUploadJobIds = new HashSet<>(uploadJobIds);

    if (quoteAttachmentId != null && ZonaRosaDatabase.attachments().hasData(quoteAttachmentId) && uploadJobIds.size() > 0) {
      Job uploadJob = new AttachmentUploadJob(quoteAttachmentId);
      AppDependencies.getJobManager().add(uploadJob);
      finalUploadJobIds.add(uploadJob.getId());
    }

    if (recipient.isPushGroup()) {
      sendGroupPush(context, recipient, messageId, Collections.emptySet(), finalUploadJobIds);
    } else if (recipient.isDistributionList()) {
      sendDistributionList(context, recipient, messageId, Collections.emptySet(), finalUploadJobIds);
    } else if (sendType == SendType.ZONAROSA && isPushMediaSend(context, recipient)) {
      sendMediaPush(context, recipient, messageId, finalUploadJobIds);
    } else {
      Log.w(TAG, "Unknown send type!");
    }
  }

  @WorkerThread
  private static void sendMediaPush(Context context, Recipient recipient, long messageId, @NonNull Collection<String> uploadJobIds) {
    JobManager jobManager = AppDependencies.getJobManager();

    if (uploadJobIds.size() > 0) {
      Job mediaSend = IndividualSendJob.create(messageId, recipient, true, false);
      jobManager.add(mediaSend, uploadJobIds);
    } else {
      IndividualSendJob.enqueue(context, jobManager, messageId, recipient, false);
    }
  }

  @WorkerThread
  private static void sendGroupPush(@NonNull Context context, @NonNull Recipient recipient, long messageId, @NonNull Set<RecipientId> filterRecipientIds, @NonNull Collection<String> uploadJobIds) {
    JobManager jobManager = AppDependencies.getJobManager();

    if (uploadJobIds.size() > 0) {
      Job groupSend = new PushGroupSendJob(messageId, recipient.getId(), filterRecipientIds, !uploadJobIds.isEmpty(), false);
      jobManager.add(groupSend, uploadJobIds, uploadJobIds.isEmpty() ? null : recipient.getId().toQueueKey());
    } else {
      PushGroupSendJob.enqueue(context, jobManager, messageId, recipient.getId(), filterRecipientIds, false);
    }
  }

  @WorkerThread
  private static void sendDistributionList(@NonNull Context context, @NonNull Recipient recipient, long messageId, @NonNull Set<RecipientId> filterRecipientIds, @NonNull Collection<String> uploadJobIds) {
    JobManager jobManager = AppDependencies.getJobManager();

    if (uploadJobIds.size() > 0) {
      Job groupSend = new PushDistributionListSendJob(messageId, recipient.getId(), !uploadJobIds.isEmpty(), filterRecipientIds);
      jobManager.add(groupSend, uploadJobIds, uploadJobIds.isEmpty() ? null : recipient.getId().toQueueKey());
    } else {
      PushDistributionListSendJob.enqueue(context, jobManager, messageId, recipient.getId(), filterRecipientIds);
    }
  }

  private static boolean isPushMediaSend(Context context, Recipient recipient) {
    if (!ZonaRosaStore.account().isRegistered()) {
      return false;
    }

    if (recipient.isGroup()) {
      return false;
    }

    return isPushDestination(context, recipient);
  }

  private static boolean isPushDestination(Context context, Recipient destination) {
    if (destination.resolve().getRegistered() == RecipientTable.RegisteredState.REGISTERED) {
      return true;
    } else if (destination.resolve().getRegistered() == RecipientTable.RegisteredState.NOT_REGISTERED) {
      return false;
    } else {
      try {
        RecipientTable.RegisteredState state = ContactDiscovery.refresh(context, destination, false);
        return state == RecipientTable.RegisteredState.REGISTERED;
      } catch (IOException e1) {
        Log.w(TAG, e1);
        return false;
      }
    }
  }

  public static class PreUploadResult implements Parcelable {
    private final Media              media;
    private final AttachmentId       attachmentId;
    private final Collection<String> jobIds;

    PreUploadResult(@NonNull Media media, @NonNull AttachmentId attachmentId, @NonNull Collection<String> jobIds) {
      this.media        = media;
      this.attachmentId = attachmentId;
      this.jobIds       = jobIds;
    }

    private PreUploadResult(Parcel in) {
      this.attachmentId = in.readParcelable(AttachmentId.class.getClassLoader());
      this.jobIds       = ParcelUtil.readStringCollection(in);
      this.media        = in.readParcelable(Media.class.getClassLoader());
    }

    public @NonNull AttachmentId getAttachmentId() {
      return attachmentId;
    }

    public @NonNull Collection<String> getJobIds() {
      return jobIds;
    }

    public @NonNull Media getMedia() {
      return media;
    }

    public static final Creator<PreUploadResult> CREATOR = new Creator<PreUploadResult>() {
      @Override
      public PreUploadResult createFromParcel(Parcel in) {
        return new PreUploadResult(in);
      }

      @Override
      public PreUploadResult[] newArray(int size) {
        return new PreUploadResult[size];
      }
    };

    @Override
    public int describeContents() {
      return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeParcelable(attachmentId, flags);
      ParcelUtil.writeStringCollection(dest, jobIds);
      dest.writeParcelable(media, flags);
    }

    @Override
    public @NonNull String toString() {
      return "{ID: " + attachmentId.id + ", URI: " + media.getUri() + ", Jobs: " + jobIds.stream().map(j -> "JOB::" + j).collect(Collectors.toList()) + "}";
    }
  }

  public enum MessageSentEvent {
    INSTANCE
  }

  public enum SendType {
    ZONAROSA, SMS, MMS
  }
}

package io.zonarosa.messenger.jobs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Stream;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.attachments.Attachment;
import io.zonarosa.messenger.crypto.SealedSenderAccessUtil;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.PaymentTable;
import io.zonarosa.messenger.database.RecipientTable.SealedSenderAccessMode;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.MessageId;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.mms.MmsException;
import io.zonarosa.messenger.mms.OutgoingMessage;
import io.zonarosa.messenger.ratelimit.ProofRequiredExceptionHandler;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.service.ExpiringMessageManager;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.messenger.transport.UndeliverableMessageException;
import io.zonarosa.messenger.util.MessageUtil;
import io.zonarosa.messenger.util.ZonaRosaLocalMetrics;
import io.zonarosa.core.util.Util;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender.IndividualSendEvents;
import io.zonarosa.service.api.crypto.ContentHint;
import io.zonarosa.service.api.crypto.UntrustedIdentityException;
import io.zonarosa.service.api.messages.SendMessageResult;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment;
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceEditMessage;
import io.zonarosa.service.api.messages.ZonaRosaServicePreview;
import io.zonarosa.service.api.messages.shared.SharedContact;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;
import io.zonarosa.service.api.push.exceptions.ProofRequiredException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;
import io.zonarosa.service.api.push.exceptions.UnregisteredUserException;
import io.zonarosa.core.util.UuidUtil;
import io.zonarosa.service.internal.push.BodyRange;
import io.zonarosa.service.internal.push.DataMessage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import okio.Utf8;

public class IndividualSendJob extends PushSendJob {

  public static final String KEY = "PushMediaSendJob";

  private static final String TAG = Log.tag(IndividualSendJob.class);

  private static final String KEY_MESSAGE_ID = "message_id";

  private final long messageId;

  public IndividualSendJob(long messageId, @NonNull Recipient recipient, boolean hasMedia, boolean isScheduledSend) {
    this(new Parameters.Builder()
             .setQueue(isScheduledSend ? recipient.getId().toScheduledSendQueueKey() : recipient.getId().toQueueKey(hasMedia))
             .addConstraint(NetworkConstraint.KEY)
             .setLifespan(TimeUnit.DAYS.toMillis(1))
             .setMaxAttempts(Parameters.UNLIMITED)
             .build(),
         messageId);
  }

  private IndividualSendJob(Job.Parameters parameters, long messageId) {
    super(parameters);
    this.messageId = messageId;
  }

  public static Job create(long messageId, @NonNull Recipient recipient, boolean hasMedia, boolean isScheduledSend) {
    if (!recipient.getHasServiceId()) {
      throw new AssertionError("No ServiceId!");
    }

    if (recipient.isGroup()) {
      throw new AssertionError("This job does not send group messages!");
    }

    return new IndividualSendJob(messageId, recipient, hasMedia, isScheduledSend);
  }

  @WorkerThread
  public static void enqueue(@NonNull Context context, @NonNull JobManager jobManager, long messageId, @NonNull Recipient recipient, boolean isScheduledSend) {
    try {
      OutgoingMessage message = ZonaRosaDatabase.messages().getOutgoingMessage(messageId);
      if (message.getScheduledDate() != -1) {
        AppDependencies.getScheduledMessageManager().scheduleIfNecessary();
        return;
      }

      Set<String> attachmentUploadIds = enqueueCompressingAndUploadAttachmentsChains(jobManager, message);
      boolean     hasMedia            = attachmentUploadIds.size() > 0;
      boolean     addHardDependencies = hasMedia && !isScheduledSend;

      jobManager.add(IndividualSendJob.create(messageId, recipient, hasMedia, isScheduledSend),
                     attachmentUploadIds,
                     addHardDependencies ? recipient.getId().toQueueKey() : null);
    } catch (NoSuchMessageException | MmsException e) {
      Log.w(TAG, "Failed to enqueue message.", e);
      ZonaRosaDatabase.messages().markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    }
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putLong(KEY_MESSAGE_ID, messageId).serialize();
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
      throws IOException, MmsException, NoSuchMessageException, UndeliverableMessageException, RetryLaterException
  {
    ZonaRosaLocalMetrics.IndividualMessageSend.onJobStarted(messageId);

    ExpiringMessageManager expirationManager     = AppDependencies.getExpiringMessageManager();
    MessageTable           database              = ZonaRosaDatabase.messages();
    OutgoingMessage        message               = database.getOutgoingMessage(messageId);
    long                   threadId              = database.getMessageRecord(messageId).getThreadId();
    MessageRecord          originalEditedMessage = message.getMessageToEdit() > 0 ? ZonaRosaDatabase.messages().getMessageRecordOrNull(message.getMessageToEdit()) : null;

    if (database.isSent(messageId)) {
      warn(TAG, String.valueOf(message.getSentTimeMillis()), "Message " + messageId + " was already sent. Ignoring.");
      return;
    }

    try {
      log(TAG, String.valueOf(message.getSentTimeMillis()), "Sending message: " + messageId + ", Recipient: " + message.getThreadRecipient()
                                                                                                                       .getId() + ", Thread: " + threadId + ", Attachments: " + buildAttachmentString(message.getAttachments()) + ", Editing: " + (originalEditedMessage != null ? originalEditedMessage.getDateSent() : "N/A"));

      RecipientUtil.shareProfileIfFirstSecureMessage(message.getThreadRecipient());

      Recipient              recipient  = message.getThreadRecipient().fresh();
      byte[]                 profileKey = recipient.getProfileKey();
      SealedSenderAccessMode accessMode = recipient.getSealedSenderAccessMode();

      boolean unidentified = deliver(message, originalEditedMessage);

      database.markAsSent(messageId, true);
      markAttachmentsUploaded(messageId, message);
      database.markUnidentified(messageId, unidentified);

      // For scheduled messages, which may not have updated the thread with it's snippet yet
      ZonaRosaDatabase.threads().updateSilently(threadId, false);

      if (recipient.isSelf()) {
        ZonaRosaDatabase.messages().incrementDeliveryReceiptCount(message.getSentTimeMillis(), recipient.getId(), System.currentTimeMillis());
        ZonaRosaDatabase.messages().incrementReadReceiptCount(message.getSentTimeMillis(), recipient.getId(), System.currentTimeMillis());
        ZonaRosaDatabase.messages().incrementViewedReceiptCount(message.getSentTimeMillis(), recipient.getId(), System.currentTimeMillis());
      }

      if (unidentified && accessMode == SealedSenderAccessMode.UNKNOWN && profileKey == null) {
        log(TAG, String.valueOf(message.getSentTimeMillis()), "Marking recipient as UD-unrestricted following a UD send.");
        ZonaRosaDatabase.recipients().setSealedSenderAccessMode(recipient.getId(), SealedSenderAccessMode.UNRESTRICTED);
      } else if (unidentified && accessMode == SealedSenderAccessMode.UNKNOWN) {
        log(TAG, String.valueOf(message.getSentTimeMillis()), "Marking recipient as UD-enabled following a UD send.");
        ZonaRosaDatabase.recipients().setSealedSenderAccessMode(recipient.getId(), SealedSenderAccessMode.ENABLED);
      } else if (!unidentified && accessMode != SealedSenderAccessMode.DISABLED) {
        log(TAG, String.valueOf(message.getSentTimeMillis()), "Marking recipient as UD-disabled following a non-UD send.");
        ZonaRosaDatabase.recipients().setSealedSenderAccessMode(recipient.getId(), SealedSenderAccessMode.DISABLED);
      }

      if (originalEditedMessage != null && originalEditedMessage.getExpireStarted() > 0) {
        database.markExpireStarted(messageId, originalEditedMessage.getExpireStarted());
        expirationManager.scheduleDeletion(messageId, true, originalEditedMessage.getExpireStarted(), originalEditedMessage.getExpiresIn());
      } else if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
        database.markExpireStarted(messageId);
        expirationManager.scheduleDeletion(messageId, true, message.getExpiresIn());
      }

      if (message.isViewOnce()) {
        ZonaRosaDatabase.attachments().deleteAttachmentFilesForViewOnceMessage(messageId);
      }

      ConversationShortcutRankingUpdateJob.enqueueForOutgoingIfNecessary(recipient);

      log(TAG, String.valueOf(message.getSentTimeMillis()), "Sent message: " + messageId);

    } catch (UnregisteredUserException uue) {
      warn(TAG, "Failure", uue);
      database.markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
      AppDependencies.getJobManager().add(new DirectoryRefreshJob(false));
    } catch (UntrustedIdentityException uie) {
      warn(TAG, "Failure", uie);
      Recipient recipient = Recipient.external(uie.getIdentifier());
      if (recipient == null) {
        Log.w(TAG, "Failed to create a Recipient for the identifier!");
        return;
      }
      database.addMismatchedIdentity(messageId, recipient.getId(), uie.getIdentityKey());
      database.markAsSentFailed(messageId);
      RetrieveProfileJob.enqueue(recipient.getId(), true);
    } catch (ProofRequiredException e) {
      ProofRequiredExceptionHandler.Result result = ProofRequiredExceptionHandler.handle(context, e, ZonaRosaDatabase.threads().getRecipientForThreadId(threadId), threadId, messageId);
      if (result.isRetry()) {
        throw new RetryLaterException();
      } else {
        throw e;
      }
    }

    ZonaRosaLocalMetrics.IndividualMessageSend.onJobFinished(messageId);
  }

  @Override
  public void onRetry() {
    ZonaRosaLocalMetrics.IndividualMessageSend.cancel(messageId);
    super.onRetry();
  }

  @Override
  public void onFailure() {
    ZonaRosaLocalMetrics.IndividualMessageSend.cancel(messageId);
    ZonaRosaDatabase.messages().markAsSentFailed(messageId);
    notifyMediaMessageDeliveryFailed(context, messageId);
  }

  private boolean deliver(OutgoingMessage message, MessageRecord originalEditedMessage)
      throws IOException, UnregisteredUserException, UntrustedIdentityException, UndeliverableMessageException
  {
    if (message.getThreadRecipient() == null) {
      throw new UndeliverableMessageException("No destination address.");
    }

    if (Utf8.size(message.getBody()) > MessageUtil.MAX_INLINE_BODY_SIZE_BYTES) {
      throw new UndeliverableMessageException("The total body size was greater than our limit of " + MessageUtil.MAX_INLINE_BODY_SIZE_BYTES + " bytes.");
    }

    try {
      rotateSenderCertificateIfNecessary();

      Recipient messageRecipient = message.getThreadRecipient().fresh();

      if (messageRecipient.isUnregistered()) {
        throw new UndeliverableMessageException(messageRecipient.getId() + " not registered!");
      }

      if (!messageRecipient.getHasServiceId()) {
        messageRecipient = messageRecipient.fresh();

        if (!messageRecipient.getHasServiceId()) {
          throw new UndeliverableMessageException(messageRecipient.getId() + " has no serviceId!");
        }
      }

      ZonaRosaServiceMessageSender                 messageSender      = AppDependencies.getZonaRosaServiceMessageSender();
      ZonaRosaServiceAddress                       address            = RecipientUtil.toZonaRosaServiceAddress(context, messageRecipient);
      List<Attachment>                           attachments        = Stream.of(message.getAttachments()).filterNot(Attachment::isSticker).toList();
      List<ZonaRosaServiceAttachment>              serviceAttachments = getAttachmentPointersFor(attachments);
      Optional<byte[]>                           profileKey         = getProfileKey(messageRecipient);
      Optional<ZonaRosaServiceDataMessage.Sticker> sticker            = getStickerFor(message);
      List<SharedContact>                        sharedContacts     = getSharedContactsFor(message);
      List<ZonaRosaServicePreview>                 previews           = getPreviewsFor(message);
      ZonaRosaServiceDataMessage.GiftBadge         giftBadge          = getGiftBadgeFor(message);
      ZonaRosaServiceDataMessage.Payment           payment            = getPayment(message);
      List<BodyRange>                            bodyRanges         = getBodyRanges(message);
      ZonaRosaServiceDataMessage.PollCreate        pollCreate         = getPollCreate(message);
      ZonaRosaServiceDataMessage.PollTerminate     pollTerminate      = getPollTerminate(message);
      ZonaRosaServiceDataMessage.PinnedMessage     pinnedMessage      = getPinnedMessage(message);
      ZonaRosaServiceDataMessage.Builder mediaMessageBuilder = ZonaRosaServiceDataMessage.newBuilder()
                                                                                     .withBody(message.getBody())
                                                                                     .withAttachments(serviceAttachments)
                                                                                     .withTimestamp(message.getSentTimeMillis())
                                                                                     .withExpiration((int) (message.getExpiresIn() / 1000))
                                                                                     .withExpireTimerVersion(message.getExpireTimerVersion())
                                                                                     .withViewOnce(message.isViewOnce())
                                                                                     .withProfileKey(profileKey.orElse(null))
                                                                                     .withSticker(sticker.orElse(null))
                                                                                     .withSharedContacts(sharedContacts)
                                                                                     .withPreviews(previews)
                                                                                     .withGiftBadge(giftBadge)
                                                                                     .asExpirationUpdate(message.isExpirationUpdate())
                                                                                     .asEndSessionMessage(message.isEndSession())
                                                                                     .withPayment(payment)
                                                                                     .withBodyRanges(bodyRanges)
                                                                                     .withPollCreate(pollCreate)
                                                                                     .withPollTerminate(pollTerminate)
                                                                                     .withPinnedMessage(pinnedMessage);

      if (message.getParentStoryId() != null) {
        try {
          MessageRecord storyRecord    = ZonaRosaDatabase.messages().getMessageRecord(message.getParentStoryId().asMessageId().getId());
          Recipient     storyRecipient = storyRecord.getFromRecipient();

          ZonaRosaServiceDataMessage.StoryContext storyContext = new ZonaRosaServiceDataMessage.StoryContext(storyRecipient.requireServiceId(), storyRecord.getDateSent());
          mediaMessageBuilder.withStoryContext(storyContext);

          Optional<ZonaRosaServiceDataMessage.Reaction> reaction = getStoryReactionFor(message, storyContext);
          if (reaction.isPresent()) {
            mediaMessageBuilder.withReaction(reaction.get());
            mediaMessageBuilder.withBody(null);
          }
        } catch (NoSuchMessageException e) {
          throw new UndeliverableMessageException(e);
        }
      } else {
        mediaMessageBuilder.withQuote(getQuoteFor(message).orElse(null));
      }

      if (message.getGiftBadge() != null || message.isPaymentsNotification()) {
        mediaMessageBuilder.withBody(null);
      }

      ZonaRosaServiceDataMessage mediaMessage = mediaMessageBuilder.build();

      if (originalEditedMessage != null) {
        if (Util.equals(ZonaRosaStore.account().getAci(), address.getServiceId())) {
          SendMessageResult result = messageSender.sendSelfSyncEditMessage(new ZonaRosaServiceEditMessage(originalEditedMessage.getDateSent(), mediaMessage));
          ZonaRosaDatabase.messageLog().insertIfPossible(messageRecipient.getId(), message.getSentTimeMillis(), result, ContentHint.RESENDABLE, new MessageId(messageId), false);

          return SealedSenderAccessUtil.getSealedSenderCertificate() != null;
        } else {
          SendMessageResult result = messageSender.sendEditMessage(address,
                                                                   SealedSenderAccessUtil.getSealedSenderAccessFor(messageRecipient),
                                                                   ContentHint.RESENDABLE,
                                                                   mediaMessage,
                                                                   IndividualSendEvents.EMPTY,
                                                                   message.isUrgent(),
                                                                   originalEditedMessage.getDateSent());
          ZonaRosaDatabase.messageLog().insertIfPossible(messageRecipient.getId(), message.getSentTimeMillis(), result, ContentHint.RESENDABLE, new MessageId(messageId), false);

          return result.getSuccess().isUnidentified();
        }
      } else if (Util.equals(ZonaRosaStore.account().getAci(), address.getServiceId())) {
        SendMessageResult result = messageSender.sendSyncMessage(mediaMessage);
        ZonaRosaDatabase.messageLog().insertIfPossible(messageRecipient.getId(), message.getSentTimeMillis(), result, ContentHint.RESENDABLE, new MessageId(messageId), false);
        return SealedSenderAccessUtil.getSealedSenderCertificate() != null;
      } else {
        ZonaRosaLocalMetrics.IndividualMessageSend.onDeliveryStarted(messageId, message.getSentTimeMillis());
        SendMessageResult result = messageSender.sendDataMessage(address,
                                                                 SealedSenderAccessUtil.getSealedSenderAccessFor(messageRecipient),
                                                                 ContentHint.RESENDABLE,
                                                                 mediaMessage,
                                                                 new MetricEventListener(messageId),
                                                                 message.isUrgent(),
                                                                 messageRecipient.getNeedsPniSignature());

        ZonaRosaDatabase.messageLog().insertIfPossible(messageRecipient.getId(), message.getSentTimeMillis(), result, ContentHint.RESENDABLE, new MessageId(messageId), message.isUrgent());

        if (messageRecipient.getNeedsPniSignature()) {
          ZonaRosaDatabase.pendingPniSignatureMessages().insertIfNecessary(messageRecipient.getId(), message.getSentTimeMillis(), result);
        }

        return result.getSuccess().isUnidentified();
      }
    } catch (FileNotFoundException e) {
      warn(TAG, String.valueOf(message.getSentTimeMillis()), e);
      throw new UndeliverableMessageException(e);
    } catch (ServerRejectedException e) {
      throw new UndeliverableMessageException(e);
    }
  }

  private ZonaRosaServiceDataMessage.Payment getPayment(OutgoingMessage message) {
    if (message.isPaymentsNotification()) {
      UUID                            paymentUuid = UuidUtil.parseOrThrow(message.getBody());
      PaymentTable.PaymentTransaction payment     = ZonaRosaDatabase.payments().getPayment(paymentUuid);

      if (payment == null) {
        Log.w(TAG, "Could not find payment, cannot send notification " + paymentUuid);
        return null;
      }

      if (payment.getReceipt() == null) {
        Log.w(TAG, "Could not find payment receipt, cannot send notification " + paymentUuid);
        return null;
      }

      return new ZonaRosaServiceDataMessage.Payment(new ZonaRosaServiceDataMessage.PaymentNotification(payment.getReceipt(), payment.getNote()), null);
    } else {
      DataMessage.Payment.Activation.Type type = null;

      if (message.isRequestToActivatePayments()) {
        type = DataMessage.Payment.Activation.Type.REQUEST;
      } else if (message.isPaymentsActivated()) {
        type = DataMessage.Payment.Activation.Type.ACTIVATED;
      }

      if (type != null) {
        return new ZonaRosaServiceDataMessage.Payment(null, new ZonaRosaServiceDataMessage.PaymentActivation(type));
      } else {
        return null;
      }
    }
  }

  public static long getMessageId(@Nullable byte[] serializedData) {
    JsonJobData data = JsonJobData.deserialize(serializedData);
    return data.getLong(KEY_MESSAGE_ID);
  }

  private static class MetricEventListener implements ZonaRosaServiceMessageSender.IndividualSendEvents {
    private final long messageId;

    private MetricEventListener(long messageId) {
      this.messageId = messageId;
    }

    @Override
    public void onMessageEncrypted() {
      ZonaRosaLocalMetrics.IndividualMessageSend.onMessageEncrypted(messageId);
    }

    @Override
    public void onMessageSent() {
      ZonaRosaLocalMetrics.IndividualMessageSend.onMessageSent(messageId);
    }

    @Override
    public void onSyncMessageSent() {
      ZonaRosaLocalMetrics.IndividualMessageSend.onSyncMessageSent(messageId);
    }
  }

  public static final class Factory implements Job.Factory<IndividualSendJob> {
    @Override
    public @NonNull IndividualSendJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);
      return new IndividualSendJob(parameters, data.getLong(KEY_MESSAGE_ID));
    }
  }
}

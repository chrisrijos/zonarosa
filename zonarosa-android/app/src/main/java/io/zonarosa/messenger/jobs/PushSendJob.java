/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.annimon.stream.Stream;

import org.greenrobot.eventbus.EventBus;
import io.zonarosa.core.util.Hex;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.metadata.certificate.InvalidCertificateException;
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialPresentation;
import io.zonarosa.blurhash.BlurHash;
import io.zonarosa.messenger.ZonaRosaExpiredException;
import io.zonarosa.messenger.attachments.Attachment;
import io.zonarosa.messenger.attachments.AttachmentId;
import io.zonarosa.messenger.attachments.DatabaseAttachment;
import io.zonarosa.messenger.contactshare.Contact;
import io.zonarosa.messenger.contactshare.ContactModelMapper;
import io.zonarosa.messenger.crypto.ProfileKeyUtil;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.NoSuchMessageException;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.Mention;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.database.model.MmsMessageRecord;
import io.zonarosa.messenger.database.model.ParentStoryId;
import io.zonarosa.messenger.database.model.StickerRecord;
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList;
import io.zonarosa.messenger.database.model.databaseprotos.GiftBadge;
import io.zonarosa.messenger.database.model.databaseprotos.PinnedMessage;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.events.PartProgressEvent;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobmanager.JobTracker;
import io.zonarosa.messenger.jobmanager.impl.BackoffUtil;
import io.zonarosa.messenger.keyvalue.CertificateType;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.linkpreview.LinkPreview;
import io.zonarosa.messenger.mms.OutgoingMessage;
import io.zonarosa.messenger.mms.PartAuthority;
import io.zonarosa.messenger.mms.QuoteModel;
import io.zonarosa.messenger.net.NotPushRegisteredException;
import io.zonarosa.messenger.notifications.v2.ConversationId;
import io.zonarosa.messenger.polls.Poll;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.messenger.transport.UndeliverableMessageException;
import io.zonarosa.core.util.Base64;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.messenger.util.MediaUtil;
import io.zonarosa.core.util.Util;
import io.zonarosa.service.api.messages.AttachmentTransferProgress;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer;
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId;
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.messages.ZonaRosaServicePreview;
import io.zonarosa.service.api.messages.shared.SharedContact;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;
import io.zonarosa.service.api.push.exceptions.ProofRequiredException;
import io.zonarosa.service.api.push.exceptions.RateLimitException;
import io.zonarosa.service.api.push.exceptions.ServerRejectedException;
import io.zonarosa.service.internal.push.BodyRange;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public abstract class PushSendJob extends SendJob {

  private static final String TAG                           = Log.tag(PushSendJob.class);
  private static final long   CERTIFICATE_EXPIRATION_BUFFER = TimeUnit.DAYS.toMillis(1);
  private static final long   PUSH_CHALLENGE_TIMEOUT        = TimeUnit.SECONDS.toMillis(10);

  protected PushSendJob(Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  protected final void onSend() throws Exception {
    long timeSinceAciSignedPreKeyRotation = System.currentTimeMillis() - ZonaRosaStore.account().aciPreKeys().getLastSignedPreKeyRotationTime();
    long timeSincePniSignedPreKeyRotation = System.currentTimeMillis() - ZonaRosaStore.account().pniPreKeys().getLastSignedPreKeyRotationTime();

    if (timeSinceAciSignedPreKeyRotation > PreKeysSyncJob.MAXIMUM_ALLOWED_SIGNED_PREKEY_AGE ||
        timeSinceAciSignedPreKeyRotation < 0 ||
        timeSincePniSignedPreKeyRotation > PreKeysSyncJob.MAXIMUM_ALLOWED_SIGNED_PREKEY_AGE ||
        timeSincePniSignedPreKeyRotation < 0
    ) {
      warn(TAG, "It's been too long since rotating our signed prekeys (ACI: " + timeSinceAciSignedPreKeyRotation + " ms, PNI: " + timeSincePniSignedPreKeyRotation + " ms)! Attempting to rotate now.");

      Optional<JobTracker.JobState> state = AppDependencies.getJobManager().runSynchronously(PreKeysSyncJob.create(), TimeUnit.SECONDS.toMillis(30));

      if (state.isPresent() && state.get() == JobTracker.JobState.SUCCESS) {
        log(TAG, "Successfully refreshed prekeys. Continuing.");
      } else {
        throw new RetryLaterException(new ZonaRosaExpiredException("Failed to refresh prekeys! State: " + (state.isEmpty() ? "<empty>" : state.get())));
      }
    }

    if (!Recipient.self().isRegistered()) {
      throw new NotPushRegisteredException();
    }

    onPushSend();

    if (ZonaRosaStore.rateLimit().needsRecaptcha()) {
      Log.i(TAG, "Successfully sent message. Assuming reCAPTCHA no longer needed.");
      ZonaRosaStore.rateLimit().onProofAccepted();
    }
  }

  @Override
  public void onRetry() {
    Log.i(TAG, "onRetry()");

    if (getRunAttempt() > 1) {
      Log.i(TAG, "Scheduling service outage detection job.");
      AppDependencies.getJobManager().add(new ServiceOutageDetectionJob());
    }
  }

  @Override
  protected boolean shouldTrace() {
    return true;
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof ServerRejectedException) {
      return false;
    }

    if (exception instanceof NotPushRegisteredException) {
      return false;
    }

    return exception instanceof IOException         ||
           exception instanceof RetryLaterException ||
           exception instanceof ProofRequiredException;
  }

  @Override
  public long getNextRunAttemptBackoff(int pastAttemptCount, @NonNull Exception exception) {
    if (exception instanceof ProofRequiredException) {
      long backoff = ((ProofRequiredException) exception).getRetryAfterSeconds();
      warn(TAG, "[Proof Required] Retry-After is " + backoff + " seconds.");
      if (backoff >= 0) {
        return TimeUnit.SECONDS.toMillis(backoff);
      }
    } else if (exception instanceof RateLimitException) {
      long backoff = ((RateLimitException) exception).getRetryAfterMilliseconds().orElse(-1L);
      if (backoff >= 0) {
        return backoff;
      }
    } else if (exception instanceof NonSuccessfulResponseCodeException) {
      if (((NonSuccessfulResponseCodeException) exception).is5xx()) {
        return BackoffUtil.exponentialBackoff(pastAttemptCount, RemoteConfig.getServerErrorMaxBackoff());
      }
    } else if (exception instanceof RetryLaterException) {
      long backoff = ((RetryLaterException) exception).getBackoff();
      if (backoff >= 0) {
        return backoff;
      }
    }

    return super.getNextRunAttemptBackoff(pastAttemptCount, exception);
  }

  protected Optional<byte[]> getProfileKey(@NonNull Recipient recipient) {
    if (!recipient.resolve().isSystemContact() && !recipient.resolve().isProfileSharing()) {
      return Optional.empty();
    }

    return Optional.of(ProfileKeyUtil.getSelfProfileKey().serialize());
  }

  protected ZonaRosaServiceAttachment getAttachmentFor(Contact.Avatar avatar) {
    Attachment attachment = avatar.getAttachment();

    try {
      if (attachment.getUri() == null || attachment.size == 0) throw new IOException("Assertion failed, outgoing attachment has no data!");
      InputStream is = PartAuthority.getAttachmentStream(context, attachment.getUri());
      return ZonaRosaServiceAttachment.newStreamBuilder()
                                    .withStream(is)
                                    .withContentType(attachment.contentType)
                                    .withLength(attachment.size)
                                    .withFileName(attachment.fileName)
                                    .withVoiceNote(attachment.voiceNote)
                                    .withBorderless(attachment.borderless)
                                    .withGif(attachment.videoGif)
                                    .withFaststart(attachment.transformProperties.mp4FastStart)
                                    .withWidth(attachment.width)
                                    .withHeight(attachment.height)
                                    .withCaption(attachment.caption)
                                    .withUuid(attachment.uuid)
                                    .withResumableUploadSpec(AppDependencies.getZonaRosaServiceMessageSender().getResumableUploadSpec())
                                    .withListener(new ZonaRosaServiceAttachment.ProgressListener() {
                                      @Override
                                      public void onAttachmentProgress(@NonNull AttachmentTransferProgress progress) {
                                        EventBus.getDefault().postSticky(new PartProgressEvent(attachment, PartProgressEvent.Type.NETWORK, progress));
                                      }

                                      @Override
                                      public boolean shouldCancel() {
                                        return isCanceled();
                                      }
                                    })
                                    .build();
    } catch (IOException ioe) {
      Log.w(TAG, "Couldn't open attachment", ioe);
    }
    return null;
  }

  protected static Set<String> enqueueCompressingAndUploadAttachmentsChains(@NonNull JobManager jobManager, OutgoingMessage message) {
    List<Attachment> attachments = new LinkedList<>();

    attachments.addAll(message.getAttachments());

    attachments.addAll(Stream.of(message.getLinkPreviews())
                             .map(LinkPreview::getThumbnail)
                             .filter(Optional::isPresent)
                             .map(Optional::get)
                             .toList());

    attachments.addAll(Stream.of(message.getSharedContacts())
                             .map(Contact::getAvatar).withoutNulls()
                             .map(Contact.Avatar::getAttachment).withoutNulls()
                             .toList());

    HashSet<String> jobs = new HashSet<>(Stream.of(attachments).map(a -> {
                                 final AttachmentId attachmentId = ((DatabaseAttachment) a).attachmentId;
                                 Log.d(TAG, "Enqueueing job chain to upload " + attachmentId);
                                 AttachmentUploadJob attachmentUploadJob = new AttachmentUploadJob(attachmentId);

                                 jobManager.startChain(AttachmentCompressionJob.fromAttachment((DatabaseAttachment) a, false, -1))
                                           .then(attachmentUploadJob)
                                           .enqueue();

                                 return attachmentUploadJob.getId();
                               })
                               .toList());

    if (message.getOutgoingQuote() != null && message.getOutgoingQuote().getAttachment() != null) {
      AttachmentId attachmentId = ((DatabaseAttachment) message.getOutgoingQuote().getAttachment()).attachmentId;

      if (ZonaRosaDatabase.attachments().hasData(attachmentId)) {
        AttachmentUploadJob quoteUploadJob = new AttachmentUploadJob(attachmentId);
        jobManager.add(quoteUploadJob);
        jobs.add(quoteUploadJob.getId());
      }
    }

    return jobs;
  }

  protected @NonNull List<ZonaRosaServiceAttachment> getAttachmentPointersFor(List<Attachment> attachments) {
    return Stream.of(attachments).map(this::getAttachmentPointerFor).filter(a -> a != null).toList();
  }

  protected @Nullable ZonaRosaServiceAttachment getAttachmentPointerFor(Attachment attachment) {
    if (TextUtils.isEmpty(attachment.remoteLocation)) {
      Log.w(TAG, "empty content id");
      return null;
    }

    if (TextUtils.isEmpty(attachment.remoteKey)) {
      Log.w(TAG, "empty encrypted key");
      return null;
    }

    try {
      final ZonaRosaServiceAttachmentRemoteId remoteId = ZonaRosaServiceAttachmentRemoteId.from(attachment.remoteLocation);
      final byte[]                          key      = Base64.decode(attachment.remoteKey);

      int width  = attachment.width;
      int height = attachment.height;

      if ((width == 0 || height == 0) && MediaUtil.hasVideoThumbnail(context, attachment.getUri())) {
        Bitmap thumbnail = MediaUtil.getVideoThumbnail(context, attachment.getUri(), 1000);

        if (thumbnail != null) {
          width  = thumbnail.getWidth();
          height = thumbnail.getHeight();
        }
      }

      return new ZonaRosaServiceAttachmentPointer(attachment.cdn.getCdnNumber(),
                                                remoteId,
                                                attachment.contentType,
                                                key,
                                                Optional.of(Util.toIntExact(attachment.size)),
                                                Optional.empty(),
                                                width,
                                                height,
                                                Optional.ofNullable(attachment.remoteDigest),
                                                Optional.ofNullable(attachment.getIncrementalDigest()),
                                                attachment.incrementalMacChunkSize,
                                                Optional.ofNullable(attachment.fileName),
                                                attachment.voiceNote,
                                                attachment.borderless,
                                                attachment.videoGif,
                                                Optional.ofNullable(attachment.caption),
                                                Optional.ofNullable(attachment.blurHash).map(BlurHash::getHash),
                                                attachment.uploadTimestamp,
                                                attachment.uuid);
    } catch (IOException | ArithmeticException e) {
      Log.w(TAG, e);
      return null;
    }
  }

  protected static void notifyMediaMessageDeliveryFailed(Context context, long messageId) {
    long                     threadId           = ZonaRosaDatabase.messages().getThreadIdForMessage(messageId);
    Recipient                recipient          = ZonaRosaDatabase.threads().getRecipientForThreadId(threadId);
    ParentStoryId.GroupReply groupReplyStoryId  = ZonaRosaDatabase.messages().getParentStoryIdForGroupReply(messageId);

    boolean isStory = false;
    try {
      MessageRecord record = ZonaRosaDatabase.messages().getMessageRecord(messageId);
      if (record instanceof MmsMessageRecord) {
        isStory = (((MmsMessageRecord) record).getStoryType().isStory());
      }
    } catch (NoSuchMessageException e) {
      Log.e(TAG, e);
    }

    if (threadId != -1 && recipient != null) {
      if (isStory) {
        ZonaRosaDatabase.messages().markAsNotNotified(messageId);
        AppDependencies.getMessageNotifier().notifyStoryDeliveryFailed(context, recipient, ConversationId.forConversation(threadId));
      } else {
        AppDependencies.getMessageNotifier().notifyMessageDeliveryFailed(context, recipient, ConversationId.fromThreadAndReply(threadId, groupReplyStoryId));
      }
    }
  }

  protected Optional<ZonaRosaServiceDataMessage.Quote> getQuoteFor(OutgoingMessage message) throws IOException {
    if (message.getOutgoingQuote() == null) return Optional.empty();
    if (message.isMessageEdit()) {
      return Optional.of(new ZonaRosaServiceDataMessage.Quote(0, ACI.UNKNOWN, "", null, null, ZonaRosaServiceDataMessage.Quote.Type.NORMAL, null));
    }

    long                                                  quoteId              = message.getOutgoingQuote().getId();
    String                                                quoteBody            = message.getOutgoingQuote().getText();
    RecipientId                                           quoteAuthor          = message.getOutgoingQuote().getAuthor();
    List<ZonaRosaServiceDataMessage.Mention>                quoteMentions        = getMentionsFor(message.getOutgoingQuote().getMentions());
    List<BodyRange>                                       bodyRanges           = getBodyRanges(message.getOutgoingQuote().getBodyRanges());
    QuoteModel.Type                                       quoteType            = message.getOutgoingQuote().getType();
    List<ZonaRosaServiceDataMessage.Quote.QuotedAttachment> quoteAttachments     = new LinkedList<>();
    Optional<Attachment>                                  localQuoteAttachment = Optional.ofNullable(message.getOutgoingQuote()).map(QuoteModel::getAttachment);

    if (localQuoteAttachment.isPresent() && MediaUtil.isViewOnceType(localQuoteAttachment.get().contentType)) {
      localQuoteAttachment = Optional.empty();
    }

    if (localQuoteAttachment.isPresent()) {
      Attachment              attachment             = localQuoteAttachment.get();
      ZonaRosaServiceAttachment quoteAttachmentPointer = getAttachmentPointerFor(localQuoteAttachment.get());

      quoteAttachments.add(new ZonaRosaServiceDataMessage.Quote.QuotedAttachment(attachment.quoteTargetContentType != null ? attachment.quoteTargetContentType : MediaUtil.IMAGE_JPEG,
                                                                               attachment.fileName,
                                                                               quoteAttachmentPointer));
    }

    Recipient quoteAuthorRecipient = Recipient.resolved(quoteAuthor);

    if (quoteAuthorRecipient.isMaybeRegistered()) {
      return Optional.of(new ZonaRosaServiceDataMessage.Quote(quoteId, RecipientUtil.getOrFetchServiceId(context, quoteAuthorRecipient), quoteBody, quoteAttachments, quoteMentions, quoteType.getDataMessageType(), bodyRanges));
    } else if (quoteAuthorRecipient.getHasServiceId()) {
      return Optional.of(new ZonaRosaServiceDataMessage.Quote(quoteId, quoteAuthorRecipient.requireAci(), quoteBody, quoteAttachments, quoteMentions, quoteType.getDataMessageType(), bodyRanges));
    } else {
      return Optional.empty();
    }
  }

  protected Optional<ZonaRosaServiceDataMessage.Sticker> getStickerFor(OutgoingMessage message) {
    Attachment stickerAttachment = Stream.of(message.getAttachments()).filter(Attachment::isSticker).findFirst().orElse(null);

    if (stickerAttachment == null) {
      return Optional.empty();
    }

    try {
      byte[]                  packId     = Hex.fromStringCondensed(stickerAttachment.stickerLocator.packId);
      byte[]                  packKey    = Hex.fromStringCondensed(stickerAttachment.stickerLocator.packKey);
      int                     stickerId  = stickerAttachment.stickerLocator.stickerId;
      StickerRecord           record     = ZonaRosaDatabase.stickers().getSticker(stickerAttachment.stickerLocator.packId, stickerId, false);
      String                  emoji      = record != null ? record.emoji : null;
      ZonaRosaServiceAttachment attachment = getAttachmentPointerFor(stickerAttachment);

      return Optional.of(new ZonaRosaServiceDataMessage.Sticker(packId, packKey, stickerId, emoji, attachment));
    } catch (IOException e) {
      Log.w(TAG, "Failed to decode sticker id/key", e);
      return Optional.empty();
    }
  }

  protected Optional<ZonaRosaServiceDataMessage.Reaction> getStoryReactionFor(@NonNull OutgoingMessage message, @NonNull ZonaRosaServiceDataMessage.StoryContext storyContext) {
    if (message.isStoryReaction()) {
      return Optional.of(new ZonaRosaServiceDataMessage.Reaction(message.getBody(),
                                                               false,
                                                               storyContext.getAuthorServiceId(),
                                                               storyContext.getSentTimestamp()));
    } else {
      return Optional.empty();
    }
  }

  List<SharedContact> getSharedContactsFor(OutgoingMessage mediaMessage) {
    List<SharedContact> sharedContacts = new LinkedList<>();

    for (Contact contact : mediaMessage.getSharedContacts()) {
      SharedContact.Builder builder = ContactModelMapper.localToRemoteBuilder(contact);
      SharedContact.Avatar  avatar  = null;

      if (contact.getAvatar() != null && contact.getAvatar().getAttachment() != null) {
        ZonaRosaServiceAttachment attachment = getAttachmentPointerFor(contact.getAvatar().getAttachment());
        if (attachment == null) {
          attachment = getAttachmentFor(contact.getAvatar());
        }
        avatar = SharedContact.Avatar.newBuilder().withAttachment(attachment)
                                                  .withProfileFlag(contact.getAvatar().isProfile())
                                                  .build();
      }

      builder.setAvatar(avatar);
      sharedContacts.add(builder.build());
    }

    return sharedContacts;
  }

  List<ZonaRosaServicePreview> getPreviewsFor(OutgoingMessage mediaMessage) {
    return Stream.of(mediaMessage.getLinkPreviews()).map(lp -> {
      ZonaRosaServiceAttachment attachment = lp.getThumbnail().isPresent() ? getAttachmentPointerFor(lp.getThumbnail().get()) : null;
      return new ZonaRosaServicePreview(lp.getUrl(), lp.getTitle(), lp.getDescription(), lp.getDate(), Optional.ofNullable(attachment));
    }).toList();
  }

  List<ZonaRosaServiceDataMessage.Mention> getMentionsFor(@NonNull List<Mention> mentions) {
    return Stream.of(mentions)
                 .map(m -> new ZonaRosaServiceDataMessage.Mention(Recipient.resolved(m.getRecipientId()).requireAci(), m.getStart(), m.getLength()))
                 .toList();
  }

  @Nullable ZonaRosaServiceDataMessage.GiftBadge getGiftBadgeFor(@NonNull OutgoingMessage message) throws UndeliverableMessageException {
    GiftBadge giftBadge = message.getGiftBadge();
    if (giftBadge == null) {
      return null;
    }

    try {
      ReceiptCredentialPresentation presentation = new ReceiptCredentialPresentation(giftBadge.redemptionToken.toByteArray());

      return new ZonaRosaServiceDataMessage.GiftBadge(presentation);
    } catch (InvalidInputException invalidInputException) {
      throw new UndeliverableMessageException(invalidInputException);
    }
  }

  protected @Nullable List<BodyRange> getBodyRanges(@NonNull OutgoingMessage message) {
    return getBodyRanges(message.getBodyRanges());
  }

  protected @Nullable ZonaRosaServiceDataMessage.PollCreate getPollCreate(OutgoingMessage message) {
    Poll poll = message.getPoll();
    if (poll == null) {
      return null;
    }

    return new ZonaRosaServiceDataMessage.PollCreate(poll.getQuestion(), poll.getAllowMultipleVotes(), poll.getPollOptions());
  }

  protected @Nullable ZonaRosaServiceDataMessage.PollTerminate getPollTerminate(OutgoingMessage message) {
    if (message.getMessageExtras() == null || message.getMessageExtras().pollTerminate == null) {
      return null;
    }

    return new ZonaRosaServiceDataMessage.PollTerminate(message.getMessageExtras().pollTerminate.targetTimestamp);
  }

  protected @Nullable List<BodyRange> getBodyRanges(@Nullable BodyRangeList bodyRanges) {
    if (bodyRanges == null || bodyRanges.ranges.size() == 0) {
      return null;
    }

    return bodyRanges
        .ranges
        .stream()
        .map(range -> {
          BodyRange.Builder builder = new BodyRange.Builder().start(range.start).length(range.length);

          if (range.style != null) {
            switch (range.style) {
              case BOLD:
                builder.style(BodyRange.Style.BOLD);
                break;
              case ITALIC:
                builder.style(BodyRange.Style.ITALIC);
                break;
              case SPOILER:
                builder.style(BodyRange.Style.SPOILER);
                break;
              case STRIKETHROUGH:
                builder.style(BodyRange.Style.STRIKETHROUGH);
                break;
              case MONOSPACE:
                builder.style(BodyRange.Style.MONOSPACE);
                break;
              default:
                throw new IllegalArgumentException("Unrecognized style");
            }
          } else {
            throw new IllegalArgumentException("Only supports style");
          }

          return builder.build();
        }).collect(Collectors.toList());
  }

  protected @Nullable ZonaRosaServiceDataMessage.PinnedMessage getPinnedMessage(OutgoingMessage message) {
    if (message.getMessageExtras() == null || message.getMessageExtras().pinnedMessage == null || ACI.parseOrNull(message.getMessageExtras().pinnedMessage.targetAuthorAci) == null) {
      return null;
    }

    PinnedMessage pinnedMessage = message.getMessageExtras().pinnedMessage;
    if (pinnedMessage.pinDurationInSeconds == MessageTable.PIN_FOREVER) {
      return new ZonaRosaServiceDataMessage.PinnedMessage(ACI.parseOrNull(pinnedMessage.targetAuthorAci), pinnedMessage.targetTimestamp, null, true);
    } else {
      return new ZonaRosaServiceDataMessage.PinnedMessage(ACI.parseOrNull(pinnedMessage.targetAuthorAci), pinnedMessage.targetTimestamp, (int) pinnedMessage.pinDurationInSeconds, null);
    }
  }

  protected void rotateSenderCertificateIfNecessary() throws IOException {
    try {
      Collection<CertificateType> requiredCertificateTypes = ZonaRosaStore.phoneNumberPrivacy()
                                                                        .getRequiredCertificateTypes();

      Log.i(TAG, "Ensuring we have these certificates " + requiredCertificateTypes);

      for (CertificateType certificateType : requiredCertificateTypes) {

        byte[] certificateBytes = ZonaRosaStore.certificate()
                                             .getUnidentifiedAccessCertificate(certificateType);

        if (certificateBytes == null) {
          throw new InvalidCertificateException(String.format("No certificate %s was present.", certificateType));
        }

        SenderCertificate certificate = new SenderCertificate(certificateBytes);

        if (System.currentTimeMillis() > (certificate.getExpiration() - CERTIFICATE_EXPIRATION_BUFFER)) {
          throw new InvalidCertificateException(String.format(Locale.US, "Certificate %s is expired, or close to it. Expires on: %d, currently: %d", certificateType, certificate.getExpiration(), System.currentTimeMillis()));
        }
        Log.d(TAG, String.format("Certificate %s is valid", certificateType));
      }

      Log.d(TAG, "All certificates are valid.");
    } catch (InvalidCertificateException e) {
      Log.w(TAG, "A certificate was invalid at send time. Fetching new ones.", e);
      if (!AppDependencies.getJobManager().runSynchronously(new RotateCertificateJob(), 5000).isPresent()) {
        throw new IOException("Timeout rotating certificate");
      }
    }
  }

  protected abstract void onPushSend() throws Exception;

}

package io.zonarosa.messenger.conversation;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import io.zonarosa.core.util.StreamUtil;
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.MessageTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.ThreadTable;
import io.zonarosa.messenger.database.model.GroupRecord;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.MultiDeviceViewedUpdateJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.mms.PartAuthority;
import io.zonarosa.messenger.mms.TextSlide;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientUtil;
import io.zonarosa.messenger.util.MessageRecordUtil;
import io.zonarosa.core.models.ServiceId;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ConversationRepository {

  private static final String TAG = Log.tag(ConversationRepository.class);

  private final Context  context;

  public ConversationRepository() {
    this.context = AppDependencies.getApplication();
  }

  @WorkerThread
  public @NonNull ConversationData getConversationData(long threadId, @NonNull Recipient conversationRecipient, int jumpToPosition) {
    ThreadTable.ConversationMetadata    metadata                       = ZonaRosaDatabase.threads().getConversationMetadata(threadId);
    int                                 threadSize                     = ZonaRosaDatabase.messages().getMessageCountForThread(threadId);
    long                                lastSeen                       = metadata.getLastSeen();
    int                                 lastSeenPosition               = 0;
    long                                lastScrolled                   = metadata.getLastScrolled();
    int                                 lastScrolledPosition           = 0;
    boolean                             isMessageRequestAccepted       = RecipientUtil.isMessageRequestAccepted(context, threadId);
    boolean                             isConversationHidden           = RecipientUtil.isRecipientHidden(threadId);
    ConversationData.MessageRequestData messageRequestData             = new ConversationData.MessageRequestData(isMessageRequestAccepted, isConversationHidden);
    boolean                             showUniversalExpireTimerUpdate = false;

    if (lastSeen > 0) {
      lastSeenPosition = ZonaRosaDatabase.messages().getMessagePositionByDateReceivedTimestamp(threadId, lastSeen, false);
    }

    if (lastSeenPosition <= 0) {
      lastSeen = 0;
    }

    if (lastSeen == 0 && lastScrolled > 0) {
      lastScrolledPosition = ZonaRosaDatabase.messages().getMessagePositionByDateReceivedTimestamp(threadId, lastScrolled, true);
    }

    if (!isMessageRequestAccepted) {
      boolean isGroup                             = false;
      boolean recipientIsKnownOrHasGroupsInCommon = false;
      if (conversationRecipient.isGroup()) {
        Optional<GroupRecord> group = ZonaRosaDatabase.groups().getGroup(conversationRecipient.getId());
        if (group.isPresent()) {
          List<Recipient> recipients = Recipient.resolvedList(group.get().getMembers());
          for (Recipient recipient : recipients) {
            if ((recipient.isProfileSharing() || recipient.getHasGroupsInCommon()) && !recipient.isSelf()) {
              recipientIsKnownOrHasGroupsInCommon = true;
              break;
            }
          }
        }
        isGroup = true;
      } else if (conversationRecipient.getHasGroupsInCommon()) {
        recipientIsKnownOrHasGroupsInCommon = true;
      }
      messageRequestData = new ConversationData.MessageRequestData(isMessageRequestAccepted, isConversationHidden, recipientIsKnownOrHasGroupsInCommon, isGroup);
    }

    List<ServiceId> groupMemberAcis;
    if (conversationRecipient.isPushV2Group()) {
      groupMemberAcis = conversationRecipient.getParticipantAcis();
    } else {
      groupMemberAcis = Collections.emptyList();
    }

    if (ZonaRosaStore.settings().getUniversalExpireTimer() != 0 &&
        conversationRecipient.getExpiresInSeconds() == 0 &&
        !conversationRecipient.isGroup() &&
        conversationRecipient.isRegistered() &&
        ZonaRosaDatabase.messages().canSetUniversalTimer(threadId))
    {
      showUniversalExpireTimerUpdate = true;
    }

    return new ConversationData(conversationRecipient, threadId, lastSeen, lastSeenPosition, lastScrolledPosition, jumpToPosition, threadSize, messageRequestData, showUniversalExpireTimerUpdate, metadata.getUnreadCount(), groupMemberAcis);
  }

  public void markGiftBadgeRevealed(long messageId) {
    ZonaRosaExecutors.BOUNDED_IO.execute(() -> {
      List<MessageTable.MarkedMessageInfo> markedMessageInfo = ZonaRosaDatabase.messages().setOutgoingGiftsRevealed(Collections.singletonList(messageId));
      if (!markedMessageInfo.isEmpty()) {
        Log.d(TAG, "Marked gift badge revealed. Sending view sync message.");
        MultiDeviceViewedUpdateJob.enqueue(
            markedMessageInfo.stream()
                             .map(MessageTable.MarkedMessageInfo::getSyncMessageId)
                             .collect(Collectors.toList()));
      }
    });
  }

  @NonNull
  public Single<ConversationMessage> resolveMessageToEdit(@NonNull ConversationMessage message) {
    return Single.fromCallable(() -> {
                   MessageRecord messageRecord = message.getMessageRecord();
                   if (MessageRecordUtil.hasTextSlide(messageRecord)) {
                     TextSlide textSlide = MessageRecordUtil.requireTextSlide(messageRecord);
                     if (textSlide.getUri() == null) {
                       return message;
                     }

                     try (InputStream stream = PartAuthority.getAttachmentStream(context, textSlide.getUri())) {
                       String body = StreamUtil.readFullyAsString(stream);
                       return ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, messageRecord, body, message.getThreadRecipient());
                     } catch (IOException e) {
                       Log.w(TAG, "Failed to read text slide data.");
                     }
                   }
                   return message;
                 }).subscribeOn(Schedulers.io())
                 .observeOn(AndroidSchedulers.mainThread());
  }
}

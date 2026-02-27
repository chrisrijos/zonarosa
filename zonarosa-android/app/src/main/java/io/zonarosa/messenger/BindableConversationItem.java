package io.zonarosa.messenger;

import android.net.Uri;
import android.view.GestureDetector;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.bumptech.glide.RequestManager;

import io.zonarosa.ringrtc.CallLinkRootKey;
import io.zonarosa.messenger.components.voice.VoiceNotePlaybackState;
import io.zonarosa.messenger.contactshare.Contact;
import io.zonarosa.messenger.conversation.ConversationItem;
import io.zonarosa.messenger.conversation.ConversationItemDisplayMode;
import io.zonarosa.messenger.conversation.ConversationMessage;
import io.zonarosa.messenger.conversation.colors.Colorizable;
import io.zonarosa.messenger.conversation.colors.Colorizer;
import io.zonarosa.messenger.conversation.mutiselect.MultiselectPart;
import io.zonarosa.messenger.conversation.mutiselect.Multiselectable;
import io.zonarosa.messenger.database.model.InMemoryMessageRecord;
import io.zonarosa.messenger.database.model.MessageRecord;
import io.zonarosa.messenger.database.model.MmsMessageRecord;
import io.zonarosa.messenger.giph.mp4.GiphyMp4Playable;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.groups.GroupMigrationMembershipChange;
import io.zonarosa.messenger.linkpreview.LinkPreview;
import io.zonarosa.messenger.mediapreview.MediaIntentFactory;
import io.zonarosa.messenger.polls.PollOption;
import io.zonarosa.messenger.polls.PollRecord;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.stickers.StickerLocator;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public interface BindableConversationItem extends Unbindable, GiphyMp4Playable, Colorizable, Multiselectable {
  void bind(@NonNull LifecycleOwner lifecycleOwner,
            @NonNull ConversationMessage messageRecord,
            @NonNull Optional<MessageRecord> previousMessageRecord,
            @NonNull Optional<MessageRecord> nextMessageRecord,
            @NonNull RequestManager requestManager,
            @NonNull Locale locale,
            @NonNull Set<MultiselectPart> batchSelected,
            @NonNull Recipient recipients,
            @Nullable String searchQuery,
            boolean pulseMention,
            boolean hasWallpaper,
            boolean isMessageRequestAccepted,
            boolean canPlayInline,
            @NonNull Colorizer colorizer,
            @NonNull ConversationItemDisplayMode displayMode);

  @NonNull ConversationMessage getConversationMessage();

  void setEventListener(@Nullable EventListener listener);

  default void setGestureDetector(@Nullable GestureDetector gestureDetector) {
    // Intentionally Blank.
  }

  default void setParentScrolling(boolean isParentScrolling) {
    // Intentionally Blank.
  }

  default void updateTimestamps() {
    // Intentionally Blank.
  }

  default void updateContactNameColor() {
    // Intentionally Blank.
  }

  default void updateSelectedState() {
    // Intentionally Blank.
  }

  interface EventListener {
    void onQuoteClicked(MmsMessageRecord messageRecord);
    void onLinkPreviewClicked(@NonNull LinkPreview linkPreview);
    void onQuotedIndicatorClicked(@NonNull MessageRecord messageRecord);
    void onMoreTextClicked(@NonNull RecipientId conversationRecipientId, long messageId, boolean isMms);
    void onStickerClicked(@NonNull StickerLocator stickerLocator);
    void onViewOnceMessageClicked(@NonNull MmsMessageRecord messageRecord);
    void onSharedContactDetailsClicked(@NonNull Contact contact, @NonNull View avatarTransitionView);
    void onAddToContactsClicked(@NonNull Contact contact);
    void onMessageSharedContactClicked(@NonNull List<Recipient> choices);
    void onInviteSharedContactClicked(@NonNull List<Recipient> choices);
    void onReactionClicked(@NonNull MultiselectPart multiselectPart, long messageId, boolean isMms);
    void onGroupMemberClicked(@NonNull RecipientId recipientId, @NonNull GroupId groupId);
    void onMessageWithErrorClicked(@NonNull MessageRecord messageRecord);
    void onMessageWithRecaptchaNeededClicked(@NonNull MessageRecord messageRecord);
    void onIncomingIdentityMismatchClicked(@NonNull RecipientId recipientId);
    void onRegisterVoiceNoteCallbacks(@NonNull Observer<VoiceNotePlaybackState> onPlaybackStartObserver);
    void onUnregisterVoiceNoteCallbacks(@NonNull Observer<VoiceNotePlaybackState> onPlaybackStartObserver);
    void onVoiceNotePause(@NonNull Uri uri);
    void onVoiceNotePlay(@NonNull Uri uri, long messageId, double position);

    default void onSingleVoiceNotePlay(@NonNull Uri uri, long messageId, double position) {
      onVoiceNotePlay(uri, messageId, position);
    }

    void onVoiceNoteSeekTo(@NonNull Uri uri, double position);
    void onVoiceNotePlaybackSpeedChanged(@NonNull Uri uri, float speed);
    void onGroupMigrationLearnMoreClicked(@NonNull GroupMigrationMembershipChange membershipChange);
    void onChatSessionRefreshLearnMoreClicked();
    void onBadDecryptLearnMoreClicked(@NonNull RecipientId author);
    void onSafetyNumberLearnMoreClicked(@NonNull Recipient recipient);
    void onJoinGroupCallClicked();
    void onInviteFriendsToGroupClicked(@NonNull GroupId.V2 groupId);
    void onEnableCallNotificationsClicked();
    void onPlayInlineContent(ConversationMessage conversationMessage);
    void onInMemoryMessageClicked(@NonNull InMemoryMessageRecord messageRecord);
    void onViewGroupDescriptionChange(@Nullable GroupId groupId, @NonNull String description, boolean isMessageRequestAccepted);
    void onChangeNumberUpdateContact(@NonNull Recipient recipient);
    void onChangeProfileNameUpdateContact(@NonNull Recipient recipient);
    void onCallToAction(@NonNull String action);
    void onDonateClicked();
    void onBlockJoinRequest(@NonNull Recipient recipient);
    void onRecipientNameClicked(@NonNull RecipientId target);
    void onInviteToZonaRosaClicked();
    void onActivatePaymentsClicked();
    void onSendPaymentClicked(@NonNull RecipientId recipientId);
    void onScheduledIndicatorClicked(@NonNull View view, @NonNull ConversationMessage conversationMessage);
    /** @return true if handled, false if you want to let the normal url handling continue */
    boolean onUrlClicked(@NonNull String url);
    void onViewGiftBadgeClicked(@NonNull MessageRecord messageRecord);
    void onGiftBadgeRevealed(@NonNull MessageRecord messageRecord);
    void goToMediaPreview(ConversationItem parent, View sharedElement, MediaIntentFactory.MediaPreviewArgs args);
    void onEditedIndicatorClicked(@NonNull ConversationMessage conversationMessage);
    void onShowGroupDescriptionClicked(@NonNull String groupName, @NonNull String description, boolean shouldLinkifyWebLinks);
    void onJoinCallLink(@NonNull CallLinkRootKey callLinkRootKey);
    void onShowSafetyTips(boolean forGroup);
    void onReportSpamLearnMoreClicked();
    void onMessageRequestAcceptOptionsClicked();
    void onItemDoubleClick(MultiselectPart multiselectPart);
    void onPaymentTombstoneClicked();
    void onDisplayMediaNoLongerAvailableSheet();
    void onShowUnverifiedProfileSheet(boolean forGroup);
    void onUpdateZonaRosaClicked();
    void onViewResultsClicked(long pollId);
    void onViewPollClicked(long messageId);
    void onToggleVote(@NonNull PollRecord poll, @NonNull PollOption pollOption, Boolean isChecked);
    void onViewPinnedMessage(long messageId);
  }
}

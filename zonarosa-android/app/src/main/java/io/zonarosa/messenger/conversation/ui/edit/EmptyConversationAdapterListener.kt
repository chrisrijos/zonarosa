/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.ui.edit

import android.net.Uri
import android.view.View
import androidx.lifecycle.Observer
import io.zonarosa.ringrtc.CallLinkRootKey
import io.zonarosa.messenger.components.voice.VoiceNotePlaybackState
import io.zonarosa.messenger.contactshare.Contact
import io.zonarosa.messenger.conversation.ConversationAdapter
import io.zonarosa.messenger.conversation.ConversationItem
import io.zonarosa.messenger.conversation.ConversationMessage
import io.zonarosa.messenger.conversation.mutiselect.MultiselectPart
import io.zonarosa.messenger.database.model.InMemoryMessageRecord
import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.groups.GroupMigrationMembershipChange
import io.zonarosa.messenger.linkpreview.LinkPreview
import io.zonarosa.messenger.mediapreview.MediaIntentFactory
import io.zonarosa.messenger.polls.PollOption
import io.zonarosa.messenger.polls.PollRecord
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.stickers.StickerLocator

/**
 * Empty object for when a callback can't be found.
 */
object EmptyConversationAdapterListener : ConversationAdapter.ItemClickListener {
  override fun onItemClick(item: MultiselectPart?) = Unit
  override fun onItemLongClick(itemView: View?, item: MultiselectPart?) = Unit
  override fun onQuoteClicked(messageRecord: MmsMessageRecord?) = Unit
  override fun onLinkPreviewClicked(linkPreview: LinkPreview) = Unit
  override fun onQuotedIndicatorClicked(messageRecord: MessageRecord) = Unit
  override fun onMoreTextClicked(conversationRecipientId: RecipientId, messageId: Long, isMms: Boolean) = Unit
  override fun onStickerClicked(stickerLocator: StickerLocator) = Unit
  override fun onViewOnceMessageClicked(messageRecord: MmsMessageRecord) = Unit
  override fun onSharedContactDetailsClicked(contact: Contact, avatarTransitionView: View) = Unit
  override fun onAddToContactsClicked(contact: Contact) = Unit
  override fun onMessageSharedContactClicked(choices: List<Recipient?>) = Unit
  override fun onInviteSharedContactClicked(choices: List<Recipient?>) = Unit
  override fun onReactionClicked(multiselectPart: MultiselectPart, messageId: Long, isMms: Boolean) = Unit
  override fun onGroupMemberClicked(recipientId: RecipientId, groupId: GroupId) = Unit
  override fun onMessageWithErrorClicked(messageRecord: MessageRecord) = Unit
  override fun onMessageWithRecaptchaNeededClicked(messageRecord: MessageRecord) = Unit
  override fun onIncomingIdentityMismatchClicked(recipientId: RecipientId) = Unit
  override fun onRegisterVoiceNoteCallbacks(onPlaybackStartObserver: Observer<VoiceNotePlaybackState?>) = Unit
  override fun onUnregisterVoiceNoteCallbacks(onPlaybackStartObserver: Observer<VoiceNotePlaybackState?>) = Unit
  override fun onVoiceNotePause(uri: Uri) = Unit
  override fun onVoiceNotePlay(uri: Uri, messageId: Long, position: Double) = Unit
  override fun onVoiceNoteSeekTo(uri: Uri, position: Double) = Unit
  override fun onVoiceNotePlaybackSpeedChanged(uri: Uri, speed: Float) = Unit
  override fun onGroupMigrationLearnMoreClicked(membershipChange: GroupMigrationMembershipChange) = Unit
  override fun onChatSessionRefreshLearnMoreClicked() = Unit
  override fun onBadDecryptLearnMoreClicked(author: RecipientId) = Unit
  override fun onSafetyNumberLearnMoreClicked(recipient: Recipient) = Unit
  override fun onJoinGroupCallClicked() = Unit
  override fun onInviteFriendsToGroupClicked(groupId: GroupId.V2) = Unit
  override fun onEnableCallNotificationsClicked() = Unit
  override fun onPlayInlineContent(conversationMessage: ConversationMessage?) = Unit
  override fun onInMemoryMessageClicked(messageRecord: InMemoryMessageRecord) = Unit
  override fun onViewGroupDescriptionChange(groupId: GroupId?, description: String, isMessageRequestAccepted: Boolean) = Unit
  override fun onChangeNumberUpdateContact(recipient: Recipient) = Unit
  override fun onChangeProfileNameUpdateContact(recipient: Recipient) = Unit
  override fun onCallToAction(action: String) = Unit
  override fun onDonateClicked() = Unit
  override fun onBlockJoinRequest(recipient: Recipient) = Unit
  override fun onRecipientNameClicked(target: RecipientId) = Unit
  override fun onInviteToZonaRosaClicked() = Unit
  override fun onActivatePaymentsClicked() = Unit
  override fun onSendPaymentClicked(recipientId: RecipientId) = Unit
  override fun onScheduledIndicatorClicked(view: View, conversationMessage: ConversationMessage) = Unit
  override fun onUrlClicked(url: String): Boolean = false
  override fun onViewGiftBadgeClicked(messageRecord: MessageRecord) = Unit
  override fun onGiftBadgeRevealed(messageRecord: MessageRecord) = Unit
  override fun goToMediaPreview(parent: ConversationItem?, sharedElement: View?, args: MediaIntentFactory.MediaPreviewArgs?) = Unit
  override fun onEditedIndicatorClicked(conversationMessage: ConversationMessage) = Unit
  override fun onShowGroupDescriptionClicked(groupName: String, description: String, shouldLinkifyWebLinks: Boolean) = Unit
  override fun onJoinCallLink(callLinkRootKey: CallLinkRootKey) = Unit
  override fun onShowSafetyTips(forGroup: Boolean) = Unit
  override fun onReportSpamLearnMoreClicked() = Unit
  override fun onMessageRequestAcceptOptionsClicked() = Unit
  override fun onItemDoubleClick(multiselectPart: MultiselectPart?) = Unit
  override fun onPaymentTombstoneClicked() = Unit
  override fun onDisplayMediaNoLongerAvailableSheet() = Unit
  override fun onShowUnverifiedProfileSheet(forGroup: Boolean) = Unit
  override fun onUpdateZonaRosaClicked() = Unit
  override fun onViewResultsClicked(pollId: Long) = Unit
  override fun onViewPollClicked(messageId: Long) = Unit
  override fun onToggleVote(poll: PollRecord, pollOption: PollOption, isChecked: Boolean?) = Unit
  override fun onViewPinnedMessage(messageId: Long) = Unit
}

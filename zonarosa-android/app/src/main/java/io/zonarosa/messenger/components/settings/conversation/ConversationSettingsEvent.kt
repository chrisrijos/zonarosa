package io.zonarosa.messenger.components.settings.conversation

import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.groups.SelectionLimits
import io.zonarosa.messenger.groups.ui.GroupChangeFailureReason
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

sealed class ConversationSettingsEvent {
  class AddToAGroup(
    val recipientId: RecipientId,
    val groupMembership: List<RecipientId>
  ) : ConversationSettingsEvent()

  class AddMembersToGroup(
    val groupId: GroupId,
    val selectionLimits: SelectionLimits,
    val groupMembersWithoutSelf: List<RecipientId>
  ) : ConversationSettingsEvent()

  object ShowGroupHardLimitDialog : ConversationSettingsEvent()

  class ShowAddMembersToGroupError(
    val failureReason: GroupChangeFailureReason
  ) : ConversationSettingsEvent()

  class ShowBlockGroupError(
    val failureReason: GroupChangeFailureReason
  ) : ConversationSettingsEvent()

  class ShowGroupInvitesSentDialog(
    val invitesSentTo: List<Recipient>
  ) : ConversationSettingsEvent()

  class ShowMembersAdded(
    val membersAddedCount: Int
  ) : ConversationSettingsEvent()
}

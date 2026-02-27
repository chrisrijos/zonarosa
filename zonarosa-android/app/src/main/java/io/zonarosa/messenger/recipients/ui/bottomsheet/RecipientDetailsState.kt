package io.zonarosa.messenger.recipients.ui.bottomsheet

import io.zonarosa.messenger.groups.memberlabel.StyledMemberLabel

data class RecipientDetailsState(
  val memberLabel: StyledMemberLabel?,
  val aboutText: String?
)

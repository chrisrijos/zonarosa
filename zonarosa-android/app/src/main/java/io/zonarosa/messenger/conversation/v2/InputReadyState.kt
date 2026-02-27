/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2

import io.zonarosa.messenger.database.GroupTable
import io.zonarosa.messenger.database.model.GroupRecord
import io.zonarosa.messenger.messagerequests.MessageRequestState
import io.zonarosa.messenger.recipients.Recipient

/**
 * Information necessary for rendering compose input.
 */
class InputReadyState(
  val conversationRecipient: Recipient,
  val messageRequestState: MessageRequestState,
  val groupRecord: GroupRecord?,
  val isClientExpired: Boolean,
  val isUnauthorized: Boolean,
  val threadContainsSms: Boolean
) {
  private val selfMemberLevel: GroupTable.MemberLevel? = groupRecord?.let {
    val level = it.memberLevel(Recipient.self())
    if (!it.isActive && level == GroupTable.MemberLevel.FULL_MEMBER) {
      GroupTable.MemberLevel.NOT_A_MEMBER
    }
    level
  }

  val isAnnouncementGroup: Boolean? = groupRecord?.isAnnouncementGroup
  val isActiveGroup: Boolean? = if (selfMemberLevel == null) null else selfMemberLevel != GroupTable.MemberLevel.NOT_A_MEMBER
  val isAdmin: Boolean? = selfMemberLevel?.equals(GroupTable.MemberLevel.ADMINISTRATOR)
  val isRequestingMember: Boolean? = selfMemberLevel?.equals(GroupTable.MemberLevel.REQUESTING_MEMBER)

  fun shouldShowInviteToZonaRosa(): Boolean {
    return !conversationRecipient.isPushGroup &&
      !conversationRecipient.isRegistered &&
      !conversationRecipient.isReleaseNotes
  }

  fun shouldClearDraft(): Boolean {
    return isActiveGroup == false ||
      isRequestingMember == true ||
      (isAnnouncementGroup == true && isAdmin == false) ||
      conversationRecipient.isReleaseNotes ||
      shouldShowInviteToZonaRosa()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as InputReadyState

    if (!conversationRecipient.hasSameContent(other.conversationRecipient)) return false
    if (messageRequestState != other.messageRequestState) return false
    if (groupRecord != other.groupRecord) return false
    if (isClientExpired != other.isClientExpired) return false
    if (isUnauthorized != other.isUnauthorized) return false

    return true
  }

  override fun hashCode(): Int {
    var result = conversationRecipient.hashCode()
    result = 31 * result + messageRequestState.hashCode()
    result = 31 * result + (groupRecord?.hashCode() ?: 0)
    result = 31 * result + isClientExpired.hashCode()
    result = 31 * result + isUnauthorized.hashCode()
    return result
  }
}

package io.zonarosa.messenger.keyvalue

import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.recipients.Recipient

data class StorySend(
  val timestamp: Long,
  val identifier: Identifier
) {
  companion object {
    @JvmStatic
    fun newSend(recipient: Recipient): StorySend {
      return if (recipient.isGroup) {
        StorySend(System.currentTimeMillis(), Identifier.Group(recipient.requireGroupId()))
      } else {
        StorySend(System.currentTimeMillis(), Identifier.DistributionList(recipient.requireDistributionListId()))
      }
    }
  }

  sealed class Identifier {
    data class Group(val groupId: GroupId) : Identifier() {
      override fun matches(recipient: Recipient) = recipient.groupId.orElse(null) == groupId
    }

    data class DistributionList(val distributionListId: DistributionListId) : Identifier() {
      override fun matches(recipient: Recipient) = recipient.distributionListId.orElse(null) == distributionListId
    }

    abstract fun matches(recipient: Recipient): Boolean
  }
}

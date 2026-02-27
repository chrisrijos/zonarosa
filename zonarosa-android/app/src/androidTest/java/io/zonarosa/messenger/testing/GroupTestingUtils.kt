package io.zonarosa.messenger.testing

import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey
import io.zonarosa.storageservice.storage.protos.groups.Member
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroup
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedMember
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.service.internal.push.GroupContextV2
import kotlin.random.Random

/**
 * Helper methods for creating groups for message processing tests et al.
 */
object GroupTestingUtils {
  fun member(aci: ACI, revision: Int = 0, role: Member.Role = Member.Role.ADMINISTRATOR, labelEmoji: String = "", labelString: String = ""): DecryptedMember {
    return DecryptedMember.Builder()
      .aciBytes(aci.toByteString())
      .joinedAtRevision(revision)
      .role(role)
      .labelEmoji(labelEmoji)
      .labelString(labelString)
      .build()
  }

  fun insertGroup(revision: Int = 0, vararg members: DecryptedMember): TestGroupInfo {
    val groupMasterKey = GroupMasterKey(Random.nextBytes(GroupMasterKey.SIZE))
    val decryptedGroupState = DecryptedGroup.Builder()
      .members(members.toList())
      .revision(revision)
      .title(MessageContentFuzzer.string())
      .build()

    val groupId = ZonaRosaDatabase.groups.create(groupMasterKey, decryptedGroupState, null)!!
    val groupRecipientId = ZonaRosaDatabase.recipients.getOrInsertFromGroupId(groupId)
    ZonaRosaDatabase.recipients.setProfileSharing(groupRecipientId, true)

    return TestGroupInfo(groupId, groupMasterKey, groupRecipientId)
  }

  fun RecipientId.asMember(): DecryptedMember {
    return Recipient.resolved(this).asMember()
  }

  fun Recipient.asMember(): DecryptedMember {
    return member(aci = requireAci())
  }

  data class TestGroupInfo(val groupId: GroupId.V2, val masterKey: GroupMasterKey, val recipientId: RecipientId) {
    val groupV2Context: GroupContextV2
      get() = GroupContextV2(masterKey = masterKey.serialize().toByteString(), revision = 0)
  }
}

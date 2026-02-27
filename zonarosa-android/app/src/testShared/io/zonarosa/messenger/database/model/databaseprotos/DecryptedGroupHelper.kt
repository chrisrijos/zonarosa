package io.zonarosa.messenger.database.model.databaseprotos

import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey
import io.zonarosa.storageservice.storage.protos.groups.Member
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroupChange
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedMember
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedPendingMember
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedRequestingMember
import io.zonarosa.service.internal.push.GroupContextV2
import java.util.UUID

fun groupContext(masterKey: GroupMasterKey, init: DecryptedGroupV2Context.Builder.() -> Unit): DecryptedGroupV2Context {
  val builder = DecryptedGroupV2Context.Builder()
  builder.context = encryptedGroupContext(masterKey)
  builder.init()
  return builder.build()
}

fun groupChange(editor: ServiceId, init: DecryptedGroupChange.Builder.() -> Unit): DecryptedGroupChange {
  val builder = DecryptedGroupChange.Builder()
  builder.editorServiceIdBytes = editor.toByteString()
  builder.init()
  return builder.build()
}

fun encryptedGroupContext(masterKey: GroupMasterKey): GroupContextV2 {
  return GroupContextV2.Builder().masterKey(masterKey.serialize().toByteString()).build()
}

fun DecryptedGroupChange.Builder.addRequestingMember(aci: ACI) {
  newRequestingMembers += requestingMember(aci)
}

fun DecryptedGroupChange.Builder.deleteRequestingMember(aci: ACI) {
  deleteRequestingMembers += aci.toByteString()
}

fun DecryptedGroupChange.Builder.addMember(aci: ACI) {
  newMembers += member(aci)
}

fun member(serviceId: UUID, role: Member.Role = Member.Role.DEFAULT, joinedAt: Int = 0, labelEmoji: String = "", labelString: String = ""): DecryptedMember {
  return member(ACI.from(serviceId), role, joinedAt, labelEmoji, labelString)
}

fun member(aci: ACI, role: Member.Role = Member.Role.DEFAULT, joinedAt: Int = 0, labelEmoji: String = "", labelString: String = ""): DecryptedMember {
  return DecryptedMember.Builder()
    .role(role)
    .aciBytes(aci.toByteString())
    .joinedAtRevision(joinedAt)
    .labelEmoji(labelEmoji)
    .labelString(labelString)
    .build()
}

fun requestingMember(serviceId: ServiceId): DecryptedRequestingMember {
  return DecryptedRequestingMember.Builder()
    .aciBytes(serviceId.toByteString())
    .build()
}

fun pendingMember(serviceId: ServiceId): DecryptedPendingMember {
  return DecryptedPendingMember.Builder()
    .serviceIdBytes(serviceId.toByteString())
    .build()
}

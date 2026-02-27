package io.zonarosa.messenger.service.webrtc

import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.ringrtc.CallManager
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.recipients.RecipientId

data class GroupCallRingCheckInfo(
  val recipientId: RecipientId,
  val groupId: GroupId.V2,
  val ringId: Long,
  val ringerAci: ACI,
  val ringUpdate: CallManager.RingUpdate
)

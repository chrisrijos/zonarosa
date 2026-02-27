/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.groupsv2

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendEndorsement
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendEndorsementsResponse
import java.time.Instant

/**
 * Group send endorsement data received from the server.
 */
data class ReceivedGroupSendEndorsements(
  val expirationMs: Long,
  val endorsements: Map<ServiceId.ACI, GroupSendEndorsement>
) {
  constructor(
    expiration: Instant,
    members: List<ServiceId.ACI>,
    receivedEndorsements: GroupSendEndorsementsResponse.ReceivedEndorsements
  ) : this(
    expirationMs = expiration.toEpochMilli(),
    endorsements = members.zip(receivedEndorsements.endorsements).toMap()
  )
}

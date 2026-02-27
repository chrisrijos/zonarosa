/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.groupsv2

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate
import io.zonarosa.libzonarosa.zkgroup.groups.GroupSecretParams
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendEndorsement
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendFullToken
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import java.time.Instant

/**
 * Helper container for all data needed to send with group send endorsements.
 */
data class GroupSendEndorsements(
  val expirationMs: Long,
  val endorsements: Map<ServiceId.ACI, GroupSendEndorsement>,
  val sealedSenderCertificate: SenderCertificate,
  val groupSecretParams: GroupSecretParams
) {

  private val expiration: Instant by lazy { Instant.ofEpochMilli(expirationMs) }
  private val combinedEndorsement: GroupSendEndorsement by lazy { GroupSendEndorsement.combine(endorsements.values) }

  fun serialize(): ByteArray {
    return combinedEndorsement.toFullToken(groupSecretParams, expiration).serialize()
  }

  fun forIndividuals(addresses: List<ZonaRosaServiceAddress>): List<GroupSendFullToken?> {
    return addresses
      .map { a -> endorsements[a.serviceId] }
      .map { e -> e?.toFullToken(groupSecretParams, expiration) }
  }
}

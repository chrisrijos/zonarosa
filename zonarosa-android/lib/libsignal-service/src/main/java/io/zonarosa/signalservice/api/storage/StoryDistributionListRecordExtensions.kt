/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import io.zonarosa.core.models.ServiceId
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import io.zonarosa.service.internal.storage.protos.StoryDistributionListRecord

val StoryDistributionListRecord.recipientServiceAddresses: List<ZonaRosaServiceAddress>
  get() {
    val serviceIds = if (this.recipientServiceIdsBinary.isNotEmpty()) {
      this.recipientServiceIdsBinary.mapNotNull { ServiceId.parseOrNull(it) }
    } else {
      this.recipientServiceIds.mapNotNull { ServiceId.parseOrNull(it) }
    }
    return serviceIds.map { ZonaRosaServiceAddress(it) }
  }

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import io.zonarosa.core.models.ServiceId
import io.zonarosa.service.internal.storage.protos.ContactRecord

val ContactRecord.zonarosaAci: ServiceId.ACI?
  get() = ServiceId.ACI.parseOrNull(this.aci, this.aciBinary)

val ContactRecord.zonarosaPni: ServiceId.PNI?
  get() = ServiceId.PNI.parseOrNull(this.pni, this.pniBinary)

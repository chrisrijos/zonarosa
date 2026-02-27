/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import io.zonarosa.core.models.ServiceId

class ACIParceler : Parceler<ServiceId.ACI> {
  override fun ServiceId.ACI.write(parcel: Parcel, flags: Int) {
    parcel.writeByteArray(this.toByteArray())
  }

  override fun create(parcel: Parcel): ServiceId.ACI {
    return ServiceId.ACI.parseOrThrow(parcel.createByteArray())
  }
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import io.zonarosa.core.models.AccountEntropyPool

object AccountEntropyPoolParceler : Parceler<AccountEntropyPool?> {
  override fun create(parcel: Parcel): AccountEntropyPool? {
    val aep = parcel.readString()
    return aep?.let { AccountEntropyPool(it) }
  }

  override fun AccountEntropyPool?.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this?.value)
  }
}

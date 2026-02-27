/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import io.zonarosa.core.models.MasterKey

object MasterKeyParceler : Parceler<MasterKey?> {
  override fun create(parcel: Parcel): MasterKey? {
    val bytes = parcel.createByteArray()
    return bytes?.let { MasterKey(it) }
  }

  override fun MasterKey?.write(parcel: Parcel, flags: Int) {
    parcel.writeByteArray(this?.serialize())
  }
}

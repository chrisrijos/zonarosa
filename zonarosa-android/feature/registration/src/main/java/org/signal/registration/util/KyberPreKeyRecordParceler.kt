/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord

class KyberPreKeyRecordParceler : Parceler<KyberPreKeyRecord> {
  override fun KyberPreKeyRecord.write(parcel: Parcel, flags: Int) {
    parcel.writeByteArray(this.serialize())
  }

  override fun create(parcel: Parcel): KyberPreKeyRecord {
    return KyberPreKeyRecord(parcel.createByteArray())
  }
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord

class SignedPreKeyRecordParceler : Parceler<SignedPreKeyRecord> {
  override fun SignedPreKeyRecord.write(parcel: Parcel, flags: Int) {
    parcel.writeByteArray(this.serialize())
  }

  override fun create(parcel: Parcel): SignedPreKeyRecord {
    return SignedPreKeyRecord(parcel.createByteArray())
  }
}

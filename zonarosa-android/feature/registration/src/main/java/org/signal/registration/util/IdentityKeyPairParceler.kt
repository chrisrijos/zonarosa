/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.util

import android.os.Parcel
import kotlinx.parcelize.Parceler
import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey

class IdentityKeyPairParceler : Parceler<IdentityKeyPair> {
  override fun IdentityKeyPair.write(parcel: Parcel, flags: Int) {
    parcel.writeByteArray(publicKey.serialize())
    parcel.writeByteArray(privateKey.serialize())
  }

  override fun create(parcel: Parcel): IdentityKeyPair {
    return IdentityKeyPair(
      IdentityKey(parcel.createByteArray()!!),
      ECPrivateKey(parcel.createByteArray()!!)
    )
  }
}

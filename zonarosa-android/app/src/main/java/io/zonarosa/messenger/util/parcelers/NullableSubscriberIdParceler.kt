/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.util.parcelers

import android.os.Parcel
import kotlinx.parcelize.Parceler
import io.zonarosa.service.api.subscriptions.SubscriberId

/**
 * Parceler for nullable SubscriberIds
 */
object NullableSubscriberIdParceler : Parceler<SubscriberId?> {
  override fun create(parcel: Parcel): SubscriberId? {
    return parcel.readString()?.let { SubscriberId.deserialize(it) }
  }

  override fun SubscriberId?.write(parcel: Parcel, flags: Int) {
    parcel.writeString(this?.serialize())
  }
}

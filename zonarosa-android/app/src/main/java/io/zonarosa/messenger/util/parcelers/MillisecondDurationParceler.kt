/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.util.parcelers

import android.os.Parcel
import kotlinx.parcelize.Parceler
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Parceler for non-null durations, storing them in milliseconds.
 */
object MillisecondDurationParceler : Parceler<Duration> {
  override fun create(parcel: Parcel): Duration {
    return parcel.readLong().milliseconds
  }

  override fun Duration.write(parcel: Parcel, flags: Int) {
    parcel.writeLong(inWholeMilliseconds)
  }
}

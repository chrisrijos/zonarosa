/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.mediasend

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Feature-module value type representing a recipient identifier.
 *
 * This avoids coupling to the app-layer `RecipientId` while making it clear we're passing
 * a recipient reference rather than an arbitrary Long.
 */
@Parcelize
@JvmInline
value class MediaRecipientId(val id: Long) : Parcelable {
  override fun toString(): String = "MediaRecipientId($id)"
}

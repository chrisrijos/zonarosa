/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.media

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import io.zonarosa.core.models.UriSerializer

/**
 * Represents a piece of media that the user has on their device.
 */
@Serializable
@Parcelize
data class Media(
  @Serializable(with = UriSerializer::class) val uri: Uri,
  val contentType: String?,
  val date: Long,
  val width: Int,
  val height: Int,
  val size: Long,
  val duration: Long,
  @get:JvmName("isBorderless") val isBorderless: Boolean,
  @get:JvmName("isVideoGif") val isVideoGif: Boolean,
  val bucketId: String?,
  val caption: String?,
  val transformProperties: TransformProperties?,
  var fileName: String?
) : Parcelable {
  companion object {
    const val ALL_MEDIA_BUCKET_ID: String = "io.zonarosa.messenger.ALL_MEDIA"
  }

  fun withMimeType(newMimeType: String) = copy(contentType = newMimeType)
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.mediasend.preupload

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.zonarosa.core.models.media.Media

/**
 * Handle returned from a successful pre-upload.
 *
 * This mirrors the legacy concept of:
 * - an attachment row identifier
 * - the upload dependency job ids
 * - the original media
 */
@Parcelize
data class PreUploadResult(
  val media: Media,
  val attachmentId: Long,
  val jobIds: List<String>
) : Parcelable {
  val uri: Uri
    get() = media.uri
}

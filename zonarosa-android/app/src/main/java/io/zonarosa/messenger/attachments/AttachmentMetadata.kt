/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.attachments

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

/**
 * Metadata for a specific attachment, specifically per data file. So there can be a
 * many-to-one relationship from attachments to metadata.
 */
@Parcelize
class AttachmentMetadata(
  val localBackupKey: @RawValue LocalBackupKey?
) : Parcelable

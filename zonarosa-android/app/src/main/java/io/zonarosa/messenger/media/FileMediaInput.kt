/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.media

import android.media.MediaExtractor
import io.zonarosa.messenger.video.interfaces.MediaInput
import java.io.File
import java.io.IOException

/**
 * A media input source that the system reads directly from the file.
 */
class FileMediaInput(private val file: File) : MediaInput {
  @Throws(IOException::class)
  override fun createExtractor(): MediaExtractor {
    val extractor = MediaExtractor()
    extractor.setDataSource(file.absolutePath)
    return extractor
  }

  override fun hasSameInput(other: MediaInput): Boolean {
    return other is FileMediaInput && other.file == this.file
  }

  override fun close() {}
}

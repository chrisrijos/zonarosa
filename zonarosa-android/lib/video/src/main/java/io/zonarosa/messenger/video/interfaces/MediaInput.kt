/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.video.interfaces

import android.media.MediaExtractor
import java.io.Closeable
import java.io.IOException

/**
 * Abstraction over the different sources of media input for transcoding.
 */
interface MediaInput : Closeable {
  @Throws(IOException::class)
  fun createExtractor(): MediaExtractor

  fun hasSameInput(other: MediaInput): Boolean
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.video.exceptions

/**
 * Exception to denote when video processing has had an issue with its source input.
 */
class VideoSourceException : Exception {
  internal constructor(message: String?) : super(message)
  internal constructor(message: String?, inner: Exception?) : super(message, inner)
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.attachments

/**
 * Thrown by jobs unable to rehydrate enough attachment information to download it.
 */
class InvalidAttachmentException : Exception {
  constructor(s: String?) : super(s)
  constructor(e: Exception?) : super(e)
}

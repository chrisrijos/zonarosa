/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.crypto

data class AttachmentDigest(
  val digest: ByteArray,
  val incrementalDigest: ByteArray?,
  val incrementalMacChunkSize: Int
)

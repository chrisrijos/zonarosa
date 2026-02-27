/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.backup

import io.zonarosa.core.util.CryptoUtil
import io.zonarosa.core.util.Hex

/**
 * Represent a media name for the various types of media that can be archived.
 */
@JvmInline
value class MediaName(val name: String) {

  companion object {
    fun fromPlaintextHashAndRemoteKey(plaintextHash: ByteArray, remoteKey: ByteArray) = MediaName(Hex.toStringCondensed(plaintextHash + remoteKey))
    fun fromPlaintextHashAndRemoteKeyForThumbnail(plaintextHash: ByteArray, remoteKey: ByteArray) = MediaName(Hex.toStringCondensed(plaintextHash + remoteKey) + "_thumbnail")
    fun forThumbnailFromMediaName(mediaName: String) = MediaName("${mediaName}_thumbnail")
    fun forLocalBackupFilename(plaintextHash: ByteArray, localKey: ByteArray) = MediaName(Hex.toStringCondensed(CryptoUtil.sha256(plaintextHash + localKey)))

    /**
     * For java, since it struggles with value classes.
     */
    @JvmStatic
    fun toMediaIdString(mediaName: String, mediaRootBackupKey: MediaRootBackupKey): String {
      return MediaName(mediaName).toMediaId(mediaRootBackupKey).encode()
    }
  }

  fun toMediaId(mediaRootBackupKey: MediaRootBackupKey): MediaId {
    return mediaRootBackupKey.deriveMediaId(this)
  }

  override fun toString(): String {
    return name
  }
}

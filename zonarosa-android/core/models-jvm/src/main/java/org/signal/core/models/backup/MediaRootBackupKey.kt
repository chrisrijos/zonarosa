/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.backup

import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.RandomUtil
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey

/**
 * Safe typing around a media root backup key, which is a 32-byte array.
 * This key is a purely random value.
 */
class MediaRootBackupKey(override val value: ByteArray) : BackupKey {

  companion object {
    fun generate(): MediaRootBackupKey {
      return MediaRootBackupKey(RandomUtil.getSecureBytes(32))
    }
  }

  /**
   * The private key used to generate anonymous credentials when interacting with the backup service.
   */
  override fun deriveAnonymousCredentialPrivateKey(aci: ServiceId.ACI): ECPrivateKey {
    return io.zonarosa.libzonarosa.messagebackup.BackupKey(value).deriveEcKey(aci.libZonaRosaAci)
  }

  fun deriveMediaId(mediaName: MediaName): MediaId {
    return MediaId(io.zonarosa.libzonarosa.messagebackup.BackupKey(value).deriveMediaId(mediaName.name))
  }

  fun deriveMediaSecrets(mediaName: MediaName): MediaKeyMaterial {
    val mediaId = deriveMediaId(mediaName)
    return deriveMediaSecrets(mediaId)
  }

  fun deriveMediaSecretsFromMediaId(base64MediaId: String): MediaKeyMaterial {
    return deriveMediaSecrets(MediaId(base64MediaId))
  }

  fun deriveThumbnailTransitKey(thumbnailMediaName: MediaName): ByteArray {
    return io.zonarosa.libzonarosa.messagebackup.BackupKey(value).deriveThumbnailTransitEncryptionKey(deriveMediaId(thumbnailMediaName).value)
  }

  private fun deriveMediaSecrets(mediaId: MediaId): MediaKeyMaterial {
    val libzonarosaBackupKey = io.zonarosa.libzonarosa.messagebackup.BackupKey(value)
    val combinedKey = libzonarosaBackupKey.deriveMediaEncryptionKey(mediaId.value)

    return MediaKeyMaterial(
      id = mediaId,
      macKey = combinedKey.copyOfRange(0, 32),
      aesKey = combinedKey.copyOfRange(32, 64)
    )
  }

  /**
   * Identifies a the location of a user's backup.
   */
  fun deriveBackupId(aci: ServiceId.ACI): BackupId {
    return BackupId(
      io.zonarosa.libzonarosa.messagebackup.BackupKey(value).deriveBackupId(aci.libZonaRosaAci)
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MediaRootBackupKey

    return value.contentEquals(other.value)
  }

  override fun hashCode(): Int {
    return value.contentHashCode()
  }

  class MediaKeyMaterial(
    val id: MediaId,
    val macKey: ByteArray,
    val aesKey: ByteArray
  )
}

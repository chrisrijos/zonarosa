/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.backup

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.messagebackup.BackupForwardSecrecyToken
import io.zonarosa.libzonarosa.messagebackup.MessageBackupKey
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey

private typealias LibZonaRosaBackupKey = io.zonarosa.libzonarosa.messagebackup.BackupKey

/**
 * Safe typing around a backup key, which is a 32-byte array.
 * This key is derived from the AEP.
 */
class MessageBackupKey(override val value: ByteArray) : BackupKey {
  init {
    require(value.size == 32) { "Backup key must be 32 bytes!" }
  }

  /**
   * The private key used to generate anonymous credentials when interacting with the backup service.
   */
  override fun deriveAnonymousCredentialPrivateKey(aci: ServiceId.ACI): ECPrivateKey {
    return LibZonaRosaBackupKey(value).deriveEcKey(aci.libZonaRosaAci)
  }

  /**
   * The cryptographic material used to encrypt a backup.
   *
   * @param forwardSecrecyToken Should be present for any backup located on the archive CDN. Absent for other uses (i.e. link+sync).
   */
  fun deriveBackupSecrets(aci: ServiceId.ACI, forwardSecrecyToken: BackupForwardSecrecyToken?): BackupKeyMaterial {
    val backupId = deriveBackupId(aci)
    val libzonarosaBackupKey = LibZonaRosaBackupKey(value)
    val libzonarosaMessageMessageBackupKey = MessageBackupKey(libzonarosaBackupKey, backupId.value, forwardSecrecyToken)

    return BackupKeyMaterial(
      id = backupId,
      macKey = libzonarosaMessageMessageBackupKey.hmacKey,
      aesKey = libzonarosaMessageMessageBackupKey.aesKey
    )
  }

  /**
   * Identifies a the location of a user's backup.
   */
  fun deriveBackupId(aci: ServiceId.ACI): BackupId {
    return BackupId(
      LibZonaRosaBackupKey(value).deriveBackupId(aci.libZonaRosaAci)
    )
  }

  /**
   * The AES key used to encrypt the backup id for local file backup metadata header.
   */
  fun deriveLocalBackupMetadataKey(): ByteArray {
    return LibZonaRosaBackupKey(value).deriveLocalBackupMetadataKey()
  }

  class BackupKeyMaterial(
    val id: BackupId,
    val macKey: ByteArray,
    val aesKey: ByteArray
  )
}

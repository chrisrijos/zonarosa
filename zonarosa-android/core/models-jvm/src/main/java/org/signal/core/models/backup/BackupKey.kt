/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.models.backup

import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey

/**
 * Contains the common properties for all "backup keys", namely the [MessageBackupKey] and [io.zonarosa.service.api.backup.MediaRootBackupKey]
 */
interface BackupKey {

  val value: ByteArray

  /**
   * The private key used to generate anonymous credentials when interacting with the backup service.
   */
  fun deriveAnonymousCredentialPrivateKey(aci: ServiceId.ACI): ECPrivateKey
}

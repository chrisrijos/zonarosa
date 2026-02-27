/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import io.zonarosa.core.models.backup.MediaRootBackupKey
import io.zonarosa.core.models.backup.MessageBackupKey

/**
 * A convenient container for passing around both a message and media archive service credential.
 */
data class ArchiveServiceAccessPair(
  val messageBackupAccess: ArchiveServiceAccess<MessageBackupKey>,
  val mediaBackupAccess: ArchiveServiceAccess<MediaRootBackupKey>
)

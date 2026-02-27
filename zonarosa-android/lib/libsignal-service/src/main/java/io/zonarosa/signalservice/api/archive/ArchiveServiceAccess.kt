/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import io.zonarosa.core.models.backup.BackupKey

/**
 * Key and credential combo needed to perform backup operations on the server.
 */
class ArchiveServiceAccess<T : BackupKey>(
  val credential: ArchiveServiceCredential,
  val backupKey: T
)

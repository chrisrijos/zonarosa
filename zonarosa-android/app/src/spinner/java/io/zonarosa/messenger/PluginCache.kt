/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger

import android.net.Uri
import io.zonarosa.messenger.backup.v2.local.ArchiveFileSystem
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

object PluginCache {
  private var archiveFileSystem: ArchiveFileSystem? = null
  var localBackups: ApiPlugin.LocalBackups? = null

  fun getArchiveFileSystem(): ArchiveFileSystem? {
    if (archiveFileSystem == null) {
      val backupDirectoryUri = ZonaRosaStore.backup.newLocalBackupsDirectory?.let { Uri.parse(it) }
      if (backupDirectoryUri == null || backupDirectoryUri.path == null) {
        return null
      }

      archiveFileSystem = ArchiveFileSystem.fromUri(AppDependencies.application, backupDirectoryUri)
    }
    return archiveFileSystem
  }

  fun clearBackupCache() {
    archiveFileSystem = null
    localBackups = null
  }
}

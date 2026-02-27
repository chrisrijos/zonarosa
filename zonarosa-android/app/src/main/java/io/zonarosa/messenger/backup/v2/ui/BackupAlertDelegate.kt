/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.protos.BackupDownloadNotifierState

/**
 * Delegate that controls whether and which backup alert sheet is displayed.
 */
object BackupAlertDelegate {

  private const val FRAGMENT_TAG = "BackupAlertFragmentTag"
  private val TAG = Log.tag(BackupAlertDelegate::class)

  @JvmStatic
  fun delegate(fragmentManager: FragmentManager, lifecycle: Lifecycle) {
    lifecycle.coroutineScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        if (BackupRepository.shouldDisplayBackupFailedSheet()) {
          Log.d(TAG, "Displaying BackupFailed sheet.")
          BackupAlertBottomSheet.create(BackupAlert.BackupFailed).show(fragmentManager, FRAGMENT_TAG)
        } else if (BackupRepository.shouldDisplayCouldNotCompleteBackupSheet()) {
          Log.d(TAG, "Displaying CouldNotCompleteBackup sheet.")
          BackupAlertBottomSheet.create(BackupAlert.CouldNotCompleteBackup(daysSinceLastBackup = ZonaRosaStore.backup.daysSinceLastBackup)).show(fragmentManager, FRAGMENT_TAG)
        } else if (BackupRepository.shouldDisplayBackupExpiredAndDowngradedSheet()) {
          Log.d(TAG, "Displaying ExpiredAndDowngraded sheet.")
          BackupAlertBottomSheet.create(BackupAlert.ExpiredAndDowngraded).show(fragmentManager, FRAGMENT_TAG)
        } else if (BackupRepository.shouldDisplayOutOfRemoteStorageSpaceSheet()) {
          Log.d(TAG, "Displaying NoRemoteStorageSpaceAvailableBottomSheet.")
          NoRemoteStorageSpaceAvailableBottomSheet().show(fragmentManager, FRAGMENT_TAG)
        }

        displayBackupDownloadNotifier(fragmentManager)
      }
    }
  }

  private suspend fun displayBackupDownloadNotifier(fragmentManager: FragmentManager) {
    val downloadYourBackupToday = withContext(ZonaRosaDispatchers.IO) { BackupRepository.getDownloadYourBackupData() }
    when (downloadYourBackupToday?.type) {
      BackupDownloadNotifierState.Type.SHEET -> {
        Log.d(TAG, "Displaying 'Download your backup today' sheet.")
        BackupAlertBottomSheet.create(downloadYourBackupToday).show(fragmentManager, FRAGMENT_TAG)
      }
      BackupDownloadNotifierState.Type.DIALOG -> {
        Log.d(TAG, "Displaying 'Download your backup today' dialog.")
        DownloadYourBackupTodayDialog.create(downloadYourBackupToday).show(fragmentManager, FRAGMENT_TAG)
      }
      null -> Unit
    }
  }
}

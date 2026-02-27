/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import io.zonarosa.core.ui.compose.ComposeDialogFragment
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Dialogs
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.R
import io.zonarosa.messenger.backup.v2.BackupRepository

/**
 * Displays a "last chance" dialog to the user to begin a media restore.
 */
class DownloadYourBackupTodayDialog : ComposeDialogFragment() {

  companion object {

    private const val ARGS = "args"

    fun create(downloadYourBackupData: BackupAlert.DownloadYourBackupData): DialogFragment {
      return DownloadYourBackupTodayDialog().apply {
        arguments = bundleOf(ARGS to downloadYourBackupData)
      }
    }
  }

  private val backupAlert: BackupAlert.DownloadYourBackupData by lazy(LazyThreadSafetyMode.NONE) {
    BundleCompat.getParcelable(requireArguments(), ARGS, BackupAlert.DownloadYourBackupData::class.java)!!
  }

  @Composable
  override fun DialogContent() {
    DownloadYourBackupTodayDialogContent(
      sizeToDownload = backupAlert.formattedSize,
      onConfirm = {
        BackupRepository.resumeMediaRestore()
      },
      onDismiss = {
        BackupRepository.snoozeDownloadYourBackupData()
        dismissAllowingStateLoss()
      }
    )
  }
}

@Composable
private fun DownloadYourBackupTodayDialogContent(
  sizeToDownload: String,
  onConfirm: () -> Unit = {},
  onDismiss: () -> Unit = {}
) {
  Dialogs.SimpleAlertDialog(
    title = stringResource(R.string.DownloadYourBackupTodayDialog__download_your_backup_today),
    body = stringResource(R.string.DownloadYourBackupTodayDialog__you_have_s_of_backup_data, sizeToDownload),
    confirm = stringResource(R.string.DownloadYourBackupTodayDialog__download),
    dismiss = stringResource(R.string.DownloadYourBackupTodayDialog__dont_download),
    dismissColor = MaterialTheme.colorScheme.error,
    onDismiss = onDismiss,
    onConfirm = onConfirm
  )
}

@DayNightPreviews
@Composable
private fun DownloadYourBackupTodayDialogContentPreview() {
  Previews.Preview {
    DownloadYourBackupTodayDialogContent(
      sizeToDownload = "2.3GB"
    )
  }
}

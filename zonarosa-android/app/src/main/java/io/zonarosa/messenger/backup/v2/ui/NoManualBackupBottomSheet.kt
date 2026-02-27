/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.compose.ComposeBottomSheetDialogFragment
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.app.AppSettingsActivity
import io.zonarosa.messenger.jobs.BackupMessagesJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * Displays an alert to the user if they've passed a given threshold without
 * performing a manual backup.
 */
class NoManualBackupBottomSheet : ComposeBottomSheetDialogFragment() {
  @Composable
  override fun SheetContent() {
    val durationSinceLastBackup = remember {
      System.currentTimeMillis().milliseconds - ZonaRosaStore.backup.lastBackupTime.milliseconds
    }

    NoManualBackupSheetContent(
      durationSinceLastBackup = durationSinceLastBackup,
      onBackUpNowClick = {
        BackupMessagesJob.enqueue()
        startActivity(AppSettingsActivity.remoteBackups(requireActivity()))
        dismissAllowingStateLoss()
      },
      onNotNowClick = this::dismissAllowingStateLoss
    )
  }
}

@Composable
private fun NoManualBackupSheetContent(
  durationSinceLastBackup: Duration,
  onBackUpNowClick: () -> Unit = {},
  onNotNowClick: () -> Unit = {}
) {
  val primaryActionLabel = stringResource(R.string.BackupAlertBottomSheet__back_up_now)
  val primaryAction = remember { BackupAlertActionButtonState(primaryActionLabel, onBackUpNowClick) }
  val secondaryActionLabel = stringResource(android.R.string.cancel)
  val secondaryAction = remember { BackupAlertActionButtonState(secondaryActionLabel, onNotNowClick) }
  val days: Int = durationSinceLastBackup.inWholeDays.toInt()

  BackupAlertBottomSheetContainer(
    icon = { BackupAlertIcon(iconColors = BackupsIconColors.Warning) },
    title = pluralStringResource(R.plurals.NoManualBackupBottomSheet__no_backup_for_d_days, days, days),
    primaryActionButtonState = primaryAction,
    secondaryActionButtonState = secondaryAction
  ) {
    BackupAlertText(
      text = pluralStringResource(R.plurals.NoManualBackupBottomSheet__you_have_not_completed_a_backup, days, days),
      modifier = Modifier.padding(bottom = 38.dp)
    )
  }
}

@DayNightPreviews
@Composable
private fun NoManualBackupSheetContentPreview() {
  Previews.BottomSheetContentPreview {
    NoManualBackupSheetContent(
      durationSinceLastBackup = 30.days
    )
  }
}

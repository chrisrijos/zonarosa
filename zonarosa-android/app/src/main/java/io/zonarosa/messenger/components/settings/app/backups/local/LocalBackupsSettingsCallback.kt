/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.components.settings.app.backups.local

import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.navigation.fragment.findNavController
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import io.zonarosa.core.ui.permissions.Permissions
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.LocalBackupJob
import io.zonarosa.messenger.jobs.LocalBackupJob.enqueueArchive
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.service.LocalBackupListener
import io.zonarosa.messenger.util.BackupUtil
import io.zonarosa.messenger.util.CommunicationActions
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.messenger.util.navigation.safeNavigate

sealed interface LocalBackupsSettingsCallback {
  fun onNavigationClick()
  fun onTurnOnClick()
  fun onCreateBackupClick()
  fun onPickTimeClick()
  fun onViewBackupKeyClick()
  fun onLearnMoreClick()
  fun onLaunchBackupLocationPickerClick()
  fun onTurnOffAndDeleteConfirmed()

  object Empty : LocalBackupsSettingsCallback {
    override fun onNavigationClick() = Unit
    override fun onTurnOnClick() = Unit
    override fun onCreateBackupClick() = Unit
    override fun onPickTimeClick() = Unit
    override fun onViewBackupKeyClick() = Unit
    override fun onLearnMoreClick() = Unit
    override fun onLaunchBackupLocationPickerClick() = Unit
    override fun onTurnOffAndDeleteConfirmed() = Unit
  }
}

class DefaultLocalBackupsSettingsCallback(
  private val fragment: LocalBackupsFragment,
  private val chooseBackupLocationLauncher: ActivityResultLauncher<Uri?>,
  private val viewModel: LocalBackupsViewModel
) : LocalBackupsSettingsCallback {

  companion object {
    private val TAG = Log.tag(LocalBackupsSettingsCallback::class)
  }

  override fun onNavigationClick() {
    fragment.requireActivity().onBackPressedDispatcher.onBackPressed()
  }

  override fun onLaunchBackupLocationPickerClick() {
    try {
      Log.d(TAG, "Starting choose backup location dialog")
      chooseBackupLocationLauncher.launch(ZonaRosaStore.settings.latestZonaRosaBackupDirectory)
    } catch (_: ActivityNotFoundException) {
      Toast.makeText(fragment.requireContext(), R.string.BackupDialog_no_file_picker_available, Toast.LENGTH_LONG).show()
    }
  }

  override fun onPickTimeClick() {
    val timeFormat = if (DateFormat.is24HourFormat(fragment.requireContext())) {
      TimeFormat.CLOCK_24H
    } else {
      TimeFormat.CLOCK_12H
    }

    val picker = MaterialTimePicker.Builder()
      .setTimeFormat(timeFormat)
      .setHour(ZonaRosaStore.settings.backupHour)
      .setMinute(ZonaRosaStore.settings.backupMinute)
      .setTitleText(R.string.BackupsPreferenceFragment__set_backup_time)
      .build()

    picker.addOnPositiveButtonClickListener {
      ZonaRosaStore.settings.setBackupSchedule(picker.hour, picker.minute)
      ZonaRosaPreferences.setNextBackupTime(fragment.requireContext(), 0)
      LocalBackupListener.schedule(fragment.requireContext())
      viewModel.refreshSettingsState()
    }

    picker.show(fragment.childFragmentManager, "TIME_PICKER")
  }

  override fun onCreateBackupClick() {
    if (BackupUtil.isUserSelectionRequired(fragment.requireContext())) {
      Log.i(TAG, "Queueing backup...")
      enqueueArchive(false)
    } else {
      Permissions.with(fragment)
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .ifNecessary()
        .onAllGranted {
          Log.i(TAG, "Queuing backup...")
          enqueueArchive(false)
        }
        .withPermanentDenialDialog(
          fragment.getString(R.string.BackupsPreferenceFragment_zonarosa_requires_external_storage_permission_in_order_to_create_backups)
        )
        .execute()
    }
  }

  override fun onTurnOnClick() {
    if (BackupUtil.isUserSelectionRequired(fragment.requireContext())) {
      // When the user-selection flow is required, the screen shows a compose dialog and then
      // triggers [launchBackupDirectoryPicker] via callback.
      // This method intentionally does nothing in that case.
    } else {
      Permissions.with(fragment)
        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        .ifNecessary()
        .onAllGranted {
          onLaunchBackupLocationPickerClick()
        }
        .withPermanentDenialDialog(
          fragment.getString(R.string.BackupsPreferenceFragment_zonarosa_requires_external_storage_permission_in_order_to_create_backups)
        )
        .execute()
    }
  }

  override fun onViewBackupKeyClick() {
    fragment.findNavController().safeNavigate(R.id.action_backupsPreferenceFragment_to_backupKeyDisplayFragment)
  }

  override fun onLearnMoreClick() {
    CommunicationActions.openBrowserLink(fragment.requireContext(), fragment.getString(R.string.backup_support_url))
  }

  override fun onTurnOffAndDeleteConfirmed() {
    ZonaRosaStore.backup.newLocalBackupsEnabled = false

    val path = ZonaRosaStore.backup.newLocalBackupsDirectory
    ZonaRosaStore.backup.newLocalBackupsDirectory = null
    AppDependencies.jobManager.cancelAllInQueue(LocalBackupJob.QUEUE)
    BackupUtil.deleteUnifiedBackups(fragment.requireContext(), path)
  }
}

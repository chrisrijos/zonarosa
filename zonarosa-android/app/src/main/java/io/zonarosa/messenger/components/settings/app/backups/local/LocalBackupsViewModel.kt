/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.backups.local

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.backup.BackupPassphrase
import io.zonarosa.messenger.backup.v2.LocalBackupV2Event
import io.zonarosa.messenger.components.settings.app.backups.remote.BackupKeyCredentialManagerHandler
import io.zonarosa.messenger.components.settings.app.backups.remote.BackupKeySaveState
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.LocalBackupJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.BackupUtil
import io.zonarosa.messenger.util.DateUtils
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.messenger.util.formatHours
import java.text.NumberFormat
import java.time.LocalTime
import java.util.Locale

/**
 * Unified data model backups. Shares the same schema and file breakout as remote backups/.
 */
class LocalBackupsViewModel : ViewModel(), BackupKeyCredentialManagerHandler {

  companion object {
    private val TAG = Log.tag(LocalBackupsViewModel::class)
  }

  private val formatter: NumberFormat = NumberFormat.getInstance().apply {
    minimumFractionDigits = 1
    maximumFractionDigits = 1
  }

  private val internalSettingsState = MutableStateFlow(
    LocalBackupsSettingsState(
      backupsEnabled = ZonaRosaStore.backup.newLocalBackupsEnabled,
      folderDisplayName = ZonaRosaStore.backup.newLocalBackupsDirectory
    )
  )

  private val internalBackupState = MutableStateFlow(LocalBackupsKeyState())

  val settingsState = internalSettingsState
  val backupState = internalBackupState

  init {
    val applicationContext = AppDependencies.application

    viewModelScope.launch {
      ZonaRosaStore.backup.newLocalBackupsEnabledFlow.collect { enabled ->
        internalSettingsState.update { it.copy(backupsEnabled = enabled) }
      }
    }

    viewModelScope.launch {
      ZonaRosaStore.backup.newLocalBackupsDirectoryFlow.collect { directory ->
        internalSettingsState.update { it.copy(folderDisplayName = directory) }
      }
    }

    viewModelScope.launch {
      ZonaRosaStore.backup.newLocalBackupsLastBackupTimeFlow.collect { lastBackupTime ->
        internalSettingsState.update { it.copy(lastBackupLabel = calculateLastBackupTimeString(applicationContext, lastBackupTime)) }
      }
    }

    EventBus.getDefault().register(this)
  }

  override fun onCleared() {
    EventBus.getDefault().unregister(this)
  }

  fun refreshSettingsState() {
    val context = AppDependencies.application
    val backupTime = LocalTime.of(ZonaRosaStore.settings.backupHour, ZonaRosaStore.settings.backupMinute).formatHours(context)

    val userUnregistered = ZonaRosaPreferences.isUnauthorizedReceived(context) || !ZonaRosaStore.account.isRegistered
    val clientDeprecated = ZonaRosaStore.misc.isClientDeprecated
    val legacyLocalBackupsEnabled = ZonaRosaStore.settings.isBackupEnabled && BackupUtil.canUserAccessBackupDirectory(context)
    val canTurnOn = legacyLocalBackupsEnabled || (!userUnregistered && !clientDeprecated)

    if (ZonaRosaStore.backup.newLocalBackupsEnabled) {
      if (!BackupUtil.canUserAccessUnifiedBackupDirectory(context)) {
        Log.w(TAG, "Lost access to backup directory, disabling backups")
        ZonaRosaStore.backup.newLocalBackupsEnabled = false
        AppDependencies.jobManager.cancelAllInQueue(LocalBackupJob.QUEUE)
      }
    } else {
      AppDependencies.jobManager.cancelAllInQueue(LocalBackupJob.QUEUE)
    }

    internalSettingsState.update {
      it.copy(
        canTurnOn = canTurnOn,
        scheduleTimeLabel = backupTime
      )
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onBackupEvent(event: LocalBackupV2Event) {
    val context = AppDependencies.application
    when (event.type) {
      LocalBackupV2Event.Type.FINISHED -> {
        internalSettingsState.update { it.copy(progress = BackupProgressState.Idle) }
      }

      else -> {
        val summary = context.getString(R.string.BackupsPreferenceFragment__in_progress)
        val progressState = if (event.estimatedTotalCount == 0L) {
          BackupProgressState.InProgress(
            summary = summary,
            percentLabel = context.getString(R.string.BackupsPreferenceFragment__d_so_far, event.count),
            progressFraction = null
          )
        } else {
          val fraction = ((event.count / event.estimatedTotalCount.toDouble()) / 100.0).toFloat().coerceIn(0f, 1f)
          BackupProgressState.InProgress(
            summary = summary,
            percentLabel = context.getString(R.string.BackupsPreferenceFragment__s_so_far, formatter.format((event.count / event.estimatedTotalCount.toDouble()))),
            progressFraction = fraction
          )
        }

        internalSettingsState.update { it.copy(progress = progressState) }
      }
    }
  }

  override fun updateBackupKeySaveState(newState: BackupKeySaveState?) {
    internalBackupState.update { it.copy(keySaveState = newState) }
  }

  suspend fun handleUpgrade(context: Context) {
    if (ZonaRosaStore.settings.isBackupEnabled) {
      withContext(Dispatchers.IO) {
        AppDependencies.jobManager.cancelAllInQueue(LocalBackupJob.QUEUE)
        AppDependencies.jobManager.flush()
      }

      ZonaRosaStore.backup.newLocalBackupsDirectory = ZonaRosaStore.settings.zonarosaBackupDirectory?.toString()

      BackupPassphrase.set(context, null)
      ZonaRosaStore.settings.isBackupEnabled = false
      BackupUtil.deleteAllBackups()
    }

    ZonaRosaStore.backup.newLocalBackupsEnabled = true
    LocalBackupJob.enqueueArchive(false)
  }
}

private fun calculateLastBackupTimeString(context: Context, lastBackupTimestamp: Long): String {
  return if (lastBackupTimestamp > 0) {
    val relativeTime = DateUtils.getDatelessRelativeTimeSpanFormattedDate(
      context,
      Locale.getDefault(),
      lastBackupTimestamp
    )

    if (relativeTime.isRelative) {
      relativeTime.value
    } else {
      val day = DateUtils.getDayPrecisionTimeString(context, Locale.getDefault(), lastBackupTimestamp)
      val time = relativeTime.value

      context.getString(R.string.RemoteBackupsSettingsFragment__s_at_s, day, time)
    }
  } else {
    context.getString(R.string.RemoteBackupsSettingsFragment__never)
  }
}

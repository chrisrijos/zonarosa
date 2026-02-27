/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.restore.local

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import io.zonarosa.core.util.ByteSize
import io.zonarosa.core.util.Result
import io.zonarosa.core.util.bytes
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.backup.v2.RestoreV2Event
import io.zonarosa.messenger.backup.v2.local.ArchiveFileSystem
import io.zonarosa.messenger.backup.v2.local.LocalArchiver
import io.zonarosa.messenger.backup.v2.local.SnapshotFileSystem
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.LocalBackupJob
import io.zonarosa.messenger.jobs.RestoreLocalAttachmentJob
import io.zonarosa.messenger.keyvalue.Completed
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.registration.ui.restore.StorageServiceRestore
import io.zonarosa.messenger.registration.util.RegistrationUtil

class RestoreLocalBackupActivityViewModel : ViewModel() {

  companion object {
    private val TAG = Log.tag(RestoreLocalBackupActivityViewModel::class)
  }

  private val internalState = MutableStateFlow(RestoreLocalBackupScreenState())
  val state: StateFlow<RestoreLocalBackupScreenState> = internalState

  init {
    EventBus.getDefault().register(this)
    beginRestore()
  }

  override fun onCleared() {
    EventBus.getDefault().unregister(this)
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  fun onRestoreEvent(event: RestoreV2Event) {
    internalState.update {
      when (event.type) {
        RestoreV2Event.Type.PROGRESS_RESTORE -> it.copy(
          restorePhase = RestorePhase.RESTORING,
          bytesRead = event.count,
          totalBytes = event.estimatedTotalCount,
          progress = event.getProgress()
        )

        RestoreV2Event.Type.PROGRESS_DOWNLOAD -> it.copy(
          restorePhase = RestorePhase.RESTORING,
          bytesRead = event.count,
          totalBytes = event.estimatedTotalCount,
          progress = event.getProgress()
        )

        RestoreV2Event.Type.PROGRESS_FINALIZING -> it.copy(
          restorePhase = RestorePhase.FINALIZING
        )
      }
    }
  }

  private fun beginRestore() {
    viewModelScope.launch(Dispatchers.IO) {
      internalState.update { it.copy(restorePhase = RestorePhase.RESTORING) }

      val self = Recipient.self()
      val selfData = BackupRepository.SelfData(self.aci.get(), self.pni.get(), self.e164.get(), ProfileKey(self.profileKey))

      val backupDirectory = ZonaRosaStore.backup.newLocalBackupsDirectory
      if (backupDirectory == null) {
        Log.w(TAG, "No backup directory set")
        internalState.update { it.copy(restorePhase = RestorePhase.FAILED) }
        return@launch
      }

      val archiveFileSystem = ArchiveFileSystem.fromUri(AppDependencies.application, Uri.parse(backupDirectory))
      if (archiveFileSystem == null) {
        Log.w(TAG, "Unable to access backup directory: $backupDirectory")
        internalState.update { it.copy(restorePhase = RestorePhase.FAILED) }
        return@launch
      }

      val selectedTimestamp = ZonaRosaStore.backup.newLocalBackupsSelectedSnapshotTimestamp
      val snapshots = archiveFileSystem.listSnapshots()
      val snapshotInfo = snapshots.firstOrNull { it.timestamp == selectedTimestamp } ?: snapshots.firstOrNull()

      if (snapshotInfo == null) {
        Log.w(TAG, "No snapshots found in backup directory")
        internalState.update { it.copy(restorePhase = RestorePhase.FAILED) }
        return@launch
      }

      val snapshotFileSystem = SnapshotFileSystem(AppDependencies.application, snapshotInfo.file)
      val result = LocalArchiver.import(snapshotFileSystem, selfData)

      if (result is Result.Success) {
        Log.i(TAG, "Local backup import succeeded")
        val mediaNameToFileInfo = archiveFileSystem.filesFileSystem.allFiles()
        RestoreLocalAttachmentJob.enqueueRestoreLocalAttachmentsJobs(mediaNameToFileInfo)

        ZonaRosaStore.registration.restoreDecisionState = RestoreDecisionState.Completed
        ZonaRosaStore.backup.backupSecretRestoreRequired = false
        ZonaRosaStore.backup.newLocalBackupsSelectedSnapshotTimestamp = -1L
        ZonaRosaStore.backup.newLocalBackupsEnabled = true
        LocalBackupJob.enqueueArchive(false)
        StorageServiceRestore.restore()
        RegistrationUtil.maybeMarkRegistrationComplete()

        internalState.update { it.copy(restorePhase = RestorePhase.COMPLETE) }
      } else {
        Log.w(TAG, "Local backup import failed")
        internalState.update { it.copy(restorePhase = RestorePhase.FAILED) }
      }
    }
  }
}

data class RestoreLocalBackupScreenState(
  val restorePhase: RestorePhase = RestorePhase.RESTORING,
  val bytesRead: ByteSize = 0L.bytes,
  val totalBytes: ByteSize = 0L.bytes,
  val progress: Float = 0f
)

enum class RestorePhase {
  RESTORING,
  FINALIZING,
  COMPLETE,
  FAILED
}

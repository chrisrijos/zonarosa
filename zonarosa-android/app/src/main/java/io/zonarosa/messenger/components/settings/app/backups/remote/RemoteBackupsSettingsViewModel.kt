/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.backups.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.bytes
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.mebiBytes
import io.zonarosa.core.util.throttleLatest
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.backup.ArchiveUploadProgress
import io.zonarosa.messenger.backup.DeletionState
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgress
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgressState.RestoreStatus
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.backup.v2.ui.subscription.BackupUpgradeAvailabilityChecker
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsType
import io.zonarosa.messenger.components.settings.app.backups.BackupState
import io.zonarosa.messenger.components.settings.app.backups.BackupStateObserver
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.attachmentUpdates
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.impl.BackupMessagesConstraint
import io.zonarosa.messenger.jobs.BackupMessagesJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.protos.ArchiveUploadProgressState
import io.zonarosa.messenger.util.Environment
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.NetworkResult
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * ViewModel for state management of RemoteBackupsSettingsFragment
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RemoteBackupsSettingsViewModel : ViewModel() {

  companion object {
    private val TAG = Log.tag(RemoteBackupsSettingsViewModel::class)
  }

  private val _state = MutableStateFlow(
    RemoteBackupsSettingsState(
      tier = ZonaRosaStore.backup.backupTier,
      backupState = BackupStateObserver.getNonIOBackupState(),
      backupsEnabled = ZonaRosaStore.backup.areBackupsEnabled,
      canBackupMessagesJobRun = BackupMessagesConstraint.isMet(AppDependencies.application),
      canViewBackupKey = !ZonaRosaPreferences.isUnauthorizedReceived(AppDependencies.application),
      lastBackupTimestamp = ZonaRosaStore.backup.lastBackupTime,
      canBackUpUsingCellular = ZonaRosaStore.backup.backupWithCellular,
      canRestoreUsingCellular = ZonaRosaStore.backup.restoreWithCellular,
      internalUser = RemoteConfig.internalUser,
      includeDebuglog = ZonaRosaStore.internal.includeDebuglogInBackup.takeIf { RemoteConfig.internalUser },
      backupCreationError = ZonaRosaStore.backup.backupCreationError,
      lastMessageCutoffTime = ZonaRosaStore.backup.lastUsedMessageCutoffTime
    )
  )

  private val _restoreState: MutableStateFlow<BackupRestoreState> = MutableStateFlow(BackupRestoreState.None)
  private val latestPurchaseId = MutableSharedFlow<InAppPaymentTable.InAppPaymentId>()

  val state: StateFlow<RemoteBackupsSettingsState> = _state
  val restoreState: StateFlow<BackupRestoreState> = _restoreState

  private var forQuickRestore = false

  init {
    ArchiveUploadProgress.triggerUpdate()

    viewModelScope.launch(Dispatchers.IO) {
      val isBillingApiAvailable = AppDependencies.billingApi.getApiAvailability().isSuccess
      if (isBillingApiAvailable) {
        _state.update {
          it.copy(isPaidTierPricingAvailable = true)
        }
      } else {
        val paidType = BackupRepository.getPaidType()
        _state.update {
          it.copy(isPaidTierPricingAvailable = paidType is NetworkResult.Success)
        }
      }
    }

    viewModelScope.launch {
      _state.update {
        it.copy(isGooglePlayServicesAvailable = BackupUpgradeAvailabilityChecker.isUpgradeAvailable(AppDependencies.application))
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      refreshBackupMediaSizeState()
    }

    viewModelScope.launch(Dispatchers.IO) {
      ZonaRosaStore.backup.deletionStateFlow.collectLatest {
        refresh()
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      latestPurchaseId
        .flatMapLatest { id -> InAppPaymentsRepository.observeUpdates(id).asFlow() }
        .collectLatest { purchase ->
          Log.d(TAG, "Refreshing state after archive IAP update.")
          refreshState(purchase)
        }
    }

    viewModelScope.launch(Dispatchers.IO) {
      AppDependencies
        .databaseObserver
        .attachmentUpdates()
        .throttleLatest(5.seconds)
        .collectLatest {
          refreshBackupMediaSizeState()
        }
    }

    viewModelScope.launch(Dispatchers.IO) {
      var optimizedRemainingBytes = 0L
      while (isActive) {
        if (ArchiveRestoreProgress.state.let { it.restoreState.isMediaRestoreOperation || it.restoreStatus == RestoreStatus.FINISHED }) {
          Log.d(TAG, "Backup is being restored. Collecting updates.")
          ArchiveRestoreProgress
            .stateFlow
            .takeWhile { it.restoreState.isMediaRestoreOperation || it.restoreStatus == RestoreStatus.FINISHED }
            .onEach { latest -> _restoreState.update { BackupRestoreState.Restoring(latest) } }
            .collect()
        } else if (
          !ZonaRosaStore.backup.optimizeStorage &&
          ZonaRosaStore.backup.userManuallySkippedMediaRestore &&
          ZonaRosaDatabase.attachments.getOptimizedMediaAttachmentSize().also { optimizedRemainingBytes = it } > 0
        ) {
          _restoreState.update { BackupRestoreState.Ready(optimizedRemainingBytes.bytes.toUnitString()) }
        } else if (ZonaRosaStore.backup.totalRestorableAttachmentSize > 0L) {
          _restoreState.update { BackupRestoreState.Ready(ZonaRosaStore.backup.totalRestorableAttachmentSize.bytes.toUnitString()) }
        } else {
          _restoreState.update { BackupRestoreState.None }
        }

        delay(1.seconds)
      }
    }

    viewModelScope.launch {
      var previous: ArchiveUploadProgressState.State? = null
      ArchiveUploadProgress.progress
        .collect { current ->
          if (previous != null && previous != current.state && current.state == ArchiveUploadProgressState.State.None) {
            Log.d(TAG, "Refreshing state after archive upload.")
            if (forQuickRestore) {
              Log.d(TAG, "Backup completed with the forQuickRestore flag on. Refreshing state.")
              _state.value = _state.value.copy(dialog = RemoteBackupsSettingsState.Dialog.READY_TO_TRANSFER)
            }
            refreshState(null)
          }
          previous = current.state
        }
    }

    viewModelScope.launch(Dispatchers.IO) {
      BackupStateObserver(viewModelScope).backupState.collect { state ->
        _state.update {
          it.copy(backupState = state)
        }
        refreshState(null)
      }
    }

    viewModelScope.launch(Dispatchers.IO) {
      BackupRepository.maybeFixAnyDanglingUploadProgress()
    }
  }

  fun setCanBackUpUsingCellular(canBackUpUsingCellular: Boolean) {
    ZonaRosaStore.backup.backupWithCellular = canBackUpUsingCellular
    _state.update {
      it.copy(
        canBackupMessagesJobRun = BackupMessagesConstraint.isMet(AppDependencies.application),
        canBackUpUsingCellular = canBackUpUsingCellular
      )
    }
  }

  fun setCanRestoreUsingCellular() {
    ZonaRosaStore.backup.restoreWithCellular = true
    _state.update { it.copy(canRestoreUsingCellular = true) }
  }

  fun beginMediaRestore() {
    BackupRepository.resumeMediaRestore()
  }

  fun cancelMediaRestore() {
    if (ArchiveRestoreProgress.state.restoreStatus == RestoreStatus.FINISHED) {
      ArchiveRestoreProgress.clearFinishedStatus()
    } else {
      requestDialog(RemoteBackupsSettingsState.Dialog.CANCEL_MEDIA_RESTORE_PROTECTION)
    }
  }

  fun skipMediaRestore() {
    BackupRepository.skipMediaRestore()

    if (ZonaRosaStore.backup.deletionState == DeletionState.AWAITING_MEDIA_DOWNLOAD) {
      BackupRepository.continueTurningOffAndDisablingBackups()
    }
  }

  fun requestDialog(dialog: RemoteBackupsSettingsState.Dialog) {
    _state.update { it.copy(dialog = dialog) }
  }

  fun requestSnackbar(snackbar: RemoteBackupsSettingsState.Snackbar) {
    _state.update { it.copy(snackbar = snackbar) }
  }

  fun getKeyRotationLimit() {
    viewModelScope.launch(ZonaRosaDispatchers.IO) {
      val result = BackupRepository.getKeyRotationLimit()
      val canRotateKey = if (result is NetworkResult.Success) {
        result.result.hasPermitsRemaining!!
      } else {
        Log.w(TAG, "Error while getting rotation limit: $result. Default to allowing key rotations.")
        true
      }

      if (!canRotateKey) {
        requestDialog(RemoteBackupsSettingsState.Dialog.KEY_ROTATION_LIMIT_REACHED)
      }
    }
  }

  fun refresh() {
    viewModelScope.launch(Dispatchers.IO) {
      val id = ZonaRosaDatabase.inAppPayments.getLatestInAppPaymentByType(InAppPaymentType.RECURRING_BACKUP)?.id

      if (id != null) {
        latestPurchaseId.emit(id)
      } else {
        refreshState(null)
      }
    }
  }

  fun turnOffAndDeleteBackups() {
    viewModelScope.launch {
      requestDialog(RemoteBackupsSettingsState.Dialog.PROGRESS_SPINNER)

      withContext(Dispatchers.IO) {
        BackupRepository.turnOffAndDisableBackups()
      }

      requestDialog(RemoteBackupsSettingsState.Dialog.NONE)
    }
  }

  fun onBackupNowClick(forQuickRestore: Boolean) {
    BackupMessagesJob.enqueue()
    this.forQuickRestore = forQuickRestore
  }

  fun cancelUpload() {
    ArchiveUploadProgress.cancel()
  }

  fun setIncludeDebuglog(includeDebuglog: Boolean) {
    ZonaRosaStore.internal.includeDebuglogInBackup = includeDebuglog
    _state.update { it.copy(includeDebuglog = includeDebuglog) }
  }

  private fun refreshBackupMediaSizeState() {
    _state.update {
      val (mediaSize, mediaRetentionDays) = getBackupSize(it.tier, (it.backupState as? BackupState.WithTypeAndRenewalTime)?.messageBackupsType)
      it.copy(
        backupMediaSize = mediaSize,
        freeTierMediaRetentionDays = mediaRetentionDays,
        backupMediaDetails = if (RemoteConfig.internalUser || Environment.IS_STAGING) {
          RemoteBackupsSettingsState.BackupMediaDetails(
            awaitingRestore = ZonaRosaDatabase.attachments.getRemainingRestorableAttachmentSize().bytes,
            offloaded = ZonaRosaDatabase.attachments.getOptimizedMediaAttachmentSize().bytes,
            protoFileSize = ZonaRosaStore.backup.lastBackupProtoSize.bytes
          )
        } else null
      )
    }
  }

  private suspend fun refreshState(lastPurchase: InAppPaymentTable.InAppPayment?) {
    try {
      Log.i(TAG, "Performing a state refresh.")
      performStateRefresh(lastPurchase)
    } catch (e: Exception) {
      Log.w(TAG, "State refresh failed", e)
      throw e
    }
  }

  private suspend fun performStateRefresh(lastPurchase: InAppPaymentTable.InAppPayment?) {
    if (BackupRepository.shouldDisplayOutOfRemoteStorageSpaceUx()) {
      val paidType = BackupRepository.getPaidType()

      if (paidType is NetworkResult.Success) {
        val remoteStorageAllowance = paidType.result.storageAllowanceBytes.bytes
        val estimatedSize = getBackupSize(paidType.result.tier, paidType.result).first.bytes

        if (estimatedSize + 300.mebiBytes <= remoteStorageAllowance) {
          BackupRepository.clearOutOfRemoteStorageSpaceError()
        }

        _state.update {
          it.copy(
            totalAllowedStorageSpace = estimatedSize.toUnitString()
          )
        }
      } else {
        Log.w(TAG, "Failed to load PAID type.", paidType.getCause())
      }
    }

    val (mediaSize, mediaRetentionDays) = getBackupSize(_state.value.tier, (_state.value.backupState as? BackupState.WithTypeAndRenewalTime)?.messageBackupsType)

    _state.update {
      it.copy(
        tier = ZonaRosaStore.backup.backupTier,
        backupsEnabled = ZonaRosaStore.backup.areBackupsEnabled,
        lastBackupTimestamp = ZonaRosaStore.backup.lastBackupTime,
        canBackupMessagesJobRun = BackupMessagesConstraint.isMet(AppDependencies.application),
        backupMediaSize = mediaSize,
        freeTierMediaRetentionDays = mediaRetentionDays,
        canBackUpUsingCellular = ZonaRosaStore.backup.backupWithCellular,
        canRestoreUsingCellular = ZonaRosaStore.backup.restoreWithCellular,
        isOutOfStorageSpace = BackupRepository.shouldDisplayOutOfRemoteStorageSpaceUx(),
        hasRedemptionError = lastPurchase?.data?.error?.data_ == "409",
        backupCreationError = ZonaRosaStore.backup.backupCreationError,
        lastMessageCutoffTime = ZonaRosaStore.backup.lastUsedMessageCutoffTime
      )
    }
  }

  private fun getBackupSize(tier: MessageBackupTier?, messageBackupsType: MessageBackupsType?): Pair<Long, Int> {
    if (tier == null) {
      return -1L to 0
    }

    val mediaRetentionDays = if (messageBackupsType is MessageBackupsType.Free) {
      messageBackupsType.mediaRetentionDays
    } else {
      when (tier) {
        MessageBackupTier.FREE -> {
          when (val result = BackupRepository.getFreeType()) {
            is NetworkResult.Success -> result.result.mediaRetentionDays
            else -> RemoteConfig.messageQueueTime.milliseconds.inWholeDays.toInt()
          }
        }

        MessageBackupTier.PAID -> 0
      }
    }

    return if (ZonaRosaStore.backup.hasBackupBeenUploaded || ZonaRosaStore.backup.lastBackupTime > 0L) {
      when (tier) {
        MessageBackupTier.PAID -> (ZonaRosaStore.backup.lastBackupProtoSize + ZonaRosaDatabase.attachments.getPaidEstimatedArchiveMediaSize()) to -1
        MessageBackupTier.FREE -> {
          if (mediaRetentionDays > 0) {
            (ZonaRosaStore.backup.lastBackupProtoSize + ZonaRosaDatabase.attachments.getFreeEstimatedArchiveMediaSize(System.currentTimeMillis() - mediaRetentionDays.days.inWholeMilliseconds)) to mediaRetentionDays
          } else {
            -1L to -1
          }
        }
      }
    } else {
      0L to mediaRetentionDays
    }
  }
}

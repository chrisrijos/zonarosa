/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.storage

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.backup.v2.ui.subscription.BackupUpgradeAvailabilityChecker
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.MediaTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.ZonaRosaDatabase.Companion.media
import io.zonarosa.messenger.database.ThreadTable
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.OptimizeMediaJob
import io.zonarosa.messenger.jobs.RestoreOptimizedMediaJob
import io.zonarosa.messenger.keyvalue.KeepMessagesDuration
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

class ManageStorageSettingsViewModel : ViewModel() {

  private val store = MutableStateFlow(
    ManageStorageState(
      keepMessagesDuration = ZonaRosaStore.settings.keepMessagesDuration,
      lengthLimit = if (ZonaRosaStore.settings.isTrimByLengthEnabled) ZonaRosaStore.settings.threadTrimLength else ManageStorageState.NO_LIMIT,
      syncTrimDeletes = ZonaRosaStore.settings.shouldSyncThreadTrimDeletes()
    )
  )
  val state = store.asStateFlow()

  init {
    viewModelScope.launch(Dispatchers.IO) {
      InAppPaymentsRepository.observeLatestBackupPayment()
        .collectLatest { payment ->
          store.update { it.copy(isPaidTierPending = payment.state == InAppPaymentTable.State.PENDING) }
        }
    }

    viewModelScope.launch {
      store.update {
        it.copy(onDeviceStorageOptimizationState = getOnDeviceStorageOptimizationState())
      }
    }
  }

  fun refresh() {
    viewModelScope.launch {
      val breakdown: MediaTable.StorageBreakdown = media.getStorageBreakdown()
      store.update { it.copy(breakdown = breakdown) }
    }
  }

  fun deleteChatHistory() {
    ZonaRosaExecutors.BOUNDED_IO.execute {
      ZonaRosaDatabase.threads.deleteAllConversations()
      AppDependencies.messageNotifier.updateNotification(AppDependencies.application)
    }
  }

  fun setKeepMessagesDuration(newDuration: KeepMessagesDuration) {
    ZonaRosaStore.settings.setKeepMessagesForDuration(newDuration)
    AppDependencies.trimThreadsByDateManager.scheduleIfNecessary()

    store.update { it.copy(keepMessagesDuration = newDuration) }
  }

  fun showConfirmKeepDurationChange(newDuration: KeepMessagesDuration): Boolean {
    return newDuration.ordinal > state.value.keepMessagesDuration.ordinal
  }

  fun setChatLengthLimit(newLimit: Int) {
    val restrictingChange = isRestrictingLengthLimitChange(newLimit)

    ZonaRosaStore.settings.setThreadTrimByLengthEnabled(newLimit != ManageStorageState.NO_LIMIT)
    ZonaRosaStore.settings.threadTrimLength = newLimit
    store.update { it.copy(lengthLimit = newLimit) }

    if (ZonaRosaStore.settings.isTrimByLengthEnabled && restrictingChange) {
      ZonaRosaExecutors.BOUNDED.execute {
        val keepMessagesDuration = ZonaRosaStore.settings.keepMessagesDuration

        val trimBeforeDate = if (keepMessagesDuration != KeepMessagesDuration.FOREVER) {
          System.currentTimeMillis() - keepMessagesDuration.duration
        } else {
          ThreadTable.NO_TRIM_BEFORE_DATE_SET
        }

        ZonaRosaDatabase.threads.trimAllThreads(newLimit, trimBeforeDate)
      }
    }
  }

  fun showConfirmSetChatLengthLimit(newLimit: Int): Boolean {
    return isRestrictingLengthLimitChange(newLimit)
  }

  fun setSyncTrimDeletes(syncTrimDeletes: Boolean) {
    ZonaRosaStore.settings.setSyncThreadTrimDeletes(syncTrimDeletes)
    store.update { it.copy(syncTrimDeletes = syncTrimDeletes) }
  }

  fun setOptimizeStorage(enabled: Boolean) {
    viewModelScope.launch {
      val storageState = getOnDeviceStorageOptimizationState()
      if (storageState >= OnDeviceStorageOptimizationState.DISABLED) {
        ZonaRosaStore.backup.optimizeStorage = enabled
        store.update {
          it.copy(
            onDeviceStorageOptimizationState = if (enabled) OnDeviceStorageOptimizationState.ENABLED else OnDeviceStorageOptimizationState.DISABLED,
            storageOptimizationStateChanged = true
          )
        }
      }
    }
  }

  private fun isRestrictingLengthLimitChange(newLimit: Int): Boolean {
    return state.value.lengthLimit == ManageStorageState.NO_LIMIT || (newLimit != ManageStorageState.NO_LIMIT && newLimit < state.value.lengthLimit)
  }

  private suspend fun getOnDeviceStorageOptimizationState(): OnDeviceStorageOptimizationState {
    return when {
      !ZonaRosaStore.backup.areBackupsEnabled || !BackupUpgradeAvailabilityChecker.isUpgradeAvailable(AppDependencies.application) -> OnDeviceStorageOptimizationState.FEATURE_NOT_AVAILABLE
      ZonaRosaStore.backup.backupTier != MessageBackupTier.PAID -> OnDeviceStorageOptimizationState.REQUIRES_PAID_TIER
      ZonaRosaStore.backup.optimizeStorage -> OnDeviceStorageOptimizationState.ENABLED
      else -> OnDeviceStorageOptimizationState.DISABLED
    }
  }

  override fun onCleared() {
    if (state.value.storageOptimizationStateChanged) {
      when (state.value.onDeviceStorageOptimizationState) {
        OnDeviceStorageOptimizationState.DISABLED -> RestoreOptimizedMediaJob.enqueue()
        OnDeviceStorageOptimizationState.ENABLED -> OptimizeMediaJob.enqueue()
        else -> Unit
      }
    }
  }

  enum class OnDeviceStorageOptimizationState {
    /**
     * The entire feature is not available and the option should not be displayed to the user.
     */
    FEATURE_NOT_AVAILABLE,

    /**
     * The feature is available, but the user is not on the paid backups plan.
     */
    REQUIRES_PAID_TIER,

    /**
     * The user is on the paid backups plan but optimized storage is disabled.
     */
    DISABLED,

    /**
     * The user is on the paid backups plan and optimized storage is enabled.
     */
    ENABLED
  }

  @Immutable
  data class ManageStorageState(
    val keepMessagesDuration: KeepMessagesDuration,
    val lengthLimit: Int,
    val syncTrimDeletes: Boolean,
    val breakdown: MediaTable.StorageBreakdown? = null,
    val onDeviceStorageOptimizationState: OnDeviceStorageOptimizationState = OnDeviceStorageOptimizationState.FEATURE_NOT_AVAILABLE,
    val storageOptimizationStateChanged: Boolean = false,
    val isPaidTierPending: Boolean = false
  ) {
    companion object {
      const val NO_LIMIT = 0
    }
  }
}

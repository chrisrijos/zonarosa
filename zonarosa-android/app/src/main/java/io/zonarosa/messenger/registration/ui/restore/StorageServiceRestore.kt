/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.restore

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.Stopwatch
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.enqueueBlocking
import io.zonarosa.messenger.jobmanager.runJobBlocking
import io.zonarosa.messenger.jobs.ProfileUploadJob
import io.zonarosa.messenger.jobs.ReclaimUsernameAndLinkJob
import io.zonarosa.messenger.jobs.StorageAccountRestoreJob
import io.zonarosa.messenger.jobs.StorageSyncJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.registration.data.RegistrationRepository
import io.zonarosa.messenger.registration.util.RegistrationUtil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object StorageServiceRestore {
  private val TAG = Log.tag(StorageServiceRestore::class)

  /**
   * Restore account data from Storage Service in a quasi-blocking manner. Uses existing jobs
   * to perform the restore but will not wait indefinitely for them to finish so may return prior
   * to completing the restore.
   */
  suspend fun restore() {
    withContext(Dispatchers.IO) {
      val stopwatch = Stopwatch("storage-service-restore")

      ZonaRosaStore.storageService.needsAccountRestore = false

      AppDependencies.jobManager.runJobBlocking(StorageAccountRestoreJob(), StorageAccountRestoreJob.LIFESPAN.milliseconds)
      stopwatch.split("account-restore")

      AppDependencies
        .jobManager
        .startChain(StorageSyncJob.forAccountRestore())
        .then(ReclaimUsernameAndLinkJob())
        .enqueueBlocking(10.seconds)
      stopwatch.split("storage-sync-restore")

      stopwatch.stop(TAG)

      val isMissingProfileData = RegistrationRepository.isMissingProfileData()

      RegistrationUtil.maybeMarkRegistrationComplete()
      if (!isMissingProfileData && ZonaRosaStore.account.isPrimaryDevice) {
        AppDependencies.jobManager.add(ProfileUploadJob())
      }
    }
  }
}

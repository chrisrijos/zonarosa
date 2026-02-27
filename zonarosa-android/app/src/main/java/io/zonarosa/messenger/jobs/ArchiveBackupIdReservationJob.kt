/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.NetworkResult

/**
 * Reserves backupIds for both text+media. The intention is that every registered user should be doing this, so it should happen post-registration
 * (as well as in a migration for pre-existing users).
 *
 * Calling this repeatedly is a no-op from the server's perspective, so no need to be careful around retries or anything.
 */
class ArchiveBackupIdReservationJob private constructor(parameters: Parameters) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(ArchiveBackupIdReservationJob::class)

    const val KEY = "ArchiveBackupIdReservationJob"
  }

  constructor() : this(
    Parameters.Builder()
      .setQueue("ArchiveBackupIdReservationJob")
      .addConstraint(NetworkConstraint.KEY)
      .setLifespan(Parameters.IMMORTAL)
      .setMaxAttempts(Parameters.UNLIMITED)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.w(TAG, "Not registered. Skipping.")
      return Result.success()
    }

    if (ZonaRosaPreferences.isUnauthorizedReceived(context)) {
      Log.w(TAG, "Not authorized. Skipping.")
      return Result.success()
    }

    if (ZonaRosaStore.account.isLinkedDevice) {
      Log.i(TAG, "Linked device. Skipping.")
      return Result.success()
    }

    return when (val result = BackupRepository.triggerBackupIdReservation()) {
      is NetworkResult.Success -> Result.success()
      is NetworkResult.NetworkError -> Result.retry(defaultBackoff())
      is NetworkResult.ApplicationError -> Result.fatalFailure(RuntimeException(result.throwable))
      is NetworkResult.StatusCodeError -> {
        when (result.code) {
          429 -> Result.retry(result.retryAfter()?.inWholeMilliseconds ?: defaultBackoff())
          else -> {
            Log.w(TAG, "Failed to reserve backupId with status: ${result.code}. This should only happen on a malformed request or server error. Reducing backoff interval to be safe.")
            Result.retry(RemoteConfig.serverErrorMaxBackoff)
          }
        }
      }
    }
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<ArchiveBackupIdReservationJob> {
    override fun create(parameters: Parameters, data: ByteArray?): ArchiveBackupIdReservationJob {
      return ArchiveBackupIdReservationJob(parameters)
    }
  }
}

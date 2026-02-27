/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.jobs

import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.Job.Result
import io.zonarosa.messenger.jobmanager.JsonJobData
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.pin.Svr3Migration
import io.zonarosa.messenger.pin.SvrRepository
import io.zonarosa.service.api.svr.SecureValueRecovery
import io.zonarosa.service.api.svr.SecureValueRecovery.BackupResponse
import io.zonarosa.service.api.svr.SecureValueRecovery.PinChangeSession
import io.zonarosa.service.internal.push.AuthCredentials
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

/**
 * Attempts to reset the guess on the SVR PIN. Intended to be enqueued after a successful restore.
 */
class ResetSvrGuessCountJob private constructor(
  parameters: Parameters,
  private var serializedChangeSessionV2: String?,
  private var serializedChangeSessionV3: String?,
  private var svr2Complete: Boolean,
  private var svr3Complete: Boolean
) : Job(parameters) {

  companion object {
    const val KEY = "ResetSvrGuessCountJob"

    private val TAG = Log.tag(ResetSvrGuessCountJob::class.java)

    private const val KEY_CHANGE_SESSION_V2 = "change_session"
    private const val KEY_CHANGE_SESSION_V3 = "change_session_v3"
    private const val KEY_SVR2_COMPLETE = "svr2_complete"
    private const val KEY_SVR3_COMPLETE = "svr3_complete"
  }

  constructor() : this(
    Parameters.Builder()
      .addConstraint(NetworkConstraint.KEY)
      .setLifespan(1.days.inWholeMilliseconds)
      .setMaxAttempts(Parameters.UNLIMITED)
      .setQueue("ResetSvrGuessCountJob")
      .setMaxInstancesForFactory(1)
      .build(),
    null,
    null,
    false,
    false
  )

  override fun serialize(): ByteArray? {
    return JsonJobData.Builder()
      .putString(KEY_CHANGE_SESSION_V2, serializedChangeSessionV2)
      .putString(KEY_CHANGE_SESSION_V3, serializedChangeSessionV3)
      .putBoolean(KEY_SVR2_COMPLETE, svr2Complete)
      .putBoolean(KEY_SVR3_COMPLETE, svr3Complete)
      .build()
      .serialize()
  }

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    SvrRepository.operationLock.withLock {
      val pin = ZonaRosaStore.svr.pin

      if (ZonaRosaStore.svr.hasOptedOut()) {
        Log.w(TAG, "Opted out of SVR! Nothing to migrate.")
        return Result.success()
      }

      if (pin == null) {
        Log.w(TAG, "No PIN available! Can't migrate!")
        return Result.success()
      }

      val masterKey: MasterKey = ZonaRosaStore.svr.masterKey

      val svr3Result = if (svr3Complete) {
        Log.d(TAG, "Already reset guess count on SVR3. Skipping.")
        Result.success()
      } else if (!Svr3Migration.shouldWriteToSvr3) {
        Log.d(TAG, "SVR3 writes disabled. Skipping.")
        Result.success()
      } else {
        Log.d(TAG, "Resetting count on SVR3...")
        resetGuessCount(
          svr = AppDependencies.zonarosaServiceAccountManager.getSecureValueRecoveryV3(AppDependencies.libzonarosaNetwork),
          serializedChangeSession = serializedChangeSessionV3,
          pin = pin,
          masterKey = masterKey,
          changeSessionSaver = { serializedChangeSessionV3 = it.serialize() },
          authTokenSaver = { ZonaRosaStore.svr.appendSvr3AuthTokenToList(it.asBasic()) }
        )
      }

      if (svr3Result.isRetry) {
        return svr3Result
      }

      return if (svr2Complete) {
        Log.d(TAG, "Already reset guess count on SVR2. Skipping.")
        Result.success()
      } else if (!Svr3Migration.shouldWriteToSvr2) {
        Log.d(TAG, "SVR2 writes disabled. Skipping.")
        Result.success()
      } else {
        Log.d(TAG, "Resetting count on SVR2...")
        resetGuessCount(
          svr = AppDependencies.zonarosaServiceAccountManager.getSecureValueRecoveryV2(BuildConfig.SVR2_MRENCLAVE),
          serializedChangeSession = serializedChangeSessionV2,
          pin = pin,
          masterKey = masterKey,
          changeSessionSaver = { serializedChangeSessionV2 = it.serialize() },
          authTokenSaver = { ZonaRosaStore.svr.appendSvr2AuthTokenToList(it.asBasic()) }
        )
      }
    }
  }

  override fun onFailure() = Unit

  private fun resetGuessCount(
    svr: SecureValueRecovery,
    serializedChangeSession: String?,
    pin: String,
    masterKey: MasterKey,
    changeSessionSaver: (PinChangeSession) -> Unit,
    authTokenSaver: (AuthCredentials) -> Unit
  ): Result {
    val session: PinChangeSession = if (serializedChangeSession != null) {
      svr.resumePinChangeSession(pin, ZonaRosaStore.svr.masterKey, serializedChangeSession)
    } else {
      svr.setPin(pin, masterKey)
    }

    changeSessionSaver(session)

    return when (val response: BackupResponse = session.execute()) {
      is BackupResponse.Success -> {
        Log.i(TAG, "Successfully reset guess count. $svr")
        authTokenSaver(response.authorization)
        Result.success()
      }
      is BackupResponse.ApplicationError -> {
        Log.w(TAG, "Hit an application error. Retrying. $svr", response.exception)
        Result.retry(defaultBackoff())
      }
      BackupResponse.EnclaveNotFound -> {
        Log.w(TAG, "Could not find the enclave. Giving up. $svr")
        Result.success()
      }
      BackupResponse.ExposeFailure -> {
        Log.w(TAG, "Failed to expose the backup. Giving up. $svr")
        Result.success()
      }
      is BackupResponse.RateLimited -> {
        val backoff = response.retryAfter ?: defaultBackoff().milliseconds
        Log.w(TAG, "Hit rate limit. Retrying in $backoff")
        Result.retry(backoff.inWholeMilliseconds)
      }
      is BackupResponse.NetworkError -> {
        Log.w(TAG, "Hit a network error. Retrying. $svr", response.exception)
        Result.retry(defaultBackoff())
      }
      BackupResponse.ServerRejected -> {
        Log.w(TAG, "Server told us to stop trying. Giving up. $svr")
        Result.success()
      }
    }
  }

  class Factory : Job.Factory<ResetSvrGuessCountJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): ResetSvrGuessCountJob {
      val data = JsonJobData.deserialize(serializedData)

      return ResetSvrGuessCountJob(
        parameters,
        data.getString(KEY_CHANGE_SESSION_V2),
        data.getStringOrDefault(KEY_CHANGE_SESSION_V3, null),
        data.getBoolean(KEY_SVR2_COMPLETE),
        data.getBooleanOrDefault(KEY_SVR3_COMPLETE, false)
      )
    }
  }
}

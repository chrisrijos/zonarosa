/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.pin

import android.app.backup.BackupManager
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.util.Stopwatch
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.JobTracker
import io.zonarosa.messenger.jobs.MultiDeviceKeysUpdateJob
import io.zonarosa.messenger.jobs.RefreshAttributesJob
import io.zonarosa.messenger.jobs.ResetSvrGuessCountJob
import io.zonarosa.messenger.jobs.StorageForcePushJob
import io.zonarosa.messenger.jobs.Svr2MirrorJob
import io.zonarosa.messenger.jobs.Svr3MirrorJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.lock.v2.PinKeyboardType
import io.zonarosa.messenger.megaphone.Megaphones
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.registration.ui.restore.StorageServiceRestore
import io.zonarosa.messenger.registration.viewmodel.SvrAuthCredentialSet
import io.zonarosa.service.api.NetworkResultUtil
import io.zonarosa.service.api.SvrNoDataException
import io.zonarosa.service.api.svr.SecureValueRecovery
import io.zonarosa.service.api.svr.SecureValueRecovery.BackupResponse
import io.zonarosa.service.api.svr.SecureValueRecovery.RestoreResponse
import io.zonarosa.service.api.svr.SecureValueRecovery.SvrVersion
import io.zonarosa.service.api.svr.Svr3Credentials
import io.zonarosa.service.internal.push.AuthCredentials
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object SvrRepository {

  val TAG = Log.tag(SvrRepository::class.java)

  private val svr2Legacy: SecureValueRecovery = AppDependencies.zonarosaServiceAccountManager.getSecureValueRecoveryV2(BuildConfig.SVR2_MRENCLAVE_LEGACY)
  private val svr2: SecureValueRecovery = AppDependencies.zonarosaServiceAccountManager.getSecureValueRecoveryV2(BuildConfig.SVR2_MRENCLAVE)
  private val svr3: SecureValueRecovery = AppDependencies.zonarosaServiceAccountManager.getSecureValueRecoveryV3(AppDependencies.libzonarosaNetwork)

  /** An ordered list of SVR implementations to read from. They should be in priority order, with the most important one listed first. */
  private val readImplementations: List<SecureValueRecovery> = if (Svr3Migration.shouldReadFromSvr3) {
    listOf(svr3, svr2)
  } else {
    listOf(svr2, svr2Legacy)
  }

  /** An ordered list of SVR implementations to write to. They should be in priority order, with the most important one listed first. */
  private val writeImplementations: List<SecureValueRecovery>
    get() {
      val implementations = mutableListOf<SecureValueRecovery>()
      if (Svr3Migration.shouldWriteToSvr3) {
        implementations += svr3
      }
      if (Svr3Migration.shouldWriteToSvr2) {
        implementations += svr2
        implementations += svr2Legacy
      }
      return implementations
    }

  /**
   * A lock that ensures that only one thread at a time is altering the various pieces of SVR state.
   *
   * External usage of this should be limited to one-time migrations. Any routine operation that needs the lock should go in
   * this repository instead.
   */
  val operationLock = ReentrantLock()

  /**
   * Restores the master key from the first available SVR implementation available.
   *
   * This is intended to be called before registration has been completed, requiring
   * that you pass in the credentials provided during registration to access SVR.
   *
   * You could be hitting this because the user has reglock (and therefore need to
   * restore the master key before you can register), or you may be doing the
   * sms-skip flow.
   */
  @JvmStatic
  @WorkerThread
  @Throws(IOException::class, SvrWrongPinException::class, SvrNoDataException::class)
  fun restoreMasterKeyPreRegistration(credentials: SvrAuthCredentialSet, userPin: String): MasterKey {
    operationLock.withLock {
      Log.i(TAG, "restoreMasterKeyPreRegistration()", true)

      val operations: List<Pair<SecureValueRecovery, () -> RestoreResponse>> = if (Svr3Migration.shouldReadFromSvr3) {
        listOf(
          svr3 to { restoreMasterKeyPreRegistrationFromV3(credentials.svr3, userPin) },
          svr2 to { restoreMasterKeyPreRegistrationFromV2(svr2, credentials.svr2, userPin) }
        )
      } else {
        listOf(
          svr2 to { restoreMasterKeyPreRegistrationFromV2(svr2, credentials.svr2, userPin) },
          svr2Legacy to { restoreMasterKeyPreRegistrationFromV2(svr2Legacy, credentials.svr2, userPin) }
        )
      }

      for ((implementation, operation) in operations) {
        when (val response: RestoreResponse = operation()) {
          is RestoreResponse.Success -> {
            Log.i(TAG, "[restoreMasterKeyPreRegistration] Successfully restored master key. $implementation", true)

            when (implementation.svrVersion) {
              SvrVersion.SVR2 -> ZonaRosaStore.svr.appendSvr2AuthTokenToList(response.authorization.asBasic())
              SvrVersion.SVR3 -> ZonaRosaStore.svr.appendSvr3AuthTokenToList(response.authorization.asBasic())
            }

            return response.masterKey
          }

          is RestoreResponse.PinMismatch -> {
            Log.i(TAG, "[restoreMasterKeyPreRegistration] Incorrect PIN. $implementation", true)
            throw SvrWrongPinException(response.triesRemaining)
          }

          is RestoreResponse.NetworkError -> {
            Log.i(TAG, "[restoreMasterKeyPreRegistration] Network error. $implementation", response.exception, true)
            throw response.exception
          }

          is RestoreResponse.ApplicationError -> {
            Log.i(TAG, "[restoreMasterKeyPreRegistration] Application error. $implementation", response.exception, true)
            throw IOException(response.exception)
          }

          RestoreResponse.Missing -> {
            Log.w(TAG, "[restoreMasterKeyPreRegistration] No data found for $implementation | Continuing to next implementation.", true)
          }

          RestoreResponse.EnclaveNotFound -> {
            Log.w(TAG, "[restoreMasterKeyPreRegistration] Enclave no longer exists: $implementation | Continuing to next implementation.", true)
          }
        }
      }

      Log.w(TAG, "[restoreMasterKeyPreRegistration] No data found for any implementation!", true)

      throw SvrNoDataException()
    }
  }

  /**
   * Restores the master key from the first available SVR implementation available.
   *
   * This is intended to be called after the user has registered, allowing the function
   * to fetch credentials on its own.
   */
  @WorkerThread
  fun restoreMasterKeyPostRegistration(userPin: String, pinKeyboardType: PinKeyboardType): RestoreResponse {
    val stopwatch = Stopwatch("pin-submission")

    operationLock.withLock {
      for (implementation in readImplementations) {
        when (val response: RestoreResponse = implementation.restoreDataPostRegistration(userPin)) {
          is RestoreResponse.Success -> {
            Log.i(TAG, "[restoreMasterKeyPostRegistration] Successfully restored master key. $implementation", true)
            stopwatch.split("restore")

            ZonaRosaStore.registration.localRegistrationMetadata?.let { metadata ->
              ZonaRosaStore.registration.localRegistrationMetadata = metadata.copy(masterKey = response.masterKey.serialize().toByteString(), pin = userPin)
            }

            ZonaRosaStore.svr.masterKeyForInitialDataRestore = response.masterKey
            ZonaRosaStore.svr.setPin(userPin)
            ZonaRosaStore.svr.isRegistrationLockEnabled = false
            ZonaRosaStore.pin.resetPinReminders()
            ZonaRosaStore.pin.keyboardType = pinKeyboardType

            when (implementation.svrVersion) {
              SvrVersion.SVR2 -> ZonaRosaStore.svr.appendSvr2AuthTokenToList(response.authorization.asBasic())
              SvrVersion.SVR3 -> ZonaRosaStore.svr.appendSvr3AuthTokenToList(response.authorization.asBasic())
            }

            AppDependencies.jobManager.add(ResetSvrGuessCountJob())
            stopwatch.split("metadata")

            runBlocking { StorageServiceRestore.restore() }
            stopwatch.split("restore-account")

            if (implementation.svrVersion != SvrVersion.SVR2 && Svr3Migration.shouldWriteToSvr2) {
              AppDependencies.jobManager.add(Svr2MirrorJob())
            }

            if (implementation.svrVersion != SvrVersion.SVR3 && Svr3Migration.shouldWriteToSvr3) {
              AppDependencies.jobManager.add(Svr3MirrorJob())
            }

            stopwatch.stop(TAG)

            return response
          }

          is RestoreResponse.PinMismatch -> {
            Log.i(TAG, "[restoreMasterKeyPostRegistration] Incorrect PIN. $implementation", true)
            return response
          }

          is RestoreResponse.NetworkError -> {
            Log.i(TAG, "[restoreMasterKeyPostRegistration] Network error. $implementation", response.exception, true)
            return response
          }

          is RestoreResponse.ApplicationError -> {
            Log.i(TAG, "[restoreMasterKeyPostRegistration] Application error. $implementation", response.exception, true)
            return response
          }

          RestoreResponse.Missing -> {
            Log.w(TAG, "[restoreMasterKeyPostRegistration] No data found for: $implementation | Continuing to next implementation.", true)
          }

          RestoreResponse.EnclaveNotFound -> {
            Log.w(TAG, "[restoreMasterKeyPostRegistration] Enclave no longer exists: $implementation | Continuing to next implementation.", true)
          }
        }
      }

      Log.w(TAG, "[restoreMasterKeyPostRegistration] No data found for any implementation!", true)
      return RestoreResponse.Missing
    }
  }

  /**
   * Sets the user's PIN to the one specified, updating local stores as necessary.
   * The resulting Single will not throw an error in any expected case, only if there's a runtime exception.
   */
  @WorkerThread
  @JvmStatic
  fun setPin(userPin: String, keyboardType: PinKeyboardType): BackupResponse {
    return operationLock.withLock {
      val masterKey: MasterKey = ZonaRosaStore.svr.masterKey

      val writeTargets = writeImplementations

      val responses: List<BackupResponse> = writeTargets
        .map { it.setPin(userPin, masterKey) }
        .map { it.execute() }

      Log.i(TAG, "[setPin] Responses: $responses", true)

      val error: BackupResponse? = responses.map {
        when (it) {
          is BackupResponse.ApplicationError -> it
          BackupResponse.ExposeFailure -> it
          is BackupResponse.NetworkError -> it
          BackupResponse.ServerRejected -> it
          is BackupResponse.RateLimited -> it
          BackupResponse.EnclaveNotFound -> null
          is BackupResponse.Success -> null
        }
      }.firstOrNull()

      val overallResponse = error
        ?: responses.firstOrNull { it is BackupResponse.Success }
        ?: responses[0]

      if (overallResponse is BackupResponse.Success) {
        Log.i(TAG, "[setPin] Success!", true)

        ZonaRosaStore.svr.setPin(userPin)
        responses
          .filterIsInstance<BackupResponse.Success>()
          .forEach {
            when (it.svrVersion) {
              SvrVersion.SVR2 -> ZonaRosaStore.svr.appendSvr2AuthTokenToList(it.authorization.asBasic())
              SvrVersion.SVR3 -> ZonaRosaStore.svr.appendSvr3AuthTokenToList(it.authorization.asBasic())
            }
          }

        ZonaRosaStore.pin.keyboardType = keyboardType
        ZonaRosaStore.pin.resetPinReminders()

        AppDependencies.megaphoneRepository.markFinished(Megaphones.Event.PINS_FOR_ALL)

        AppDependencies.jobManager.add(RefreshAttributesJob())
      } else {
        Log.w(TAG, "[setPin] Failed to set PIN! $overallResponse", true)

        if (hasNoRegistrationLock) {
          ZonaRosaStore.svr.onPinCreateFailure()
        }
      }

      overallResponse
    }
  }

  /**
   * Invoked after a user has successfully registered. Ensures all the necessary state is updated.
   */
  @WorkerThread
  @JvmStatic
  fun onRegistrationComplete(
    masterKey: MasterKey?,
    userPin: String?,
    hasPinToRestore: Boolean,
    setRegistrationLockEnabled: Boolean,
    restoredAEP: Boolean
  ) {
    Log.i(TAG, "[onRegistrationComplete] Starting", true)
    operationLock.withLock {
      if (masterKey == null && userPin != null) {
        error("If masterKey is present, pin must also be present!")
      }

      if (masterKey != null && userPin != null) {
        if (setRegistrationLockEnabled) {
          Log.i(TAG, "[onRegistrationComplete] Registration Lock", true)
          ZonaRosaStore.svr.isRegistrationLockEnabled = true
        } else {
          Log.i(TAG, "[onRegistrationComplete] ReRegistration Skip SMS", true)
        }

        ZonaRosaStore.svr.masterKeyForInitialDataRestore = masterKey
        ZonaRosaStore.svr.setPin(userPin)
        ZonaRosaStore.pin.resetPinReminders()

        AppDependencies.jobManager.add(ResetSvrGuessCountJob())
      } else if (masterKey != null) {
        Log.i(TAG, "[onRegistrationComplete] ReRegistered with key without pin", true)
        ZonaRosaStore.svr.masterKeyForInitialDataRestore = masterKey
        if (restoredAEP && setRegistrationLockEnabled) {
          Log.i(TAG, "[onRegistrationComplete] Registration Lock", true)
          ZonaRosaStore.svr.isRegistrationLockEnabled = true
        }
      } else if (hasPinToRestore) {
        Log.i(TAG, "[onRegistrationComplete] Has a PIN to restore.", true)
        ZonaRosaStore.svr.clearRegistrationLockAndPin()
        ZonaRosaStore.storageService.needsAccountRestore = true
      } else {
        Log.i(TAG, "[onRegistrationComplete] No registration lock or PIN at all.", true)
        ZonaRosaStore.svr.clearRegistrationLockAndPin()
      }
    }

    AppDependencies.jobManager.add(RefreshAttributesJob())
  }

  /**
   * Invoked when the user skips out on PIN restoration or otherwise fails to remember their PIN.
   */
  @JvmStatic
  fun onPinRestoreForgottenOrSkipped() {
    operationLock.withLock {
      ZonaRosaStore.svr.clearRegistrationLockAndPin()
      ZonaRosaStore.storageService.needsAccountRestore = false
    }
  }

  /**
   * @param rotateAep If true, this will rotate the AEP as part of the process of opting out. Only do this if the user has not enabled backups! If the user
   *    has backups enabled, you should guide them through rotating the AEP first, and then call this with [rotateAep] = false.
   */
  @JvmStatic
  @WorkerThread
  fun optOutOfPin(rotateAep: Boolean) {
    operationLock.withLock {
      ZonaRosaStore.svr.optOut()

      if (rotateAep) {
        ZonaRosaStore.account.rotateAccountEntropyPool(AccountEntropyPool.generate())
        AppDependencies.jobManager.add(MultiDeviceKeysUpdateJob())
      }

      AppDependencies.megaphoneRepository.markFinished(Megaphones.Event.PINS_FOR_ALL)

      bestEffortRefreshAttributes()
      bestEffortForcePushStorage()
    }
  }

  @JvmStatic
  @WorkerThread
  @Throws(IOException::class)
  fun enableRegistrationLockForUserWithPin() {
    operationLock.withLock {
      check(ZonaRosaStore.svr.hasPin() && !ZonaRosaStore.svr.hasOptedOut()) { "Must have a PIN to set a registration lock!" }

      Log.i(TAG, "[enableRegistrationLockForUserWithPin] Enabling registration lock.", true)
      NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.account.enableRegistrationLock(ZonaRosaStore.svr.masterKey.deriveRegistrationLock()))
      ZonaRosaStore.svr.isRegistrationLockEnabled = true
      Log.i(TAG, "[enableRegistrationLockForUserWithPin] Registration lock successfully enabled.", true)
    }
  }

  @JvmStatic
  @WorkerThread
  @Throws(IOException::class)
  fun disableRegistrationLockForUserWithPin() {
    operationLock.withLock {
      check(ZonaRosaStore.svr.hasPin() && !ZonaRosaStore.svr.hasOptedOut()) { "Must have a PIN to disable registration lock!" }

      Log.i(TAG, "[disableRegistrationLockForUserWithPin] Disabling registration lock.", true)
      NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.account.disableRegistrationLock())
      ZonaRosaStore.svr.isRegistrationLockEnabled = false
      Log.i(TAG, "[disableRegistrationLockForUserWithPin] Registration lock successfully disabled.", true)
    }
  }

  /**
   * Fetches new SVR credentials and persists them in the backup store to be used during re-registration.
   */
  @WorkerThread
  @Throws(IOException::class)
  fun refreshAndStoreAuthorization() {
    try {
      var newToken = if (Svr3Migration.shouldWriteToSvr3) {
        val credentials: AuthCredentials = svr3.authorization()
        ZonaRosaStore.svr.appendSvr3AuthTokenToList(credentials.asBasic())
      } else {
        false
      }

      newToken = newToken ||
        if (Svr3Migration.shouldWriteToSvr2) {
          val credentials: AuthCredentials = svr2.authorization()
          ZonaRosaStore.svr.appendSvr2AuthTokenToList(credentials.asBasic())
        } else {
          false
        }

      if (newToken && ZonaRosaStore.svr.hasPin()) {
        BackupManager(AppDependencies.application).dataChanged()
      }
    } catch (e: Throwable) {
      if (e is IOException) {
        throw e
      } else {
        throw IOException(e)
      }
    }
  }

  @WorkerThread
  @VisibleForTesting
  fun restoreMasterKeyPreRegistrationFromV2(svr: SecureValueRecovery, credentials: AuthCredentials?, userPin: String): RestoreResponse {
    return if (credentials == null) {
      RestoreResponse.Missing
    } else {
      svr.restoreDataPreRegistration(credentials, shareSet = null, userPin)
    }
  }

  @WorkerThread
  @VisibleForTesting
  fun restoreMasterKeyPreRegistrationFromV3(credentials: Svr3Credentials?, userPin: String): RestoreResponse {
    return if (credentials?.shareSet == null) {
      RestoreResponse.Missing
    } else {
      svr3.restoreDataPreRegistration(credentials.authCredentials, credentials.shareSet, userPin)
    }
  }

  @WorkerThread
  private fun bestEffortRefreshAttributes() {
    val result = AppDependencies.jobManager.runSynchronously(RefreshAttributesJob(), TimeUnit.SECONDS.toMillis(10))
    if (result.isPresent && result.get() == JobTracker.JobState.SUCCESS) {
      Log.i(TAG, "Attributes were refreshed successfully.", true)
    } else if (result.isPresent) {
      Log.w(TAG, "Attribute refresh finished, but was not successful. Enqueuing one for later. (Result: " + result.get() + ")", true)
      AppDependencies.jobManager.add(RefreshAttributesJob())
    } else {
      Log.w(TAG, "Job did not finish in the allotted time. It'll finish later.", true)
    }
  }

  @WorkerThread
  private fun bestEffortForcePushStorage() {
    val result = AppDependencies.jobManager.runSynchronously(StorageForcePushJob(), TimeUnit.SECONDS.toMillis(10))
    if (result.isPresent && result.get() == JobTracker.JobState.SUCCESS) {
      Log.i(TAG, "Storage was force-pushed successfully.", true)
    } else if (result.isPresent) {
      Log.w(TAG, "Storage force-pushed finished, but was not successful. Enqueuing one for later. (Result: " + result.get() + ")", true)
      AppDependencies.jobManager.add(RefreshAttributesJob())
    } else {
      Log.w(TAG, "Storage fore push did not finish in the allotted time. It'll finish later.", true)
    }
  }

  private val hasNoRegistrationLock: Boolean
    get() {
      return !ZonaRosaStore.svr.isRegistrationLockEnabled &&
        !ZonaRosaStore.svr.hasPin() &&
        !ZonaRosaStore.svr.hasOptedOut()
    }
}

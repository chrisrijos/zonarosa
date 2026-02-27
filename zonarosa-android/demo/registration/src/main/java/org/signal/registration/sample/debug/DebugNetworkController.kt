/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.debug

import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.util.logging.Log
import io.zonarosa.registration.NetworkController
import io.zonarosa.registration.NetworkController.AccountAttributes
import io.zonarosa.registration.NetworkController.BackupMasterKeyError
import io.zonarosa.registration.NetworkController.CheckSvrCredentialsError
import io.zonarosa.registration.NetworkController.CheckSvrCredentialsResponse
import io.zonarosa.registration.NetworkController.CreateSessionError
import io.zonarosa.registration.NetworkController.GetSessionStatusError
import io.zonarosa.registration.NetworkController.GetSvrCredentialsError
import io.zonarosa.registration.NetworkController.MasterKeyResponse
import io.zonarosa.registration.NetworkController.PreKeyCollection
import io.zonarosa.registration.NetworkController.RegisterAccountError
import io.zonarosa.registration.NetworkController.RegisterAccountResponse
import io.zonarosa.registration.NetworkController.RegistrationNetworkResult
import io.zonarosa.registration.NetworkController.RequestVerificationCodeError
import io.zonarosa.registration.NetworkController.RestoreMasterKeyError
import io.zonarosa.registration.NetworkController.SessionMetadata
import io.zonarosa.registration.NetworkController.SetAccountAttributesError
import io.zonarosa.registration.NetworkController.SetRegistrationLockError
import io.zonarosa.registration.NetworkController.SubmitVerificationCodeError
import io.zonarosa.registration.NetworkController.SvrCredentials
import io.zonarosa.registration.NetworkController.UpdateSessionError
import io.zonarosa.registration.NetworkController.VerificationCodeTransport
import java.util.Locale

/**
 * Debug wrapper for NetworkController that allows forcing specific responses.
 *
 * When an override is set for a method via [NetworkDebugState], this controller
 * returns the forced result instead of calling the delegate.
 *
 * This is useful for testing error handling, edge cases, and UI states without
 * needing a real backend connection.
 */
class DebugNetworkController(
  private val delegate: NetworkController
) : NetworkController {

  companion object {
    private val TAG = Log.tag(DebugNetworkController::class)
  }

  override suspend fun createSession(
    e164: String,
    fcmToken: String?,
    mcc: String?,
    mnc: String?
  ): RegistrationNetworkResult<SessionMetadata, CreateSessionError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<SessionMetadata, CreateSessionError>>("createSession")?.let {
      Log.d(TAG, "[createSession] Returning debug override")
      return it
    }
    return delegate.createSession(e164, fcmToken, mcc, mnc)
  }

  override suspend fun getSession(sessionId: String): RegistrationNetworkResult<SessionMetadata, GetSessionStatusError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<SessionMetadata, GetSessionStatusError>>("getSession")?.let {
      Log.d(TAG, "[getSession] Returning debug override")
      return it
    }
    return delegate.getSession(sessionId)
  }

  override suspend fun updateSession(
    sessionId: String?,
    pushChallengeToken: String?,
    captchaToken: String?
  ): RegistrationNetworkResult<SessionMetadata, UpdateSessionError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<SessionMetadata, UpdateSessionError>>("updateSession")?.let {
      Log.d(TAG, "[updateSession] Returning debug override")
      return it
    }
    return delegate.updateSession(sessionId, pushChallengeToken, captchaToken)
  }

  override suspend fun requestVerificationCode(
    sessionId: String,
    locale: Locale?,
    androidSmsRetrieverSupported: Boolean,
    transport: VerificationCodeTransport
  ): RegistrationNetworkResult<SessionMetadata, RequestVerificationCodeError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<SessionMetadata, RequestVerificationCodeError>>("requestVerificationCode")?.let {
      Log.d(TAG, "[requestVerificationCode] Returning debug override")
      return it
    }
    return delegate.requestVerificationCode(sessionId, locale, androidSmsRetrieverSupported, transport)
  }

  override suspend fun submitVerificationCode(
    sessionId: String,
    verificationCode: String
  ): RegistrationNetworkResult<SessionMetadata, SubmitVerificationCodeError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<SessionMetadata, SubmitVerificationCodeError>>("submitVerificationCode")?.let {
      Log.d(TAG, "[submitVerificationCode] Returning debug override")
      return it
    }
    return delegate.submitVerificationCode(sessionId, verificationCode)
  }

  override suspend fun registerAccount(
    e164: String,
    password: String,
    sessionId: String?,
    recoveryPassword: String?,
    attributes: AccountAttributes,
    aciPreKeys: PreKeyCollection,
    pniPreKeys: PreKeyCollection,
    fcmToken: String?,
    skipDeviceTransfer: Boolean
  ): RegistrationNetworkResult<RegisterAccountResponse, RegisterAccountError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<RegisterAccountResponse, RegisterAccountError>>("registerAccount")?.let {
      Log.d(TAG, "[registerAccount] Returning debug override")
      return it
    }
    return delegate.registerAccount(e164, password, sessionId, recoveryPassword, attributes, aciPreKeys, pniPreKeys, fcmToken, skipDeviceTransfer)
  }

  override suspend fun getFcmToken(): String? {
    // No override support for simple value methods
    return delegate.getFcmToken()
  }

  override suspend fun awaitPushChallengeToken(): String? {
    // No override support for simple value methods
    return delegate.awaitPushChallengeToken()
  }

  override fun getCaptchaUrl(): String {
    // No override support for simple value methods
    return delegate.getCaptchaUrl()
  }

  override suspend fun restoreMasterKeyFromSvr(
    svrCredentials: SvrCredentials,
    pin: String
  ): RegistrationNetworkResult<MasterKeyResponse, RestoreMasterKeyError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<MasterKeyResponse, RestoreMasterKeyError>>("restoreMasterKeyFromSvr")?.let {
      Log.d(TAG, "[restoreMasterKeyFromSvr] Returning debug override")
      return it
    }
    return delegate.restoreMasterKeyFromSvr(svrCredentials, pin)
  }

  override suspend fun setPinAndMasterKeyOnSvr(
    pin: String,
    masterKey: MasterKey
  ): RegistrationNetworkResult<SvrCredentials?, BackupMasterKeyError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<SvrCredentials?, BackupMasterKeyError>>("setPinAndMasterKeyOnSvr")?.let {
      Log.d(TAG, "[setPinAndMasterKeyOnSvr] Returning debug override")
      return it
    }
    return delegate.setPinAndMasterKeyOnSvr(pin, masterKey)
  }

  override suspend fun enqueueSvrGuessResetJob() {
    // No override support for simple value methods
    delegate.enqueueSvrGuessResetJob()
  }

  override suspend fun enableRegistrationLock(): RegistrationNetworkResult<Unit, SetRegistrationLockError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<Unit, SetRegistrationLockError>>("enableRegistrationLock")?.let {
      Log.d(TAG, "[enableRegistrationLock] Returning debug override")
      return it
    }
    return delegate.enableRegistrationLock()
  }

  override suspend fun disableRegistrationLock(): RegistrationNetworkResult<Unit, SetRegistrationLockError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<Unit, SetRegistrationLockError>>("disableRegistrationLock")?.let {
      Log.d(TAG, "[disableRegistrationLock] Returning debug override")
      return it
    }
    return delegate.disableRegistrationLock()
  }

  override suspend fun setAccountAttributes(attributes: AccountAttributes): RegistrationNetworkResult<Unit, SetAccountAttributesError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<Unit, SetAccountAttributesError>>("setAccountAttributes")?.let {
      Log.d(TAG, "[setAccountAttributes] Returning debug override")
      return it
    }
    return delegate.setAccountAttributes(attributes)
  }

  override suspend fun getSvrCredentials(): RegistrationNetworkResult<SvrCredentials, GetSvrCredentialsError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<SvrCredentials, GetSvrCredentialsError>>("getSvrCredentials")?.let {
      Log.d(TAG, "[getSvrCredentials] Returning debug override")
      return it
    }
    return delegate.getSvrCredentials()
  }

  override suspend fun checkSvrCredentials(
    e164: String,
    credentials: List<SvrCredentials>
  ): RegistrationNetworkResult<CheckSvrCredentialsResponse, CheckSvrCredentialsError> {
    NetworkDebugState.getOverride<RegistrationNetworkResult<CheckSvrCredentialsResponse, CheckSvrCredentialsError>>("checkSvrCredentials")?.let {
      Log.d(TAG, "[checkSvrCredentials] Returning debug override")
      return it
    }

    return delegate.checkSvrCredentials(e164, credentials)
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.registration

import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.account.AccountAttributes
import io.zonarosa.service.api.account.PreKeyCollection
import io.zonarosa.service.api.messages.multidevice.RegisterAsSecondaryDeviceResponse
import io.zonarosa.service.api.provisioning.RestoreMethod
import io.zonarosa.service.api.push.SignedPreKeyEntity
import io.zonarosa.service.internal.push.BackupV2AuthCheckResponse
import io.zonarosa.service.internal.push.BackupV3AuthCheckResponse
import io.zonarosa.service.internal.push.GcmRegistrationId
import io.zonarosa.service.internal.push.KyberPreKeyEntity
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.push.RegisterAsSecondaryDeviceRequest
import io.zonarosa.service.internal.push.RegistrationSessionMetadataResponse
import io.zonarosa.service.internal.push.VerifyAccountResponse
import java.util.Locale

/**
 * Class to interact with various registration-related endpoints.
 */
class RegistrationApi(
  private val pushServiceSocket: PushServiceSocket
) {

  /**
   * Request that the service initialize a new registration session.
   *
   * `POST /v1/verification/session`
   */
  fun createRegistrationSession(fcmToken: String?, mcc: String?, mnc: String?): NetworkResult<RegistrationSessionMetadataResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.createVerificationSession(fcmToken, mcc, mnc)
    }
  }

  /**
   * Retrieve current status of a registration session.
   *
   * `GET /v1/verification/session/{session-id}`
   */
  fun getRegistrationSessionStatus(sessionId: String): NetworkResult<RegistrationSessionMetadataResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.getSessionStatus(sessionId)
    }
  }

  /**
   * Submit an FCM token to the service as proof that this is an honest user attempting to register.
   *
   * `PATCH /v1/verification/session/{session-id}`
   */
  fun submitPushChallengeToken(sessionId: String?, pushChallengeToken: String?): NetworkResult<RegistrationSessionMetadataResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.patchVerificationSession(sessionId, null, null, null, null, pushChallengeToken)
    }
  }

  /**
   * Request an SMS verification code.  On success, the server will send
   * an SMS verification code to this ZonaRosa user.
   *
   * `POST /v1/verification/session/{session-id}/code`
   *
   * @param androidSmsRetrieverSupported whether the system framework will automatically parse the incoming verification message.
   */
  fun requestSmsVerificationCode(sessionId: String?, locale: Locale?, androidSmsRetrieverSupported: Boolean, transport: PushServiceSocket.VerificationCodeTransport): NetworkResult<RegistrationSessionMetadataResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.requestVerificationCode(sessionId, locale, androidSmsRetrieverSupported, transport)
    }
  }

  /**
   * Submit a verification code sent by the service via one of the supported channels (SMS, phone call) to prove the registrant's control of the phone number.
   *
   * `PUT /v1/verification/session/{session-id}/code`
   */
  fun verifyAccount(sessionId: String, verificationCode: String): NetworkResult<RegistrationSessionMetadataResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.submitVerificationCode(sessionId, verificationCode)
    }
  }

  /**
   * Submits the solved captcha token to the service.
   *
   * `PATCH /v1/verification/session/{session-id}`
   */
  fun submitCaptchaToken(sessionId: String, captchaToken: String): NetworkResult<RegistrationSessionMetadataResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.patchVerificationSession(sessionId, null, null, null, captchaToken, null)
    }
  }

  /**
   * Submit the cryptographic assets required for an account to use the service.
   *
   * `POST /v1/registration`
   */
  fun registerAccount(sessionId: String?, recoveryPassword: String?, attributes: AccountAttributes?, aciPreKeys: PreKeyCollection?, pniPreKeys: PreKeyCollection?, fcmToken: String?, skipDeviceTransfer: Boolean): NetworkResult<VerifyAccountResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.submitRegistrationRequest(sessionId, recoveryPassword, attributes, aciPreKeys, pniPreKeys, fcmToken, skipDeviceTransfer)
    }
  }

  /**
   * Validates the provided SVR2 auth credentials, returning information on their usability.
   *
   * `POST /v2/svr/auth/check`
   */
  fun validateSvr2AuthCredential(e164: String, usernamePasswords: List<String>): NetworkResult<BackupV2AuthCheckResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.checkSvr2AuthCredentials(e164, usernamePasswords)
    }
  }

  /**
   * Validates the provided SVR3 auth credentials, returning information on their usability.
   *
   * `POST /v3/backup/auth/check`
   */
  fun validateSvr3AuthCredential(e164: String, usernamePasswords: List<String>): NetworkResult<BackupV3AuthCheckResponse> {
    return NetworkResult.fromFetch {
      pushServiceSocket.checkSvr3AuthCredentials(e164, usernamePasswords)
    }
  }

  /**
   * Set [RestoreMethod] enum on the server for use by the old device to update UX.
   */
  fun setRestoreMethod(token: String, method: RestoreMethod): NetworkResult<Unit> {
    return NetworkResult.fromFetch {
      pushServiceSocket.setRestoreMethodChosen(token, RestoreMethodBody(method = method))
    }
  }

  /**
   * Registers a device as a linked device on a pre-existing account.
   *
   * `PUT /v1/devices/link`
   *
   * - 403: Incorrect account verification
   * - 409: Device missing required account capability
   * - 411: Account reached max number of linked devices
   * - 422: Request is invalid
   * - 429: Rate limited
   */
  fun registerAsSecondaryDevice(verificationCode: String, attributes: AccountAttributes, aciPreKeys: PreKeyCollection, pniPreKeys: PreKeyCollection, fcmToken: String?): NetworkResult<RegisterAsSecondaryDeviceResponse> {
    val request = RegisterAsSecondaryDeviceRequest(
      verificationCode = verificationCode,
      accountAttributes = attributes,
      aciSignedPreKey = SignedPreKeyEntity(aciPreKeys.signedPreKey.id, aciPreKeys.signedPreKey.keyPair.publicKey, aciPreKeys.signedPreKey.signature),
      pniSignedPreKey = SignedPreKeyEntity(pniPreKeys.signedPreKey.id, pniPreKeys.signedPreKey.keyPair.publicKey, pniPreKeys.signedPreKey.signature),
      aciPqLastResortPreKey = KyberPreKeyEntity(aciPreKeys.lastResortKyberPreKey.id, aciPreKeys.lastResortKyberPreKey.keyPair.publicKey, aciPreKeys.lastResortKyberPreKey.signature),
      pniPqLastResortPreKey = KyberPreKeyEntity(pniPreKeys.lastResortKyberPreKey.id, pniPreKeys.lastResortKyberPreKey.keyPair.publicKey, pniPreKeys.lastResortKyberPreKey.signature),
      gcmToken = fcmToken?.let { GcmRegistrationId(it, true) }
    )

    return NetworkResult.fromFetch {
      pushServiceSocket.registerAsSecondaryDevice(request)
    }
  }
}

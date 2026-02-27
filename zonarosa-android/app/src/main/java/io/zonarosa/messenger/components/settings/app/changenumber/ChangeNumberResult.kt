/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.changenumber

import io.zonarosa.messenger.pin.SvrWrongPinException
import io.zonarosa.messenger.registration.data.network.RegistrationResult
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.SvrNoDataException
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException
import io.zonarosa.service.api.svr.Svr3Credentials
import io.zonarosa.service.internal.push.AuthCredentials
import io.zonarosa.service.internal.push.PushServiceSocket.RegistrationLockFailure
import io.zonarosa.service.internal.push.VerifyAccountResponse

/**
 * This is a processor to map a [VerifyAccountResponse] to all the known outcomes.
 */
sealed class ChangeNumberResult(cause: Throwable?) : RegistrationResult(cause) {
  companion object {
    fun from(networkResult: NetworkResult<ChangeNumberRepository.NumberChangeResult>): ChangeNumberResult {
      return when (networkResult) {
        is NetworkResult.Success -> Success(networkResult.result)
        is NetworkResult.ApplicationError -> UnknownError(networkResult.throwable)
        is NetworkResult.NetworkError -> UnknownError(networkResult.exception)
        is NetworkResult.StatusCodeError -> {
          when (networkResult.code) {
            403 -> IncorrectRecoveryPassword(networkResult.exception)
            401 -> AuthorizationFailed(networkResult.exception)
            400 -> MalformedRequest(networkResult.exception)
            429 -> createRateLimitProcessor(networkResult.exception, networkResult.header("retry-after")?.toLongOrNull())
            423 -> {
              val registrationLockFailure: RegistrationLockFailure? = networkResult.parseJsonBody()
              if (registrationLockFailure != null) {
                RegistrationLocked(cause = networkResult.exception, timeRemaining = registrationLockFailure.timeRemaining, svr2Credentials = registrationLockFailure.svr2Credentials, svr3Credentials = registrationLockFailure.svr3Credentials)
              } else {
                UnknownError(networkResult.exception)
              }
            }
            422 -> ValidationError(networkResult.exception)
            else -> UnknownError(networkResult.exception)
          }
        }
      }
    }

    private fun createRateLimitProcessor(exception: NonSuccessfulResponseCodeException, retryAfter: Long?): ChangeNumberResult {
      return if (retryAfter != null) {
        RateLimited(exception, retryAfter)
      } else {
        AttemptsExhausted(exception)
      }
    }
  }

  class Success(val numberChangeResult: ChangeNumberRepository.NumberChangeResult) : ChangeNumberResult(null)
  class IncorrectRecoveryPassword(cause: Throwable) : ChangeNumberResult(cause)
  class AuthorizationFailed(cause: Throwable) : ChangeNumberResult(cause)
  class MalformedRequest(cause: Throwable) : ChangeNumberResult(cause)
  class ValidationError(cause: Throwable) : ChangeNumberResult(cause)
  class RateLimited(cause: Throwable, val timeRemaining: Long) : ChangeNumberResult(cause)
  class AttemptsExhausted(cause: Throwable) : ChangeNumberResult(cause)
  class RegistrationLocked(cause: Throwable, val timeRemaining: Long, val svr2Credentials: AuthCredentials?, val svr3Credentials: Svr3Credentials?) : ChangeNumberResult(cause)
  class UnknownError(cause: Throwable) : ChangeNumberResult(cause)

  class SvrNoData(cause: SvrNoDataException) : ChangeNumberResult(cause)
  class SvrWrongPin(cause: SvrWrongPinException) : ChangeNumberResult(cause) {
    val triesRemaining = cause.triesRemaining
  }
}

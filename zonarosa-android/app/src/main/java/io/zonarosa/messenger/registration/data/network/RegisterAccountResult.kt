/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data.network

import io.zonarosa.messenger.pin.SvrWrongPinException
import io.zonarosa.messenger.registration.data.AccountRegistrationResult
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.SvrNoDataException
import io.zonarosa.service.api.push.exceptions.AuthorizationFailedException
import io.zonarosa.service.api.push.exceptions.IncorrectRegistrationRecoveryPasswordException
import io.zonarosa.service.api.push.exceptions.MalformedRequestException
import io.zonarosa.service.api.push.exceptions.RateLimitException
import io.zonarosa.service.api.svr.Svr3Credentials
import io.zonarosa.service.internal.push.AuthCredentials
import io.zonarosa.service.internal.push.LockedException
import io.zonarosa.service.internal.push.VerifyAccountResponse

/**
 * This is a processor to map a [VerifyAccountResponse] to all the known outcomes.
 */
sealed class RegisterAccountResult(cause: Throwable?) : RegistrationResult(cause) {
  companion object {
    fun from(networkResult: NetworkResult<AccountRegistrationResult>): RegisterAccountResult {
      return when (networkResult) {
        is NetworkResult.Success -> Success(networkResult.result)
        is NetworkResult.ApplicationError -> UnknownError(networkResult.throwable)
        is NetworkResult.NetworkError -> UnknownError(networkResult.exception)
        is NetworkResult.StatusCodeError -> {
          when (val cause = networkResult.exception) {
            is IncorrectRegistrationRecoveryPasswordException -> IncorrectRecoveryPassword(cause)
            is AuthorizationFailedException -> AuthorizationFailed(cause)
            is MalformedRequestException -> MalformedRequest(cause)
            is RateLimitException -> createRateLimitProcessor(cause)
            is LockedException -> RegistrationLocked(cause = cause, timeRemaining = cause.timeRemaining, svr2Credentials = cause.svr2Credentials, svr3Credentials = cause.svr3Credentials)
            else -> {
              if (networkResult.code == 422) {
                ValidationError(cause)
              } else {
                UnknownError(cause)
              }
            }
          }
        }
      }
    }

    private fun createRateLimitProcessor(exception: RateLimitException): RegisterAccountResult {
      return if (exception.retryAfterMilliseconds.isPresent) {
        RateLimited(exception, exception.retryAfterMilliseconds.get())
      } else {
        AttemptsExhausted(exception)
      }
    }
  }
  class Success(val accountRegistrationResult: AccountRegistrationResult) : RegisterAccountResult(null)
  class IncorrectRecoveryPassword(cause: Throwable) : RegisterAccountResult(cause)
  class AuthorizationFailed(cause: Throwable) : RegisterAccountResult(cause)
  class MalformedRequest(cause: Throwable) : RegisterAccountResult(cause)
  class ValidationError(cause: Throwable) : RegisterAccountResult(cause)
  class RateLimited(cause: Throwable, val timeRemaining: Long) : RegisterAccountResult(cause)
  class AttemptsExhausted(cause: Throwable) : RegisterAccountResult(cause)
  class RegistrationLocked(cause: Throwable, val timeRemaining: Long, val svr2Credentials: AuthCredentials?, val svr3Credentials: Svr3Credentials?) : RegisterAccountResult(cause)
  class UnknownError(cause: Throwable) : RegisterAccountResult(cause)

  class SvrNoData(cause: SvrNoDataException) : RegisterAccountResult(cause)
  class SvrWrongPin(cause: SvrWrongPinException) : RegisterAccountResult(cause) {
    val triesRemaining = cause.triesRemaining
  }
}

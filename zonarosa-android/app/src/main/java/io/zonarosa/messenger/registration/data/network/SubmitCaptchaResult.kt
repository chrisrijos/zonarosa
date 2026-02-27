/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data.network

import io.zonarosa.core.util.logging.Log
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.internal.push.RegistrationSessionMetadataResponse

sealed class SubmitCaptchaResult(cause: Throwable?) : RegistrationResult(cause) {
  companion object {
    private val TAG = Log.tag(SubmitCaptchaResult::class.java)

    fun from(networkResult: NetworkResult<RegistrationSessionMetadataResponse>): SubmitCaptchaResult {
      return when (networkResult) {
        is NetworkResult.Success -> Success()
        is NetworkResult.ApplicationError -> UnknownError(networkResult.throwable)
        is NetworkResult.NetworkError -> UnknownError(networkResult.exception)
        is NetworkResult.StatusCodeError -> UnknownError(networkResult.exception)
      }
    }
  }

  class Success : SubmitCaptchaResult(null)
  class ChallengeRequired(val challenges: List<String>) : SubmitCaptchaResult(null)
  class UnknownError(cause: Throwable) : SubmitCaptchaResult(cause)
}

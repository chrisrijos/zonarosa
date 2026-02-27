/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data.network

import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.svr.Svr3Credentials
import io.zonarosa.service.internal.push.AuthCredentials
import io.zonarosa.service.internal.push.BackupV2AuthCheckResponse
import io.zonarosa.service.internal.push.BackupV3AuthCheckResponse

/**
 * This is a processor to map a [BackupV2AuthCheckResponse] to all the known outcomes.
 */
sealed class BackupAuthCheckResult(cause: Throwable?) : RegistrationResult(cause) {
  companion object {
    @JvmStatic
    fun fromV2(networkResult: NetworkResult<BackupV2AuthCheckResponse>): BackupAuthCheckResult {
      return when (networkResult) {
        is NetworkResult.Success -> {
          val match = networkResult.result.match
          if (match != null) {
            SuccessWithCredentials(svr2Credentials = match, svr3Credentials = null)
          } else {
            SuccessWithoutCredentials()
          }
        }

        is NetworkResult.ApplicationError -> UnknownError(networkResult.throwable)
        is NetworkResult.NetworkError -> UnknownError(networkResult.exception)
        is NetworkResult.StatusCodeError -> UnknownError(networkResult.exception)
      }
    }

    @JvmStatic
    fun fromV3(networkResult: NetworkResult<BackupV3AuthCheckResponse>): BackupAuthCheckResult {
      return when (networkResult) {
        is NetworkResult.Success -> {
          val match = networkResult.result.match
          if (match != null) {
            SuccessWithCredentials(svr2Credentials = null, svr3Credentials = match)
          } else {
            SuccessWithoutCredentials()
          }
        }

        is NetworkResult.ApplicationError -> UnknownError(networkResult.throwable)
        is NetworkResult.NetworkError -> UnknownError(networkResult.exception)
        is NetworkResult.StatusCodeError -> UnknownError(networkResult.exception)
      }
    }
  }

  class SuccessWithCredentials(val svr2Credentials: AuthCredentials?, val svr3Credentials: Svr3Credentials?) : BackupAuthCheckResult(null)

  class SuccessWithoutCredentials : BackupAuthCheckResult(null)

  class UnknownError(cause: Throwable) : BackupAuthCheckResult(cause)
}

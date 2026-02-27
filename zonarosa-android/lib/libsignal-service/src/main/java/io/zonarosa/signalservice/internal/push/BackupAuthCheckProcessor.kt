/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push

import io.zonarosa.service.internal.ServiceResponse
import io.zonarosa.service.internal.ServiceResponseProcessor

/**
 * Processes a response from the verify stored KBS auth credentials request.
 */
class BackupAuthCheckProcessor(response: ServiceResponse<BackupV2AuthCheckResponse>) : ServiceResponseProcessor<BackupV2AuthCheckResponse>(response) {
  fun getInvalid(): List<String> {
    return response.result.map { it.invalid }.orElse(emptyList())
  }

  fun hasValidSvr2AuthCredential(): Boolean {
    return response.result.map { it.match }.orElse(null) != null
  }

  fun requireSvr2AuthCredential(): AuthCredentials {
    return response.result.get().match!!
  }
}

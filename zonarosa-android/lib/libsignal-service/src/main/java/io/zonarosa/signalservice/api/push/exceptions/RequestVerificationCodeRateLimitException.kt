/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.push.exceptions

import io.zonarosa.service.internal.push.RegistrationSessionMetadataResponse

/**
 * Rate limit exception specific to requesting a verification code for registration.
 */
class RequestVerificationCodeRateLimitException(
  val sessionMetadata: RegistrationSessionMetadataResponse
) : NonSuccessfulResponseCodeException(429, "User request verification code rate limited")

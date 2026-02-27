/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.push.exceptions

import io.zonarosa.service.internal.push.RegistrationSessionMetadataResponse

/**
 * Rate limit exception specific to submitting verification codes during registration.
 */
class SubmitVerificationCodeRateLimitException(
  val sessionMetadata: RegistrationSessionMetadataResponse
) : NonSuccessfulResponseCodeException(429, "User submit verification code rate limited")

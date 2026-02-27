/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.push.exceptions

import io.zonarosa.service.internal.push.RegistrationSessionMetadataResponse

/**
 * We tried to do something on registration endpoints that didn't go well, so now we have to do a challenge. And not a
 * fun one involving ice buckets.
 */
class ChallengeRequiredException(val response: RegistrationSessionMetadataResponse) : NonSuccessfulResponseCodeException(409)

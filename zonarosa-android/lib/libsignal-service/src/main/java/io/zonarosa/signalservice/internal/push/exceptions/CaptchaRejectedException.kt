/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.push.exceptions

import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException

/**
 * Indicates that the captcha we submitted was not accepted by the server.
 */
class CaptchaRejectedException : NonSuccessfulResponseCodeException(428, "Captcha rejected by server.")

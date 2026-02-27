/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.link

import kotlin.time.Duration

sealed interface RegisterLinkDeviceResult {
  data object Success : RegisterLinkDeviceResult
  data object IncorrectVerification : RegisterLinkDeviceResult
  data object MissingCapability : RegisterLinkDeviceResult
  data object MaxLinkedDevices : RegisterLinkDeviceResult
  data object InvalidRequest : RegisterLinkDeviceResult
  data class RateLimited(val retryAfter: Duration?) : RegisterLinkDeviceResult
  data class NetworkException(val t: Throwable) : RegisterLinkDeviceResult
  data class UnexpectedException(val t: Throwable) : RegisterLinkDeviceResult
}

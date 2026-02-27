/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.screens.util

import io.zonarosa.registration.RegistrationFlowEvent
import io.zonarosa.registration.RegistrationRoute

/**
 * Convenience function to emit a navigation event to a parentEmitter.
 */
fun ((RegistrationFlowEvent) -> Unit).navigateTo(route: RegistrationRoute) {
  this(RegistrationFlowEvent.NavigateToScreen(route))
}

/**
 * Convenience function to emit a navigate-back event to a parentEmitter.
 */
fun ((RegistrationFlowEvent) -> Unit).navigateBack() {
  this(RegistrationFlowEvent.NavigateBack)
}

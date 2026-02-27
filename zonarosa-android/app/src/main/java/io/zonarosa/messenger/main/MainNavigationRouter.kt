/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.main

interface MainNavigationRouter {
  fun goTo(location: MainNavigationDetailLocation)

  fun goTo(location: MainNavigationListLocation)
}

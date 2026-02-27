/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.main

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

fun NavGraphBuilder.storiesNavGraphBuilder() {
  composable<MainNavigationDetailLocation.Empty> {
    EmptyDetailScreen()
  }
}

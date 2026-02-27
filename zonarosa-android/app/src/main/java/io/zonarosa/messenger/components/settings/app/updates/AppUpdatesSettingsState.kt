/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.updates

import kotlin.time.Duration

data class AppUpdatesSettingsState(
  val lastCheckedTime: Duration,
  val autoUpdateEnabled: Boolean
)

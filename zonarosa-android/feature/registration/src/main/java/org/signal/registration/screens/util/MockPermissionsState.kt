/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

@file:OptIn(ExperimentalPermissionsApi::class)

package io.zonarosa.registration.screens.util

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus

/**
 * Helpful mock for [PermissionsState] to make previews easier.
 */
class MockPermissionsState(
  override val permission: String,
  override val status: PermissionStatus = PermissionStatus.Granted
) : PermissionState {
  override fun launchPermissionRequest() = Unit
}

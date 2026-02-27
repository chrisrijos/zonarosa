/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

@file:OptIn(ExperimentalPermissionsApi::class)

package io.zonarosa.registration.screens.util

import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState

/**
 * Helpful mock for [MultiplePermissionsState] to make previews easier.
 */
class MockMultiplePermissionsState(
  override val allPermissionsGranted: Boolean = false,
  override val permissions: List<PermissionState> = emptyList(),
  override val revokedPermissions: List<PermissionState> = emptyList(),
  override val shouldShowRationale: Boolean = false
) : MultiplePermissionsState {
  override fun launchMultiplePermissionRequest() = Unit
}

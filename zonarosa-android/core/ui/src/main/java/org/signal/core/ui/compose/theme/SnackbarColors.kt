/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.ui.compose.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Borrowed from [androidx.compose.material3.Snackbar]
 *
 * Works in conjunction with [io.zonarosa.core.ui.Snackbars] for properly
 * themed snackbars in light and dark modes.
 */
@Immutable
data class SnackbarColors(
  val color: Color,
  val contentColor: Color,
  val actionColor: Color,
  val actionContentColor: Color,
  val dismissActionContentColor: Color
)

val LocalSnackbarColors = staticCompositionLocalOf {
  SnackbarColors(
    color = Color.Unspecified,
    contentColor = Color.Unspecified,
    actionColor = Color.Unspecified,
    actionContentColor = Color.Unspecified,
    dismissActionContentColor = Color.Unspecified
  )
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.mediasend.edit

import androidx.compose.runtime.Composable

/**
 * Allows user to perform actions while viewing an editable image.
 */
@Composable
fun ImageEditorTopLevelToolbar(
  imageEditorController: ImageEditorController
) {
  // Draw -- imageEditorController draw mode
  // Crop&Rotate -- imageEditorController crop mode
  // Quality -- callback toggle quality
  // Save -- callback save to disk
  // Add -- callback go to media select
}

interface ImageEditorToolbarsCallback {

  object Empty : ImageEditorToolbarsCallback
}

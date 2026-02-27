/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.mediasend.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.viewinterop.AndroidView
import io.zonarosa.imageeditor.core.ImageEditorView

@Composable
fun ImageEditor(
  controller: ImageEditorController,
  modifier: Modifier = Modifier
) {
  AndroidView(
    factory = { context -> ImageEditorView(context) },
    update = { view ->
      view.model = controller.editorModel
      view.mode = mapMode(controller.mode)
    },
    onReset = { },
    modifier = modifier.clipToBounds()
  )
}

private fun mapMode(mode: ImageEditorController.Mode): ImageEditorView.Mode {
  return when (mode) {
    ImageEditorController.Mode.NONE -> ImageEditorView.Mode.MoveAndResize
    ImageEditorController.Mode.CROP -> ImageEditorView.Mode.MoveAndResize
    ImageEditorController.Mode.TEXT -> ImageEditorView.Mode.MoveAndResize
    ImageEditorController.Mode.DRAW -> ImageEditorView.Mode.Draw
    ImageEditorController.Mode.HIGHLIGHT -> ImageEditorView.Mode.Draw
    ImageEditorController.Mode.BLUR -> ImageEditorView.Mode.Blur
    ImageEditorController.Mode.MOVE_STICKER -> ImageEditorView.Mode.MoveAndResize
    ImageEditorController.Mode.MOVE_TEXT -> ImageEditorView.Mode.MoveAndResize
    ImageEditorController.Mode.DELETE -> ImageEditorView.Mode.MoveAndResize
    ImageEditorController.Mode.INSERT_STICKER -> ImageEditorView.Mode.MoveAndResize
  }
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.mediasend.edit

import androidx.compose.runtime.Stable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.zonarosa.imageeditor.core.model.EditorModel

@Stable
class ImageEditorController @RememberInComposition constructor(
  val editorModel: EditorModel
) {

  var mode: Mode by mutableStateOf(Mode.NONE)

  enum class Mode {
    NONE,
    CROP,
    TEXT,
    DRAW,
    HIGHLIGHT,
    BLUR,
    MOVE_STICKER,
    MOVE_TEXT,
    DELETE,
    INSERT_STICKER
  }
}

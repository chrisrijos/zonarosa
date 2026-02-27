/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.internal.conversation.springboard

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class InternalConversationSpringboardViewModel : ViewModel() {
  val hasWallpaper = mutableStateOf(false)
}

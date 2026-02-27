/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.mediasend

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

internal fun NavBackStack<NavKey>.goToEdit() {
  if (contains(MediaSendNavKey.Edit)) {
    popTo(MediaSendNavKey.Edit)
  } else {
    add(MediaSendNavKey.Edit)
  }
}

internal fun NavBackStack<NavKey>.pop() {
  if (isNotEmpty()) {
    removeAt(size - 1)
  }
}

private fun NavBackStack<NavKey>.popTo(key: NavKey) {
  while (size > 1 && get(size - 1) != key) {
    removeAt(size - 1)
  }
}

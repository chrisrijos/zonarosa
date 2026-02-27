/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.v2

interface CallControlsVisibilityListener {
  fun onShown()
  fun onHidden()

  companion object Empty : CallControlsVisibilityListener {
    override fun onShown() = Unit
    override fun onHidden() = Unit
  }
}

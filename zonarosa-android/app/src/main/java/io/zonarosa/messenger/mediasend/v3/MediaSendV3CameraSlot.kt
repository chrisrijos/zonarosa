/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.mediasend.v3

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.fragment.compose.AndroidFragment
import io.zonarosa.messenger.mediasend.CameraFragment

/**
 * Displays the proper camera capture ui
 */
@Composable
fun MediaSendV3CameraSlot() {
  val fragmentClass = remember {
    CameraFragment.getFragmentClass()
  }

  AndroidFragment(
    clazz = fragmentClass,
    modifier = Modifier.fillMaxSize()
  )
}

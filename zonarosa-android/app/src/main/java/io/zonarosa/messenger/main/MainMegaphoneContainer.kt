/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.main

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.fragment.app.DialogFragment
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.isHeightCompact
import io.zonarosa.messenger.megaphone.Megaphone
import io.zonarosa.messenger.megaphone.MegaphoneActionController
import io.zonarosa.messenger.megaphone.MegaphoneComponent
import io.zonarosa.messenger.megaphone.Megaphones

data class MainMegaphoneState(
  val megaphone: Megaphone = Megaphone.NONE,
  val mainToolbarMode: MainToolbarMode = MainToolbarMode.FULL
)

object EmptyMegaphoneActionController : MegaphoneActionController {
  override fun onMegaphoneNavigationRequested(intent: Intent) = Unit
  override fun onMegaphoneNavigationRequested(intent: Intent, requestCode: Int) = Unit
  override fun onMegaphoneToastRequested(string: String) = Unit
  override fun getMegaphoneActivity(): Activity = error("Empty controller")
  override fun onMegaphoneSnooze(event: Megaphones.Event) = Unit
  override fun onMegaphoneCompleted(event: Megaphones.Event) = Unit
  override fun onMegaphoneDialogFragmentRequested(dialogFragment: DialogFragment) = Unit
}

/**
 * Composable wrapper for Megaphones
 */
@Composable
fun MainMegaphoneContainer(
  state: MainMegaphoneState,
  controller: MegaphoneActionController,
  onMegaphoneVisible: (Megaphone) -> Unit
) {
  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
  val visible = remember(windowSizeClass, state) {
    !(state.megaphone == Megaphone.NONE || state.mainToolbarMode != MainToolbarMode.FULL || windowSizeClass.isHeightCompact)
  }

  AnimatedVisibility(visible = visible) {
    MegaphoneComponent(
      megaphone = state.megaphone,
      megaphoneActionController = controller
    )
  }

  LaunchedEffect(state, windowSizeClass) {
    if (state.megaphone == Megaphone.NONE || state.mainToolbarMode == MainToolbarMode.BASIC || windowSizeClass.isHeightCompact) {
      return@LaunchedEffect
    }

    onMegaphoneVisible(state.megaphone)
  }
}

@DayNightPreviews
@Composable
private fun MainMegaphoneContainerPreview() {
  Previews.Preview {
    MainMegaphoneContainer(
      state = MainMegaphoneState(
        megaphone = Megaphone.Builder(Megaphones.Event.ONBOARDING, Megaphone.Style.ONBOARDING).build()
      ),
      controller = EmptyMegaphoneActionController,
      onMegaphoneVisible = {}
    )
  }
}

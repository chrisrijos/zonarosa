/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.main

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.zonarosa.core.ui.compose.AllDevicePreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.Snackbars
import io.zonarosa.core.ui.compose.showSnackbar
import io.zonarosa.core.ui.isSplitPane
import io.zonarosa.messenger.components.snackbars.SnackbarHostKey
import io.zonarosa.messenger.components.snackbars.rememberSnackbarState
import io.zonarosa.messenger.megaphone.Megaphone
import io.zonarosa.messenger.megaphone.MegaphoneActionController
import io.zonarosa.messenger.megaphone.Megaphones
import io.zonarosa.messenger.window.NavigationType

interface MainBottomChromeCallback : MainFloatingActionButtonsCallback {
  fun onMegaphoneVisible(megaphone: Megaphone)
  fun onSnackbarDismissed()

  object Empty : MainBottomChromeCallback {
    override fun onNewChatClick() = Unit
    override fun onNewCallClick() = Unit
    override fun onCameraClick(destination: MainNavigationListLocation) = Unit
    override fun onMegaphoneVisible(megaphone: Megaphone) = Unit
    override fun onSnackbarDismissed() = Unit
  }
}

data class MainBottomChromeState(
  val destination: MainNavigationListLocation = MainNavigationListLocation.CHATS,
  val megaphoneState: MainMegaphoneState = MainMegaphoneState(),
  val mainToolbarMode: MainToolbarMode = MainToolbarMode.FULL
)

/**
 * Stack of bottom chrome components:
 * - The Floating Action buttons
 * - The megaphone view
 * - The snackbar
 */
@Composable
fun MainBottomChrome(
  state: MainBottomChromeState,
  callback: MainBottomChromeCallback,
  megaphoneActionController: MegaphoneActionController,
  modifier: Modifier = Modifier
) {
  val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
  val navigationType = NavigationType.rememberNavigationType()

  Column(
    modifier = modifier
      .fillMaxWidth()
      .animateContentSize()
  ) {
    if (state.mainToolbarMode == MainToolbarMode.FULL && navigationType != NavigationType.RAIL) {
      Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier.fillMaxWidth()
      ) {
        MainFloatingActionButtons(
          destination = state.destination,
          callback = callback
        )
      }
    }

    if (state.mainToolbarMode == MainToolbarMode.FULL) {
      MainMegaphoneContainer(
        state = state.megaphoneState,
        controller = megaphoneActionController,
        onMegaphoneVisible = callback::onMegaphoneVisible
      )
    }

    if (windowSizeClass.isSplitPane()) {
      return@Column
    }

    val snackBarModifier = if (state.mainToolbarMode == MainToolbarMode.BASIC) {
      Modifier.navigationBarsPadding()
    } else {
      Modifier
    }

    MainSnackbar(
      onDismissed = callback::onSnackbarDismissed,
      modifier = snackBarModifier
    )
  }
}

@Composable
fun MainSnackbar(
  onDismissed: () -> Unit,
  modifier: Modifier = Modifier,
  hostKey: SnackbarHostKey = MainSnackbarHostKey.MainChrome
) {
  val hostState = remember { SnackbarHostState() }
  val stateHolder = rememberSnackbarState(hostKey)
  val snackbarState = stateHolder.value

  Snackbars.Host(
    hostState,
    modifier = modifier
  )

  LaunchedEffect(snackbarState) {
    if (snackbarState != null) {
      val result = hostState.showSnackbar(
        message = snackbarState.message,
        actionLabel = snackbarState.actionState?.action,
        duration = snackbarState.duration
      )

      when (result) {
        SnackbarResult.Dismissed -> Unit
        SnackbarResult.ActionPerformed -> snackbarState.actionState?.onActionClick?.invoke()
      }

      stateHolder.clear()
      onDismissed()
    }
  }
}

@AllDevicePreviews
@Composable
fun MainBottomChromePreview() {
  Previews.Preview {
    val megaphone = remember {
      Megaphone.Builder(Megaphones.Event.ONBOARDING, Megaphone.Style.ONBOARDING).build()
    }

    Box(
      contentAlignment = Alignment.BottomCenter,
      modifier = Modifier.fillMaxSize()
    ) {
      MainBottomChrome(
        state = MainBottomChromeState(
          megaphoneState = MainMegaphoneState(
            megaphone = megaphone
          )
        ),
        callback = MainBottomChromeCallback.Empty,
        megaphoneActionController = EmptyMegaphoneActionController
      )
    }
  }
}

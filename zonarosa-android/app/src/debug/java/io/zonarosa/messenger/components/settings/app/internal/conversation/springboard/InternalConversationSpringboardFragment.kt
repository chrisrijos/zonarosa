/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.internal.conversation.springboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.zonarosa.core.ui.compose.ComposeFragment
import io.zonarosa.core.ui.compose.NightPreview
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.Rows
import io.zonarosa.core.ui.compose.Scaffolds
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.messenger.R

/**
 * Configuration fragment for the internal conversation test fragment.
 */
class InternalConversationSpringboardFragment : ComposeFragment() {

  private val viewModel: InternalConversationSpringboardViewModel by navGraphViewModels(R.id.app_settings)

  @Composable
  override fun FragmentContent() {
    Content(this::navigateBack, this::launchTestFragment, viewModel.hasWallpaper)
  }

  private fun navigateBack() {
    findNavController().popBackStack()
  }

  private fun launchTestFragment() {
    findNavController().navigate(
      InternalConversationSpringboardFragmentDirections
        .actionInternalConversationSpringboardFragmentToInternalConversationTestFragment()
    )
  }
}

@NightPreview
@Composable
private fun ContentPreview() {
  val hasWallpaper = remember { mutableStateOf(false) }

  Previews.Preview {
    Content(onBackPressed = {}, onLaunchTestFragment = {}, hasWallpaper = hasWallpaper)
  }
}

@Composable
private fun Content(
  onBackPressed: () -> Unit,
  onLaunchTestFragment: () -> Unit,
  hasWallpaper: MutableState<Boolean>
) {
  Scaffolds.Settings(
    title = "Conversation Test Springboard",
    onNavigationClick = onBackPressed,
    navigationIcon = ZonaRosaIcons.ArrowStart.imageVector
  ) {
    Column(modifier = Modifier.padding(it)) {
      Rows.TextRow(
        text = "Launch Conversation Test Fragment",
        onClick = onLaunchTestFragment
      )

      Rows.ToggleRow(
        checked = hasWallpaper.value,
        text = "Enable Wallpaper",
        onCheckChanged = { hasWallpaper.value = it }
      )
    }
  }
}

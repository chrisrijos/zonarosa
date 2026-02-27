/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import io.zonarosa.core.ui.compose.IconButtons
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.messenger.R

/**
 * A consistent ActionMode top-bar for dealing with multiselect scenarios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionModeTopBar(
  title: String,
  onCloseClick: () -> Unit,
  toolbarColor: Color? = null,
  windowInsets: WindowInsets = TopAppBarDefaults.windowInsets
) {
  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = toolbarColor ?: MaterialTheme.colorScheme.surface
    ),
    navigationIcon = {
      IconButtons.IconButton(onClick = onCloseClick) {
        Icon(
          imageVector = ZonaRosaIcons.X.imageVector,
          contentDescription = stringResource(R.string.CallScreenTopBar__go_back)
        )
      }
    },
    title = {
      Text(text = title)
    },
    windowInsets = windowInsets
  )
}

@PreviewLightDark
@Composable
fun ActionModeTopBarPreview() {
  Previews.Preview {
    ActionModeTopBar(
      title = "1 selected",
      onCloseClick = {}
    )
  }
}

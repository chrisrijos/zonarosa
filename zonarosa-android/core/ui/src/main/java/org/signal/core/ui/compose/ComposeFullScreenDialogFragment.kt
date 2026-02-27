/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import io.zonarosa.core.ui.R
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme
import io.zonarosa.core.ui.initializeScreenshotSecurity
import io.zonarosa.core.ui.util.ThemeUtil

/**
 * Generic Compose-based full screen dialog fragment.
 *
 * Expects [R.attr.fullScreenDialogStyle] to be defined in your app theme, pointing to a style
 * suitable for full screen dialogs.
 */
abstract class ComposeFullScreenDialogFragment : DialogFragment() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val fullScreenDialogStyle = ThemeUtil.getThemedResourceId(requireContext(), R.attr.fullScreenDialogStyle)
    setStyle(STYLE_NO_FRAME, fullScreenDialogStyle)
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        ZonaRosaTheme {
          DialogContent()
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    requireDialog().window?.initializeScreenshotSecurity()
  }

  @Composable
  abstract fun DialogContent()
}

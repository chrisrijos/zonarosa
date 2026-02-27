/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.ui.compose

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.FixedRoundedCornerBottomSheetDialogFragment
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme

abstract class ComposeBottomSheetDialogFragment : FixedRoundedCornerBottomSheetDialogFragment() {

  protected open val forceDarkTheme = false

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return ComposeView(requireContext()).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val isDark = if (forceDarkTheme) {
          true
        } else {
          LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }
        ZonaRosaTheme(isDarkMode = isDark) {
          Surface(
            shape = RoundedCornerShape(cornerRadius.dp, cornerRadius.dp),
            color = ZonaRosaTheme.colors.colorSurface1,
            contentColor = MaterialTheme.colorScheme.onSurface
          ) {
            SheetContent()
          }
        }
      }
    }
  }

  @Composable
  abstract fun SheetContent()
}

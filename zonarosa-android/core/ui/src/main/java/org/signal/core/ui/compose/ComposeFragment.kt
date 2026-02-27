/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.ui.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.fragment.compose.content
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme
import io.zonarosa.core.ui.logging.LoggingFragment

/**
 * Generic ComposeFragment which can be subclassed to build UI with compose.
 */
abstract class ComposeFragment : LoggingFragment() {
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = content {
    ZonaRosaTheme {
      FragmentContent()
    }
  }

  @Composable
  abstract fun FragmentContent()
}

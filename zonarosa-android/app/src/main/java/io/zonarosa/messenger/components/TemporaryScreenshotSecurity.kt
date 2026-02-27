/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components

import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.zonarosa.messenger.util.ZonaRosaPreferences

/**
 * Applies temporary screenshot security for the given component lifecycle.
 */
object TemporaryScreenshotSecurity {

  @JvmStatic
  fun bindToViewLifecycleOwner(fragment: Fragment) {
    val observer = LifecycleObserver { fragment.requireActivity() }

    fragment.viewLifecycleOwner.lifecycle.addObserver(observer)
  }

  @JvmStatic
  fun bind(activity: ComponentActivity) {
    val observer = LifecycleObserver { activity }

    activity.lifecycle.addObserver(observer)
  }

  private class LifecycleObserver(
    private val activityProvider: () -> ComponentActivity
  ) : DefaultLifecycleObserver {
    override fun onResume(owner: LifecycleOwner) {
      val activity = activityProvider()
      if (!ZonaRosaPreferences.isScreenSecurityEnabled(activity)) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
    }

    override fun onPause(owner: LifecycleOwner) {
      val activity = activityProvider()
      if (!ZonaRosaPreferences.isScreenSecurityEnabled(activity)) {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
    }
  }
}

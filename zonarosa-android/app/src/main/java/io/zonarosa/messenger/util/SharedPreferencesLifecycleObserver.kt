/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.util

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * A lifecycle-aware observer that will let the changes to the [ZonaRosaPreferences] be observed.
 *
 * @param keysToListeners a map of [ZonaRosaPreferences] string keys to listeners that should be invoked when the values change.
 */
class SharedPreferencesLifecycleObserver(private val context: Context, keysToListeners: Map<String, () -> Unit>) : DefaultLifecycleObserver {

  private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
    keysToListeners[key]?.invoke()
  }

  override fun onResume(owner: LifecycleOwner) {
    ZonaRosaPreferences.registerListener(context, listener)
  }

  override fun onPause(owner: LifecycleOwner) {
    ZonaRosaPreferences.unregisterListener(context, listener)
  }
}

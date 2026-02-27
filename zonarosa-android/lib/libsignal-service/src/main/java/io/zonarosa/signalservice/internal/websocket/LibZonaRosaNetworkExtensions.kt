/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
@file:JvmName("LibZonaRosaNetworkExtensions")

package io.zonarosa.service.internal.websocket

import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.orNull
import io.zonarosa.libzonarosa.net.Network
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import java.io.IOException

private const val TAG = "LibZonaRosaNetworkExtensions"

/**
 * Helper method to apply settings from the ZonaRosaServiceConfiguration.
 */
fun Network.applyConfiguration(config: ZonaRosaServiceConfiguration) {
  val zonarosaProxy = config.zonarosaProxy.orNull()
  val systemHttpProxy = config.systemHttpProxy.orNull()

  when {
    (zonarosaProxy != null) -> {
      try {
        this.setProxy(zonarosaProxy.host, zonarosaProxy.port)
      } catch (e: IOException) {
        Log.e(TAG, "Invalid proxy configuration set! Failing connections until changed.")
        this.setInvalidProxy()
      }
    }
    (systemHttpProxy != null) -> {
      try {
        this.setProxy("http", systemHttpProxy.host, systemHttpProxy.port, "", "")
      } catch (e: IOException) {
        // The Android settings screen where this is set explicitly calls out that apps are allowed to
        //  ignore the HTTP Proxy setting, so if using the specified proxy would cause us to break, let's
        //  try just ignoring it and seeing if that still lets us connect.
        Log.w(TAG, "Failed to set system HTTP proxy, ignoring and continuing...")
      }
    }
  }

  this.setCensorshipCircumventionEnabled(config.censored)
}

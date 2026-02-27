/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.glide

import android.net.Uri
import io.zonarosa.core.util.logging.Log
import io.zonarosa.glide.common.io.InputStreamFactory
import io.zonarosa.messenger.dependencies.AppDependencies
import java.io.InputStream

/**
 * A factory that creates a new [InputStream] for the given [Uri] each time [create] is called.
 */
class DecryptableStreamFactory(
  private val uri: Uri
) : InputStreamFactory {
  companion object {
    private val TAG = Log.tag(DecryptableStreamFactory::class)
  }

  override fun create(): InputStream {
    return try {
      DecryptableStreamLocalUriFetcher(AppDependencies.application, uri).loadResource(uri, AppDependencies.application.contentResolver)
    } catch (e: Exception) {
      Log.w(TAG, "Error creating input stream for URI.", e)
      throw e
    }
  }
}

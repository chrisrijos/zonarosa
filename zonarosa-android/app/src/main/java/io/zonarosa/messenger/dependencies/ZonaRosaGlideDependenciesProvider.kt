/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.dependencies

import android.net.Uri
import io.zonarosa.glide.ZonaRosaGlideDependencies
import io.zonarosa.glide.common.io.InputStreamFactory
import io.zonarosa.messenger.glide.DecryptableStreamFactory

object ZonaRosaGlideDependenciesProvider : ZonaRosaGlideDependencies.Provider {
  override fun getUriInputStreamFactory(uri: Uri): InputStreamFactory {
    return DecryptableStreamFactory(uri)
  }
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.dependencies

import io.zonarosa.core.util.CoreUtilDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.RemoteDeprecation

object CoreUtilDependenciesProvider : CoreUtilDependencies.Provider {
  override fun provideIsClientDeprecated(): Boolean {
    return ZonaRosaStore.misc.isClientDeprecated
  }

  override fun provideTimeUntilRemoteDeprecation(currentTime: Long): Long {
    return RemoteDeprecation.getTimeUntilDeprecation(currentTime)
  }
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.ui

import android.app.Application
import org.junit.rules.ExternalResource

/**
 * Since tests that depend on [io.zonarosa.core.ui.compose.theme.ZonaRosaTheme] need
 * [CoreUiDependencies] to be initialized, this rule provides a convenient way to do so.
 */
class CoreUiDependenciesRule(
  private val application: Application,
  private val isIncognitoKeyboardEnabled: Boolean = false
) : ExternalResource() {
  override fun before() {
    CoreUiDependencies.init(application, Provider(isIncognitoKeyboardEnabled))
  }

  private class Provider(
    val isIncognitoKeyboardEnabled: Boolean
  ) : CoreUiDependencies.Provider {
    override fun providePackageId(): String = "io.zonarosa.messenger"
    override fun provideIsIncognitoKeyboardEnabled(): Boolean = isIncognitoKeyboardEnabled
    override fun provideIsScreenSecurityEnabled(): Boolean = false
    override fun provideForceSplitPane(): Boolean = false
  }
}

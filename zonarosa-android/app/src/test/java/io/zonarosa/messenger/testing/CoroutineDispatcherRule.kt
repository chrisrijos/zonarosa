/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.testing

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.rules.ExternalResource
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers

/**
 * Rule that allows for injection of test dispatchers when operating with ViewModels.
 */
class CoroutineDispatcherRule(
  defaultDispatcher: TestDispatcher,
  mainDispatcher: TestDispatcher = defaultDispatcher,
  ioDispatcher: TestDispatcher = defaultDispatcher,
  unconfinedDispatcher: TestDispatcher = defaultDispatcher
) : ExternalResource() {

  private val testDispatcherProvider = TestDispatcherProvider(
    main = mainDispatcher,
    io = ioDispatcher,
    default = defaultDispatcher,
    unconfined = unconfinedDispatcher
  )

  override fun before() {
    ZonaRosaDispatchers.setDispatcherProvider(testDispatcherProvider)
  }

  override fun after() {
    ZonaRosaDispatchers.setDispatcherProvider()
  }

  private class TestDispatcherProvider(
    override val main: CoroutineDispatcher,
    override val io: CoroutineDispatcher,
    override val default: CoroutineDispatcher,
    override val unconfined: CoroutineDispatcher
  ) : ZonaRosaDispatchers.DispatcherProvider
}

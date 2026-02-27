/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.testing

import androidx.test.platform.app.InstrumentationRegistry
import io.mockk.every
import org.junit.rules.ExternalResource
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.util.JsonUtils
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.internal.push.SubscriptionsConfiguration

/**
 * Sets up some common infrastructure for on-device InAppPayment testing
 */
class InAppPaymentsRule : ExternalResource() {
  override fun before() {
    initialiseConfigurationResponse()
    initialisePutSubscription()
    initialiseSetArchiveBackupId()
  }

  private fun initialiseConfigurationResponse() {
    val assets = InstrumentationRegistry.getInstrumentation().context.resources.assets
    val response = assets.open("inAppPaymentsTests/configuration.json").use { stream ->
      NetworkResult.Success(JsonUtils.fromJson(stream, SubscriptionsConfiguration::class.java))
    }

    AppDependencies.donationsApi.apply {
      every { getDonationsConfiguration(any()) } returns response
    }
  }

  private fun initialisePutSubscription() {
    AppDependencies.donationsApi.apply {
      every { putSubscription(any()) } returns NetworkResult.Success(Unit)
    }
  }

  private fun initialiseSetArchiveBackupId() {
    AppDependencies.archiveApi.apply {
      every { triggerBackupIdReservation(any(), any(), any()) } returns NetworkResult.Success(Unit)
    }
  }
}

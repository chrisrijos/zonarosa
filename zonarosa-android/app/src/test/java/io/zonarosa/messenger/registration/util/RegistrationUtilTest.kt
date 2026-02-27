/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.util

import android.app.Application
import assertk.assertThat
import assertk.assertions.each
import assertk.assertions.extracting
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.core.util.logging.Log.initialize
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.Skipped
import io.zonarosa.messenger.keyvalue.Start
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.testutil.LogRecorder
import io.zonarosa.messenger.testutil.MockAppDependenciesRule
import io.zonarosa.messenger.testutil.MockZonaRosaStoreRule
import io.zonarosa.messenger.util.RemoteConfig

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, manifest = Config.NONE)
class RegistrationUtilTest {
  @get:Rule
  val zonarosaStore = MockZonaRosaStoreRule(relaxed = setOf(PhoneNumberPrivacyValues::class))

  @get:Rule
  val appDependencies = MockAppDependenciesRule()

  private lateinit var logRecorder: LogRecorder

  @Before
  fun setup() {
    mockkObject(Recipient)
    mockkStatic(RemoteConfig::class)

    logRecorder = LogRecorder()
    initialize(logRecorder)

    every { ZonaRosaStore.backup.backupTier } returns null
    every { ZonaRosaStore.backup.backupsInitialized = any() } answers { }
    every { ZonaRosaStore.backup.cachedMediaCdnPath = any() } answers { }
    every { ZonaRosaStore.backup.mediaCredentials } returns mockk {
      every { clearAll() } answers {}
    }
    every { ZonaRosaStore.backup.messageCredentials } returns mockk {
      every { clearAll() } answers {}
    }
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun maybeMarkRegistrationComplete_allValidWithRestoreOption() {
    every { zonarosaStore.registration.isRegistrationComplete } returns false
    every { zonarosaStore.account.isRegistered } returns true
    every { Recipient.self() } returns Recipient(profileName = ProfileName.fromParts("Dark", "Helmet"))
    every { zonarosaStore.svr.hasPin() } returns true
    every { zonarosaStore.registration.restoreDecisionState } returns RestoreDecisionState.Skipped

    RegistrationUtil.maybeMarkRegistrationComplete()

    verify { zonarosaStore.registration.markRegistrationComplete() }
  }

  @Test
  fun maybeMarkRegistrationComplete_missingData() {
    every { zonarosaStore.registration.isRegistrationComplete } returns false
    every { zonarosaStore.account.isRegistered } returns false

    RegistrationUtil.maybeMarkRegistrationComplete()

    every { zonarosaStore.account.isRegistered } returns true
    every { Recipient.self() } returns Recipient(profileName = ProfileName.EMPTY)

    RegistrationUtil.maybeMarkRegistrationComplete()

    every { Recipient.self() } returns Recipient(profileName = ProfileName.fromParts("Dark", "Helmet"))
    every { zonarosaStore.svr.hasPin() } returns false
    every { zonarosaStore.svr.hasOptedOut() } returns false
    every { zonarosaStore.account.isLinkedDevice } returns false

    RegistrationUtil.maybeMarkRegistrationComplete()

    every { zonarosaStore.svr.hasPin() } returns true
    every { zonarosaStore.registration.restoreDecisionState } returns RestoreDecisionState.Start

    RegistrationUtil.maybeMarkRegistrationComplete()

    verify(exactly = 0) { zonarosaStore.registration.markRegistrationComplete() }

    val regUtilLogs = logRecorder.information.filter { it.tag == "RegistrationUtil" }
    assertThat(regUtilLogs).hasSize(4)
    assertThat(regUtilLogs)
      .extracting { it.message }
      .each { it.isEqualTo("Registration is not yet complete.") }
  }

  @Test
  fun maybeMarkRegistrationComplete_alreadyMarked() {
    every { zonarosaStore.registration.isRegistrationComplete } returns true

    RegistrationUtil.maybeMarkRegistrationComplete()

    verify(exactly = 0) { zonarosaStore.registration.markRegistrationComplete() }

    val regUtilLogs = logRecorder.information.filter { it.tag == "RegistrationUtil" }
    assertThat(regUtilLogs).isEmpty()
  }
}

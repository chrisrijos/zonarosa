/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.testutil

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.rules.ExternalResource
import io.zonarosa.messenger.keyvalue.AccountValues
import io.zonarosa.messenger.keyvalue.BackupValues
import io.zonarosa.messenger.keyvalue.EmojiValues
import io.zonarosa.messenger.keyvalue.InAppPaymentValues
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues
import io.zonarosa.messenger.keyvalue.RegistrationValues
import io.zonarosa.messenger.keyvalue.SettingsValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.SvrValues
import kotlin.reflect.KClass

/**
 * Mocks [ZonaRosaStore] to return mock versions of the various values. Mocks will default to not be relaxed (each
 * method call on them will need to be mocked) except for unit functions which will do nothing.
 *
 * Expand mocked values as necessary when needed.
 *
 * @param relaxed Set of value classes that should default to relaxed thus defaulting all methods. Useful
 * when value is not part of the input state under test but called within the under test code.
 */
@Suppress("MemberVisibilityCanBePrivate")
class MockZonaRosaStoreRule(private val relaxed: Set<KClass<*>> = emptySet()) : ExternalResource() {

  lateinit var account: AccountValues
    private set

  lateinit var phoneNumberPrivacy: PhoneNumberPrivacyValues
    private set

  lateinit var registration: RegistrationValues
    private set

  lateinit var svr: SvrValues
    private set

  lateinit var emoji: EmojiValues
    private set

  lateinit var inAppPayments: InAppPaymentValues
    private set

  lateinit var backup: BackupValues
    private set

  lateinit var settings: SettingsValues
    private set

  override fun before() {
    account = mockk(relaxed = relaxed.contains(AccountValues::class), relaxUnitFun = true)
    phoneNumberPrivacy = mockk(relaxed = relaxed.contains(PhoneNumberPrivacyValues::class), relaxUnitFun = true)
    registration = mockk(relaxed = relaxed.contains(RegistrationValues::class), relaxUnitFun = true)
    svr = mockk(relaxed = relaxed.contains(SvrValues::class), relaxUnitFun = true)
    emoji = mockk(relaxed = relaxed.contains(EmojiValues::class), relaxUnitFun = true)
    inAppPayments = mockk(relaxed = relaxed.contains(InAppPaymentValues::class), relaxUnitFun = true)
    backup = mockk(relaxed = relaxed.contains(BackupValues::class), relaxUnitFun = true)
    settings = mockk(relaxed = relaxed.contains(SettingsValues::class), relaxUnitFun = true)

    mockkObject(ZonaRosaStore)
    every { ZonaRosaStore.account } returns account
    every { ZonaRosaStore.phoneNumberPrivacy } returns phoneNumberPrivacy
    every { ZonaRosaStore.registration } returns registration
    every { ZonaRosaStore.svr } returns svr
    every { ZonaRosaStore.emoji } returns emoji
    every { ZonaRosaStore.inAppPayments } returns inAppPayments
    every { ZonaRosaStore.backup } returns backup
    every { ZonaRosaStore.settings } returns settings
  }

  override fun after() {
    unmockkObject(ZonaRosaStore)
  }
}

/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.util

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.mockk.every
import io.mockk.mockkObject
import org.junit.Before
import org.junit.Test
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

class ZonaRosaE164UtilTest {

  @Before
  fun setup() {
    mockkObject(ZonaRosaStore)
    every { ZonaRosaStore.account.e164 } returns "+11234567890"
  }

  @Test
  fun `isPotentialNonShortCodeE164 - valid`() {
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("+1234567890")).isTrue()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("1234567")).isTrue()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("1234568")).isTrue()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("12345679")).isTrue()
  }

  @Test
  fun `isPotentialNonShortCodeE164 - invalid, no leading characters`() {
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("1")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("12")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("123")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("12345")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("123456")).isFalse()
  }

  @Test
  fun `isPotentialNonShortCodeE164 - invalid, leading plus sign`() {
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("+123456")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("++123456")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("+++123456")).isFalse()
  }

  @Test
  fun `isPotentialNonShortCodeE164 - invalid, leading zeros`() {
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("0123456")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("00123456")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("000123456")).isFalse()
  }

  @Test
  fun `isPotentialNonShortCodeE164 - invalid, mix of leading characters`() {
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("+0123456")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("0+0123456")).isFalse()
    assertThat(ZonaRosaE164Util.isPotentialNonShortCodeE164("+0+123456")).isFalse()
  }
}

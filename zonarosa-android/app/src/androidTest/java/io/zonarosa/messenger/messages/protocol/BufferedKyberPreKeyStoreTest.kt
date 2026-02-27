/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.messages.protocol

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.protocol.ReusedBaseKeyException
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.testing.ZonaRosaDatabaseRule
import io.zonarosa.messenger.util.KyberPreKeysTestUtil

class BufferedKyberPreKeyStoreTest {

  @get:Rule
  val harness = ZonaRosaDatabaseRule()

  private lateinit var aci: ServiceId
  private lateinit var testSubject: BufferedKyberPreKeyStore
  private lateinit var dataStore: BufferedZonaRosaServiceAccountDataStore

  @Before
  fun setUp() {
    ZonaRosaStore.account.generateAciIdentityKeyIfNecessary()

    aci = harness.localAci
    testSubject = BufferedKyberPreKeyStore(aci)
    dataStore = BufferedZonaRosaServiceAccountDataStore(aci)
  }

  @Test
  fun givenALastResortKey_whenIMarkKyberPreKeyUsed_thenIExpectNoIssues() {
    KyberPreKeysTestUtil.insertTestRecord(aci, 1, lastResort = true)
    val publicKey = KyberPreKeysTestUtil.generateECPublicKey()

    testSubject.markKyberPreKeyUsed(
      kyberPreKeyId = 1,
      signedPreKeyId = 2,
      publicKey = publicKey
    )
  }

  @Test(expected = ReusedBaseKeyException::class)
  fun givenALastResortKey_whenIMarkKyberPreKeyUsedTwice_thenIExpectException() {
    KyberPreKeysTestUtil.insertTestRecord(aci, 1, lastResort = true)
    val publicKey = KyberPreKeysTestUtil.generateECPublicKey()

    testSubject.markKyberPreKeyUsed(
      kyberPreKeyId = 1,
      signedPreKeyId = 2,
      publicKey = publicKey
    )

    testSubject.markKyberPreKeyUsed(
      kyberPreKeyId = 1,
      signedPreKeyId = 2,
      publicKey = publicKey
    )
  }

  @Test
  fun givenAMarkedLastResortKey_whenIFlushTwice_thenIExpectNoIssues() {
    KyberPreKeysTestUtil.insertTestRecord(aci, 1, lastResort = true)
    val publicKey = KyberPreKeysTestUtil.generateECPublicKey()

    testSubject.markKyberPreKeyUsed(
      kyberPreKeyId = 1,
      signedPreKeyId = 2,
      publicKey = publicKey
    )

    testSubject.flushToDisk(dataStore)
    testSubject.flushToDisk(dataStore)
  }
}

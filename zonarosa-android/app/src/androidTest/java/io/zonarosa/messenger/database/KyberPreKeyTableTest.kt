/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.libzonarosa.protocol.ReusedBaseKeyException
import io.zonarosa.messenger.util.KyberPreKeysTestUtil.generateECPublicKey
import io.zonarosa.messenger.util.KyberPreKeysTestUtil.getStaleTime
import io.zonarosa.messenger.util.KyberPreKeysTestUtil.insertTestRecord
import java.util.UUID

class KyberPreKeyTableTest {

  private val aci: ACI = ACI.from(UUID.randomUUID())
  private val pni: PNI = PNI.from(UUID.randomUUID())

  @Test
  fun markAllStaleIfNecessary_onlyUpdatesMatchingAccountAndZeroValues() {
    insertTestRecord(aci, id = 1)
    insertTestRecord(aci, id = 2)
    insertTestRecord(aci, id = 3, staleTime = 42)
    insertTestRecord(pni, id = 4)

    val now = System.currentTimeMillis()
    ZonaRosaDatabase.kyberPreKeys.markAllStaleIfNecessary(aci, now)

    assertEquals(now, getStaleTime(aci, 1))
    assertEquals(now, getStaleTime(aci, 2))
    assertEquals(42L, getStaleTime(aci, 3))
    assertEquals(0L, getStaleTime(pni, 4))
  }

  @Test
  fun deleteAllStaleBefore_deleteOldBeforeThreshold() {
    insertTestRecord(aci, id = 1, staleTime = 10)
    insertTestRecord(aci, id = 2, staleTime = 10)
    insertTestRecord(aci, id = 3, staleTime = 10)
    insertTestRecord(aci, id = 4, staleTime = 15)
    insertTestRecord(aci, id = 5, staleTime = 0)

    ZonaRosaDatabase.kyberPreKeys.deleteAllStaleBefore(aci, threshold = 11, minCount = 0)

    assertNull(getStaleTime(aci, 1))
    assertNull(getStaleTime(aci, 2))
    assertNull(getStaleTime(aci, 3))
    assertNotNull(getStaleTime(aci, 4))
    assertNotNull(getStaleTime(aci, 5))
  }

  @Test
  fun deleteAllStaleBefore_neverDeleteStaleOfZero() {
    insertTestRecord(aci, id = 1, staleTime = 0)
    insertTestRecord(aci, id = 2, staleTime = 0)
    insertTestRecord(aci, id = 3, staleTime = 0)
    insertTestRecord(aci, id = 4, staleTime = 0)
    insertTestRecord(aci, id = 5, staleTime = 0)

    ZonaRosaDatabase.kyberPreKeys.deleteAllStaleBefore(aci, threshold = 10, minCount = 1)

    assertNotNull(getStaleTime(aci, 1))
    assertNotNull(getStaleTime(aci, 2))
    assertNotNull(getStaleTime(aci, 3))
    assertNotNull(getStaleTime(aci, 4))
    assertNotNull(getStaleTime(aci, 5))
  }

  @Test
  fun deleteAllStaleBefore_respectMinCount() {
    insertTestRecord(aci, id = 1, staleTime = 10)
    insertTestRecord(aci, id = 2, staleTime = 10)
    insertTestRecord(aci, id = 3, staleTime = 10)
    insertTestRecord(aci, id = 4, staleTime = 10)
    insertTestRecord(aci, id = 5, staleTime = 10)

    ZonaRosaDatabase.kyberPreKeys.deleteAllStaleBefore(aci, threshold = 11, minCount = 3)

    assertNull(getStaleTime(aci, 1))
    assertNull(getStaleTime(aci, 2))
    assertNotNull(getStaleTime(aci, 3))
    assertNotNull(getStaleTime(aci, 4))
    assertNotNull(getStaleTime(aci, 5))
  }

  @Test
  fun deleteAllStaleBefore_respectAccount() {
    insertTestRecord(aci, id = 1, staleTime = 10)
    insertTestRecord(aci, id = 2, staleTime = 10)
    insertTestRecord(aci, id = 3, staleTime = 10)

    insertTestRecord(pni, id = 4, staleTime = 10)
    insertTestRecord(pni, id = 5, staleTime = 10)

    ZonaRosaDatabase.kyberPreKeys.deleteAllStaleBefore(aci, threshold = 11, minCount = 2)

    assertNull(getStaleTime(aci, 1))
    assertNotNull(getStaleTime(aci, 2))
    assertNotNull(getStaleTime(aci, 3))
    assertNotNull(getStaleTime(pni, 4))
    assertNotNull(getStaleTime(pni, 5))
  }

  @Test
  fun deleteAllStaleBefore_ignoreLastResortForMinCount() {
    insertTestRecord(aci, id = 1, staleTime = 10)
    insertTestRecord(aci, id = 2, staleTime = 10)
    insertTestRecord(aci, id = 3, staleTime = 10)
    insertTestRecord(aci, id = 4, staleTime = 10)
    insertTestRecord(aci, id = 5, staleTime = 10, lastResort = true)

    ZonaRosaDatabase.kyberPreKeys.deleteAllStaleBefore(aci, threshold = 11, minCount = 3)

    assertNull(getStaleTime(aci, 1))
    assertNotNull(getStaleTime(aci, 2))
    assertNotNull(getStaleTime(aci, 3))
    assertNotNull(getStaleTime(aci, 4))
    assertNotNull(getStaleTime(aci, 5))
  }

  @Test
  fun deleteAllStaleBefore_neverDeleteLastResort() {
    insertTestRecord(aci, id = 1, staleTime = 10, lastResort = true)
    insertTestRecord(aci, id = 2, staleTime = 10, lastResort = true)
    insertTestRecord(aci, id = 3, staleTime = 10, lastResort = true)

    ZonaRosaDatabase.oneTimePreKeys.deleteAllStaleBefore(aci, threshold = 11, minCount = 0)

    assertNotNull(getStaleTime(aci, 1))
    assertNotNull(getStaleTime(aci, 2))
    assertNotNull(getStaleTime(aci, 3))
  }

  @Test(expected = ReusedBaseKeyException::class)
  fun handleMarkKyberPreKeyUsed_doesNotAllowDuplicateLastResortKeyEntries() {
    insertTestRecord(aci, id = 1, staleTime = 10, lastResort = true)
    val publicKey = generateECPublicKey()

    ZonaRosaDatabase.kyberPreKeys.handleMarkKyberPreKeyUsed(
      serviceId = aci,
      kyberPreKeyId = 1,
      signedPreKeyId = 1,
      baseKey = publicKey
    )

    ZonaRosaDatabase.kyberPreKeys.handleMarkKyberPreKeyUsed(
      serviceId = aci,
      kyberPreKeyId = 1,
      signedPreKeyId = 1,
      baseKey = publicKey
    )
  }

  @Test
  fun handleMarkKyberPreKeyUsed_allowDuplicateNonLastResortKeyEntries() {
    insertTestRecord(aci, id = 1, staleTime = 10, lastResort = false)
    val publicKey = generateECPublicKey()

    ZonaRosaDatabase.kyberPreKeys.handleMarkKyberPreKeyUsed(
      serviceId = aci,
      kyberPreKeyId = 1,
      signedPreKeyId = 1,
      baseKey = publicKey
    )

    ZonaRosaDatabase.kyberPreKeys.handleMarkKyberPreKeyUsed(
      serviceId = aci,
      kyberPreKeyId = 1,
      signedPreKeyId = 1,
      baseKey = publicKey
    )
  }
}

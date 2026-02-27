package io.zonarosa.messenger.storage

import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import okio.ByteString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.ACI.Companion.parseOrThrow
import io.zonarosa.messenger.storage.StorageSyncHelper.findIdDifference
import io.zonarosa.messenger.storage.StorageSyncHelper.profileKeyChanged
import io.zonarosa.messenger.testutil.TestHelpers
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.storage.ZonaRosaContactRecord
import io.zonarosa.service.api.storage.ZonaRosaRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.internal.storage.protos.ContactRecord
import kotlin.time.Duration.Companion.days

class StorageSyncHelperTest {
  @Before
  fun setup() {
    mockkObject(RemoteConfig)
  }

  @After
  fun tearDown() {
    unmockkObject(RemoteConfig)
  }

  @Test
  fun findIdDifference_allOverlap() {
    every { RemoteConfig.messageQueueTime } returns 45.days.inWholeMilliseconds

    val result = findIdDifference(keyListOf(1, 2, 3), keyListOf(1, 2, 3))
    assertTrue(result.localOnlyIds.isEmpty())
    assertTrue(result.remoteOnlyIds.isEmpty())
    assertFalse(result.hasTypeMismatches)
  }

  @Test
  fun findIdDifference_noOverlap() {
    every { RemoteConfig.messageQueueTime } returns 45.days.inWholeMilliseconds

    val result = findIdDifference(keyListOf(1, 2, 3), keyListOf(4, 5, 6))
    TestHelpers.assertContentsEqual(keyListOf(1, 2, 3), result.remoteOnlyIds)
    TestHelpers.assertContentsEqual(keyListOf(4, 5, 6), result.localOnlyIds)
    assertFalse(result.hasTypeMismatches)
  }

  @Test
  fun findIdDifference_someOverlap() {
    every { RemoteConfig.messageQueueTime } returns 45.days.inWholeMilliseconds

    val result = findIdDifference(keyListOf(1, 2, 3), keyListOf(2, 3, 4))
    TestHelpers.assertContentsEqual(keyListOf(1), result.remoteOnlyIds)
    TestHelpers.assertContentsEqual(keyListOf(4), result.localOnlyIds)
    assertFalse(result.hasTypeMismatches)
  }

  @Test
  fun findIdDifference_typeMismatch_allOverlap() {
    every { RemoteConfig.messageQueueTime } returns 45.days.inWholeMilliseconds

    val result = findIdDifference(
      keyListOf(
        mapOf(
          100 to 1,
          200 to 2
        )
      ),
      keyListOf(
        mapOf(
          100 to 1,
          200 to 1
        )
      )
    )

    assertTrue(result.localOnlyIds.isEmpty())
    assertTrue(result.remoteOnlyIds.isEmpty())
    assertTrue(result.hasTypeMismatches)
  }

  @Test
  fun findIdDifference_typeMismatch_someOverlap() {
    every { RemoteConfig.messageQueueTime } returns 45.days.inWholeMilliseconds

    val result = findIdDifference(
      keyListOf(
        mapOf(
          100 to 1,
          200 to 2,
          300 to 1
        )
      ),
      keyListOf(
        mapOf(
          100 to 1,
          200 to 1,
          400 to 1
        )
      )
    )

    TestHelpers.assertContentsEqual(listOf(StorageId.forType(TestHelpers.byteArray(300), 1)), result.remoteOnlyIds)
    TestHelpers.assertContentsEqual(listOf(StorageId.forType(TestHelpers.byteArray(400), 1)), result.localOnlyIds)
    assertTrue(result.hasTypeMismatches)
  }

  @Test
  fun test_ContactUpdate_equals_sameProfileKeys() {
    every { RemoteConfig.messageQueueTime } returns 45.days.inWholeMilliseconds

    val profileKey = ByteArray(32)
    val profileKeyCopy = profileKey.clone()

    val contactA = contactBuilder(ACI_A, E164_A, "a").profileKey(ByteString.of(*profileKey)).build()
    val contactB = contactBuilder(ACI_A, E164_A, "a").profileKey(ByteString.of(*profileKeyCopy)).build()

    val zonarosaContactA = ZonaRosaContactRecord(StorageId.forContact(TestHelpers.byteArray(1)), contactA)
    val zonarosaContactB = ZonaRosaContactRecord(StorageId.forContact(TestHelpers.byteArray(1)), contactB)

    assertEquals(zonarosaContactA, zonarosaContactB)
    assertEquals(zonarosaContactA.hashCode(), zonarosaContactB.hashCode())

    assertFalse(profileKeyChanged(update(zonarosaContactA, zonarosaContactB)))
  }

  @Test
  fun test_ContactUpdate_equals_differentProfileKeys() {
    every { RemoteConfig.messageQueueTime } returns 45.days.inWholeMilliseconds

    val profileKey = ByteArray(32)
    val profileKeyCopy = profileKey.clone()
    profileKeyCopy[0] = 1

    val contactA = contactBuilder(ACI_A, E164_A, "a").profileKey(ByteString.of(*profileKey)).build()
    val contactB = contactBuilder(ACI_A, E164_A, "a").profileKey(ByteString.of(*profileKeyCopy)).build()

    val zonarosaContactA = ZonaRosaContactRecord(StorageId.forContact(TestHelpers.byteArray(1)), contactA)
    val zonarosaContactB = ZonaRosaContactRecord(StorageId.forContact(TestHelpers.byteArray(1)), contactB)

    assertNotEquals(zonarosaContactA, zonarosaContactB)
    assertNotEquals(zonarosaContactA.hashCode(), zonarosaContactB.hashCode())

    assertTrue(profileKeyChanged(update(zonarosaContactA, zonarosaContactB)))
  }

  companion object {
    private val ACI_A = parseOrThrow("ebef429e-695e-4f51-bcc4-526a60ac68c7")

    private const val E164_A = "+16108675309"

    @Suppress("SameParameterValue")
    private fun contactBuilder(aci: ACI, e164: String, profileName: String): ContactRecord.Builder {
      return ContactRecord.Builder()
        .aci(aci.toString())
        .aciBinary(aci.toByteString())
        .e164(e164)
        .givenName(profileName)
    }

    private fun <E : ZonaRosaRecord<*>> update(oldRecord: E, newRecord: E): StorageRecordUpdate<E> {
      return StorageRecordUpdate(oldRecord, newRecord)
    }

    private fun keyListOf(vararg vals: Int): List<StorageId> {
      return TestHelpers.byteListOf(*vals).map { StorageId.forType(it, 1) }.toList()
    }

    private fun keyListOf(vals: Map<Int, Int>): List<StorageId> {
      return vals.map { StorageId.forType(TestHelpers.byteArray(it.key), it.value) }.toList()
    }
  }
}

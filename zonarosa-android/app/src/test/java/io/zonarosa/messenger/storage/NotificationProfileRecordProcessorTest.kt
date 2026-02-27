package io.zonarosa.messenger.storage

import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.testutil.EmptyLogger
import io.zonarosa.service.api.storage.ZonaRosaNotificationProfileRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.internal.storage.protos.NotificationProfile
import io.zonarosa.service.internal.storage.protos.Recipient
import java.util.UUID

/**
 * Tests for [NotificationProfileRecordProcessor]
 */
class NotificationProfileRecordProcessorTest {
  companion object {
    val STORAGE_ID: StorageId = StorageId.forNotificationProfile(byteArrayOf(1, 2, 3, 4))

    @JvmStatic
    @BeforeClass
    fun setUpClass() {
      Log.initialize(EmptyLogger())
    }
  }

  private val testSubject = NotificationProfileRecordProcessor()

  @Test
  fun `Given a valid proto with a known name and id, assert valid`() {
    // GIVEN
    val proto = NotificationProfile.Builder().apply {
      id = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "name"
    }.build()
    val record = ZonaRosaNotificationProfileRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertFalse(result)
  }

  @Test
  fun `Given a valid proto with a deleted timestamp, known name and id, assert valid`() {
    // GIVEN
    val proto = NotificationProfile.Builder().apply {
      id = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "name"
      deletedAtTimestampMs = 1000L
    }.build()
    val record = ZonaRosaNotificationProfileRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertFalse(result)
  }

  @Test
  fun `Given an invalid proto with no id, assert invalid`() {
    // GIVEN
    val proto = NotificationProfile.Builder().apply {
      id = "Bad".toByteArray().toByteString()
      name = "Profile"
      deletedAtTimestampMs = 0L
    }.build()
    val record = ZonaRosaNotificationProfileRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }

  @Test
  fun `Given an invalid proto with no name, assert invalid`() {
    // GIVEN
    val proto = NotificationProfile.Builder().apply {
      id = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = ""
      deletedAtTimestampMs = 0L
    }.build()
    val record = ZonaRosaNotificationProfileRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }

  @Test
  fun `Given an invalid proto with a member that does not have a service id, assert invalid`() {
    // GIVEN
    val proto = NotificationProfile.Builder().apply {
      id = UuidUtil.toByteArray(UUID.randomUUID()).toByteString()
      name = "Profile"
      allowedMembers = listOf(Recipient(contact = Recipient.Contact(serviceIdBinary = "bad".toByteArray().toByteString())))
    }.build()
    val record = ZonaRosaNotificationProfileRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertTrue(result)
  }
}

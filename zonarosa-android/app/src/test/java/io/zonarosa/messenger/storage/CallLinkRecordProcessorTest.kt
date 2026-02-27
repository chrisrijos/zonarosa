/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.storage

import okio.ByteString.Companion.EMPTY
import okio.ByteString.Companion.toByteString
import org.junit.Assert.assertFalse
import org.junit.BeforeClass
import org.junit.Test
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.testutil.EmptyLogger
import io.zonarosa.service.api.storage.ZonaRosaCallLinkRecord
import io.zonarosa.service.api.storage.StorageId
import io.zonarosa.service.internal.storage.protos.CallLinkRecord

/**
 * See [CallLinkRecordProcessor]
 */
class CallLinkRecordProcessorTest {
  companion object {
    val STORAGE_ID: StorageId = StorageId.forCallLink(byteArrayOf(1, 2, 3, 4))

    @JvmStatic
    @BeforeClass
    fun setUpClass() {
      Log.initialize(EmptyLogger())
    }
  }

  private val testSubject = CallLinkRecordProcessor()
  private val mockCredentials = CallLinkCredentials(
    "root key".toByteArray(),
    "admin pass".toByteArray()
  )

  @Test
  fun `Given a valid proto with only an admin pass key and not a deletion timestamp, assert valid`() {
    // GIVEN
    val proto = CallLinkRecord.Builder().apply {
      rootKey = mockCredentials.linkKeyBytes.toByteString()
      adminPasskey = mockCredentials.adminPassBytes!!.toByteString()
      deletedAtTimestampMs = 0L
    }.build()

    val record = ZonaRosaCallLinkRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertFalse(result)
  }

  @Test
  fun `Given a valid proto with only a deletion timestamp and not an admin pass key, assert valid`() {
    // GIVEN
    val proto = CallLinkRecord.Builder().apply {
      rootKey = mockCredentials.linkKeyBytes.toByteString()
      adminPasskey = EMPTY
      deletedAtTimestampMs = System.currentTimeMillis()
    }.build()

    val record = ZonaRosaCallLinkRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertFalse(result)
  }

  @Test
  fun `Given a proto with neither an admin pass key nor a deletion timestamp, assert valid`() {
    // GIVEN
    val proto = CallLinkRecord.Builder().apply {
      rootKey = mockCredentials.linkKeyBytes.toByteString()
      adminPasskey = EMPTY
      deletedAtTimestampMs = 0L
    }.build()

    val record = ZonaRosaCallLinkRecord(STORAGE_ID, proto)

    // WHEN
    val result = testSubject.isInvalid(record)

    // THEN
    assertFalse(result)
  }
}

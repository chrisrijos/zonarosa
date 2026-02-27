/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkState
import io.zonarosa.messenger.testing.ZonaRosaActivityRule

@RunWith(AndroidJUnit4::class)
class CallLinkTableTest {

  companion object {
    private val ROOM_ID_A = byteArrayOf(1, 2, 3, 4)
    private val ROOM_ID_B = byteArrayOf(2, 2, 3, 4)
    private const val TIMESTAMP_A = 1000L
    private const val TIMESTAMP_B = 2000L
  }

  @get:Rule
  val harness = ZonaRosaActivityRule(createGroup = true)

//  @Test
  fun givenTwoNonAdminCallLinks_whenIDeleteBeforeFirst_thenIExpectNeitherDeleted() {
    insertTwoNonAdminCallLinksWithEvents()
//    ZonaRosaDatabase.callLinks.deleteNonAdminCallLinksOnOrBefore(TIMESTAMP_A - 500)
//    val callEvents = ZonaRosaDatabase.calls.getCalls(0, 2, "", CallLogFilter.ALL)
//    assertEquals(2, callEvents.size)
  }

//  @Test
  fun givenTwoNonAdminCallLinks_whenIDeleteOnFirst_thenIExpectFirstDeleted() {
    insertTwoNonAdminCallLinksWithEvents()
    ZonaRosaDatabase.callLinks.deleteNonAdminCallLinksOnOrBefore(TIMESTAMP_A)
//    val callEvents = ZonaRosaDatabase.calls.getCalls(0, 2, "", CallLogFilter.ALL)
//    assertEquals(1, callEvents.size)
//    assertEquals(TIMESTAMP_B, callEvents.first().record.timestamp)
  }

//  @Test
  fun givenTwoNonAdminCallLinks_whenIDeleteAfterFirstAndBeforeSecond_thenIExpectFirstDeleted() {
    insertTwoNonAdminCallLinksWithEvents()
    ZonaRosaDatabase.callLinks.deleteNonAdminCallLinksOnOrBefore(TIMESTAMP_B - 500)
//    val callEvents = ZonaRosaDatabase.calls.getCalls(0, 2, "", CallLogFilter.ALL)
//    assertEquals(1, callEvents.size)
//    assertEquals(TIMESTAMP_B, callEvents.first().record.timestamp)
  }

//  @Test
  fun givenTwoNonAdminCallLinks_whenIDeleteOnSecond_thenIExpectBothDeleted() {
    insertTwoNonAdminCallLinksWithEvents()
    ZonaRosaDatabase.callLinks.deleteNonAdminCallLinksOnOrBefore(TIMESTAMP_B)
//    val callEvents = ZonaRosaDatabase.calls.getCalls(0, 2, "", CallLogFilter.ALL)
//    assertEquals(0, callEvents.size)
  }

//  @Test
  fun givenTwoNonAdminCallLinks_whenIDeleteAfterSecond_thenIExpectBothDeleted() {
    insertTwoNonAdminCallLinksWithEvents()
    ZonaRosaDatabase.callLinks.deleteNonAdminCallLinksOnOrBefore(TIMESTAMP_B + 500)
//    val callEvents = ZonaRosaDatabase.calls.getCalls(0, 2, "", CallLogFilter.ALL)
//    assertEquals(0, callEvents.size)
  }

  private fun insertTwoNonAdminCallLinksWithEvents() {
    insertCallLinkWithEvent(ROOM_ID_A, 1000)
    insertCallLinkWithEvent(ROOM_ID_B, 2000)
  }

  private fun insertCallLinkWithEvent(roomId: ByteArray, timestamp: Long) {
    ZonaRosaDatabase.callLinks.insertCallLink(
      CallLinkTable.CallLink(
        recipientId = RecipientId.UNKNOWN,
        roomId = CallLinkRoomId.fromBytes(roomId),
        credentials = CallLinkCredentials(
          linkKeyBytes = roomId,
          adminPassBytes = null
        ),
        state = ZonaRosaCallLinkState(),
        deletionTimestamp = 0L
      )
    )

    val callLinkRecipient = ZonaRosaDatabase.recipients.getByCallLinkRoomId(CallLinkRoomId.fromBytes(roomId)).get()

    ZonaRosaDatabase.calls.insertAcceptedGroupCall(
      1,
      callLinkRecipient,
      CallTable.Direction.INCOMING,
      timestamp
    )
  }
}

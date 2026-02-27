/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.v2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.withContext
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.GroupRecord
import io.zonarosa.messenger.groups.ui.GroupMemberEntry
import io.zonarosa.messenger.recipients.LiveRecipient
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

/**
 * Repository providing different fields about the call peer.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CallPeerRepository(
  externalScope: CoroutineScope
) {
  val recipientId = MutableStateFlow(RecipientId.UNKNOWN)
  val liveRecipient: StateFlow<LiveRecipient> = recipientId.mapLatest { Recipient.live(it) }
    .stateIn(externalScope, SharingStarted.Eagerly, Recipient.live(RecipientId.UNKNOWN))

  private val recipient: Flow<Recipient> = liveRecipient.flatMapLatest { it.observable().asFlow() }
  private val groupRecipient = recipient.filter { it.isActiveGroup }
  private val groupRecord: Flow<GroupRecord> = groupRecipient.mapLatest {
    withContext(Dispatchers.IO) {
      ZonaRosaDatabase.groups.getGroup(it.requireGroupId()).get()
    }
  }

  val groupMembers: Flow<List<GroupMemberEntry.FullMember>> = groupRecord.mapLatest { record ->
    withContext(Dispatchers.IO) {
      record.members.map { Recipient.resolved(it) }.map { GroupMemberEntry.FullMember(it, record.isAdmin(it)) }
    }
  }

  val groupMembersChanged: Flow<List<GroupMemberEntry.FullMember>> = groupMembers.drop(1)
  val groupMembersCount: Flow<Int> = groupMembers.map { it.size }
}

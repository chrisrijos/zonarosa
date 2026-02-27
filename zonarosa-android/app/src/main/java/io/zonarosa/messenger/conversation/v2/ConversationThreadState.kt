/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2

import io.zonarosa.paging.ObservablePagedData
import io.zonarosa.messenger.conversation.ConversationData
import io.zonarosa.messenger.conversation.v2.data.ConversationElementKey
import io.zonarosa.messenger.util.adapter.mapping.MappingModel

/**
 * Represents the content that will be displayed in the conversation
 * thread (recycler).
 */
class ConversationThreadState(
  val items: ObservablePagedData<ConversationElementKey, MappingModel<*>>,
  val meta: ConversationData
)

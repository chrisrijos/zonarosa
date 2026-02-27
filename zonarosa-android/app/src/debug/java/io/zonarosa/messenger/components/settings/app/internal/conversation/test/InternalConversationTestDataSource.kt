/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.internal.conversation.test

import io.zonarosa.paging.PagedDataSource
import io.zonarosa.messenger.conversation.v2.data.ConversationElementKey
import io.zonarosa.messenger.conversation.v2.data.ConversationMessageElement
import io.zonarosa.messenger.util.adapter.mapping.MappingModel
import kotlin.math.min

class InternalConversationTestDataSource(
  private val size: Int,
  private val generator: ConversationElementGenerator
) : PagedDataSource<ConversationElementKey, MappingModel<*>> {
  override fun size(): Int = size

  override fun load(start: Int, length: Int, totalSize: Int, cancellationZonaRosa: PagedDataSource.CancellationZonaRosa): MutableList<MappingModel<*>> {
    val end = min(start + length, totalSize)
    return (start until end).map {
      load(ConversationElementKey.forMessage(it.toLong()))!!
    }.toMutableList()
  }

  override fun getKey(data: MappingModel<*>): ConversationElementKey {
    check(data is ConversationMessageElement)

    return ConversationElementKey.forMessage(data.conversationMessage.messageRecord.id)
  }

  override fun load(key: ConversationElementKey?): MappingModel<*>? {
    return key?.let { generator.getMappingModel(it) }
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.internal.conversation.test

import androidx.lifecycle.ViewModel
import io.zonarosa.paging.PagedData
import io.zonarosa.paging.PagingConfig

class InternalConversationTestViewModel : ViewModel() {
  private val generator = ConversationElementGenerator()
  private val dataSource = InternalConversationTestDataSource(
    500,
    generator
  )

  private val config = PagingConfig.Builder().setPageSize(25)
    .setBufferPages(2)
    .build()

  private val pagedData = PagedData.createForObservable(dataSource, config)

  val controller = pagedData.controller
  val data = pagedData.data
}

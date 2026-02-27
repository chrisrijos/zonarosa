/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import android.database.Cursor
import io.zonarosa.spinner.ColumnTransformer
import io.zonarosa.spinner.DefaultColumnTransformer

object IdPopupTransformer : ColumnTransformer {

  private val recipientColumns = setOf(
    MessageTable.FROM_RECIPIENT_ID,
    MessageTable.TO_RECIPIENT_ID,
    ThreadTable.RECIPIENT_ID
  )

  private val threadIdColumns = setOf(
    MessageTable.THREAD_ID
  )

  private val messageIdColumns = setOf(
    AttachmentTable.MESSAGE_ID,
    ThreadTable.SNIPPET_MESSAGE_ID
  )

  override fun matches(tableName: String?, columnName: String): Boolean {
    return recipientColumns.contains(columnName) || threadIdColumns.contains(columnName) || messageIdColumns.contains(columnName)
  }

  override fun transform(tableName: String?, columnName: String, cursor: Cursor): String? {
    val default = DefaultColumnTransformer.transform(tableName, columnName, cursor)

    return if (recipientColumns.contains(columnName)) {
      default?.let {
        """<div class="popup" data-recipient-id="$it">$it<span class="tooltip">Loading...</span></div>"""
      }
    } else if (threadIdColumns.contains(columnName)) {
      default?.let {
        """<div class="popup" data-thread-id="$it">$it<span class="tooltip">Loading...</span></div>"""
      }
    } else if (messageIdColumns.contains(columnName)) {
      default?.let {
        """<div class="popup" data-message-id="$it">$it<span class="tooltip">Loading...</span></div>"""
      }
    } else {
      default
    }
  }
}

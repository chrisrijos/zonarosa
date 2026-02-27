/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.main

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.notifications.MarkReadReceiver

object MainToolbarRepository {
  /**
   * Mark all unread messages in the local database as read.
   */
  fun markAllMessagesRead() {
    ZonaRosaExecutors.BOUNDED.execute {
      val messageIds = ZonaRosaDatabase.threads.setAllThreadsRead()
      AppDependencies.messageNotifier.updateNotification(AppDependencies.application)
      MarkReadReceiver.process(messageIds)
    }
  }
}

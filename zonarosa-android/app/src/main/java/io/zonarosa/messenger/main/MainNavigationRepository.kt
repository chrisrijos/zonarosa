package io.zonarosa.messenger.main

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import io.zonarosa.messenger.database.RxDatabaseObserver
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.recipients.Recipient

object MainNavigationRepository {

  fun getNumberOfUnreadMessages(): Flow<Long> {
    return RxDatabaseObserver.conversationList.map { ZonaRosaDatabase.threads.getUnreadMessageCount() }.asFlow()
  }

  fun getNumberOfUnseenStories(): Flow<Long> {
    return RxDatabaseObserver.conversationList.map {
      ZonaRosaDatabase
        .messages
        .getUnreadStoryThreadRecipientIds()
        .map { Recipient.resolved(it) }
        .filterNot { it.shouldHideStory }
        .size
        .toLong()
    }.asFlow()
  }

  fun getHasFailedOutgoingStories(): Flow<Boolean> {
    return RxDatabaseObserver.conversationList.map { ZonaRosaDatabase.messages.hasFailedOutgoingStory() }.asFlow()
  }

  fun getNumberOfUnseenCalls(): Flow<Long> {
    return RxDatabaseObserver.conversationList.map { ZonaRosaDatabase.calls.getUnreadMissedCallCount() }.asFlow()
  }
}

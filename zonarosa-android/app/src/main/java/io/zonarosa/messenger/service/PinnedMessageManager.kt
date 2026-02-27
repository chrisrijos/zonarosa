package io.zonarosa.messenger.service

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.WorkerThread
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.util.GroupUtil
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage

/**
 * Manages waking up and unpinning pinned messages at the correct time
 */
class PinnedMessageManager(
  val application: Application
) : TimedEventManager<PinnedMessageManager.Event>(application, "PinnedMessagesManager") {

  companion object {
    private val TAG = Log.tag(PinnedMessageManager::class.java)
  }

  private val messagesTable = ZonaRosaDatabase.messages

  init {
    scheduleIfNecessary()
  }

  @WorkerThread
  override fun getNextClosestEvent(): Event? {
    val oldestMessage: MmsMessageRecord? = messagesTable.getOldestExpiringPinnedMessageTimestamp() as? MmsMessageRecord

    if (oldestMessage == null) {
      cancelAlarm(application, PinnedMessagesAlarm::class.java)
      return null
    }

    val delay = (oldestMessage.pinnedUntil - System.currentTimeMillis()).coerceAtLeast(0)
    Log.i(TAG, "The next pinned message needs to be unpinned in $delay ms.")

    return Event(delay, oldestMessage.toRecipient.id, oldestMessage.threadId)
  }

  @WorkerThread
  override fun executeEvent(event: Event) {
    val pinnedMessagesToUnpin = messagesTable.getPinnedMessagesBefore(System.currentTimeMillis())
    for (record in pinnedMessagesToUnpin) {
      messagesTable.unpinMessage(messageId = record.id, threadId = record.threadId)
      val dataMessageBuilder = ZonaRosaServiceDataMessage.newBuilder()
        .withTimestamp(System.currentTimeMillis())
        .withUnpinnedMessage(
          ZonaRosaServiceDataMessage.UnpinnedMessage(
            targetAuthor = record.fromRecipient.requireServiceId(),
            targetSentTimestamp = record.dateSent
          )
        )

      val conversationRecipient = ZonaRosaDatabase.threads.getRecipientForThreadId(record.threadId) ?: continue
      if (conversationRecipient.isGroup) {
        GroupUtil.setDataMessageGroupContext(application, dataMessageBuilder, conversationRecipient.requireGroupId().requirePush())
      }
      AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(dataMessageBuilder.build())
    }
  }

  @WorkerThread
  override fun getDelayForEvent(event: Event): Long = event.delay

  @WorkerThread
  override fun scheduleAlarm(application: Application, event: Event, delay: Long) {
    setAlarm(
      application,
      System.currentTimeMillis() + delay,
      PinnedMessagesAlarm::class.java
    )
  }

  data class Event(val delay: Long, val recipientId: RecipientId, val threadId: Long)

  class PinnedMessagesAlarm : BroadcastReceiver() {

    companion object {
      private val TAG = Log.tag(PinnedMessagesAlarm::class.java)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
      Log.d(TAG, "onReceive()")
      AppDependencies.pinnedMessageManager.scheduleIfNecessary()
    }
  }
}

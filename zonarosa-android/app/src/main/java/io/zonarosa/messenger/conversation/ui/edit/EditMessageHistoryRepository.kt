package io.zonarosa.messenger.conversation.ui.edit

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.conversation.ConversationMessage
import io.zonarosa.messenger.conversation.v2.data.AttachmentHelper
import io.zonarosa.messenger.database.DatabaseObserver
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.notifications.MarkReadReceiver
import io.zonarosa.messenger.recipients.Recipient

object EditMessageHistoryRepository {

  fun getEditHistory(messageId: Long): Observable<List<ConversationMessage>> {
    return Observable.create { emitter ->
      val threadId: Long = ZonaRosaDatabase.messages.getThreadIdForMessage(messageId)
      if (threadId < 0) {
        emitter.onNext(emptyList())
        return@create
      }

      val databaseObserver: DatabaseObserver = AppDependencies.databaseObserver
      val observer = DatabaseObserver.Observer { emitter.onNext(getEditHistorySync(messageId)) }

      databaseObserver.registerConversationObserver(threadId, observer)

      emitter.setCancellable { databaseObserver.unregisterObserver(observer) }
      emitter.onNext(getEditHistorySync(messageId))
    }.subscribeOn(Schedulers.io())
  }

  fun markRevisionsRead(messageId: Long) {
    ZonaRosaExecutors.BOUNDED.execute {
      MarkReadReceiver.process(ZonaRosaDatabase.messages.setAllEditMessageRevisionsRead(messageId))
    }
  }

  private fun getEditHistorySync(messageId: Long): List<ConversationMessage> {
    val context = AppDependencies.application
    val records = ZonaRosaDatabase
      .messages
      .getMessageEditHistory(messageId)
      .toList()
      .reversed()

    if (records.isEmpty()) {
      return emptyList()
    }

    val attachmentHelper = AttachmentHelper()
      .apply {
        addAll(records)
        fetchAttachments()
      }

    val threadRecipient: Recipient = requireNotNull(ZonaRosaDatabase.threads.getRecipientForThreadId(records[0].threadId))

    return attachmentHelper
      .buildUpdatedModels(context, records)
      .map { ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, it, threadRecipient) }
  }
}

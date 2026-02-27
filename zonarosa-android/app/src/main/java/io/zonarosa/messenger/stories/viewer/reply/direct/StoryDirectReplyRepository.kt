package io.zonarosa.messenger.stories.viewer.reply.direct

import android.content.Context
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.database.model.ParentStoryId
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.messenger.mms.QuoteModel
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.sms.MessageSender
import java.util.concurrent.TimeUnit

class StoryDirectReplyRepository(context: Context) {

  private val context = context.applicationContext

  fun getStoryPost(storyId: Long): Single<MessageRecord> {
    return Single.fromCallable {
      ZonaRosaDatabase.messages.getMessageRecord(storyId)
    }.subscribeOn(Schedulers.io())
  }

  fun send(storyId: Long, groupDirectReplyRecipientId: RecipientId?, body: CharSequence, bodyRangeList: BodyRangeList?, isReaction: Boolean): Completable {
    return Completable.create { emitter ->
      val message = ZonaRosaDatabase.messages.getMessageRecord(storyId) as MmsMessageRecord
      val (recipient, threadId) = if (groupDirectReplyRecipientId == null) {
        message.fromRecipient to message.threadId
      } else {
        val resolved = Recipient.resolved(groupDirectReplyRecipientId)
        resolved to ZonaRosaDatabase.threads.getOrCreateThreadIdFor(resolved)
      }

      val quoteAuthor: Recipient = message.fromRecipient

      MessageSender.send(
        context,
        OutgoingMessage(
          threadRecipient = recipient,
          body = body.toString(),
          sentTimeMillis = System.currentTimeMillis(),
          expiresIn = TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong()),
          parentStoryId = ParentStoryId.DirectReply(storyId),
          isStoryReaction = isReaction,
          outgoingQuote = QuoteModel(message.dateSent, quoteAuthor.id, message.body, false, message.slideDeck.asAttachments().firstOrNull(), null, QuoteModel.Type.NORMAL, message.messageRanges),
          bodyRanges = bodyRangeList,
          isSecure = true
        ),
        threadId,
        MessageSender.SendType.ZONAROSA,
        null
      ) {
        emitter.onComplete()
      }
    }.subscribeOn(Schedulers.io())
  }
}

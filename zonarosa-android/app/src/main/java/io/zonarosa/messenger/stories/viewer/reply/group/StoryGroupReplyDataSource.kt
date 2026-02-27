package io.zonarosa.messenger.stories.viewer.reply.group

import io.zonarosa.paging.PagedDataSource
import io.zonarosa.messenger.conversation.ConversationMessage
import io.zonarosa.messenger.database.MessageTable
import io.zonarosa.messenger.database.MessageTypes
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.MessageId
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.recipients.Recipient

class StoryGroupReplyDataSource(private val parentStoryId: Long) : PagedDataSource<MessageId, ReplyBody> {
  override fun size(): Int {
    return ZonaRosaDatabase.messages.getNumberOfStoryReplies(parentStoryId)
  }

  override fun load(start: Int, length: Int, totalSize: Int, cancellationZonaRosa: PagedDataSource.CancellationZonaRosa): MutableList<ReplyBody> {
    val results: MutableList<ReplyBody> = ArrayList(length)
    ZonaRosaDatabase.messages.getStoryReplies(parentStoryId).use { cursor ->
      cursor.moveToPosition(start - 1)
      val mmsReader = MessageTable.MmsReader(cursor)
      while (cursor.moveToNext() && cursor.position < start + length) {
        results.add(readRowFromRecord(mmsReader.getCurrent() as MmsMessageRecord))
      }
    }

    return results
  }

  override fun load(key: MessageId): ReplyBody {
    return readRowFromRecord(ZonaRosaDatabase.messages.getMessageRecord(key.id) as MmsMessageRecord)
  }

  override fun getKey(data: ReplyBody): MessageId {
    return data.key
  }

  private fun readRowFromRecord(record: MmsMessageRecord): ReplyBody {
    val threadRecipient: Recipient = requireNotNull(ZonaRosaDatabase.threads.getRecipientForThreadId(record.threadId))
    return when {
      record.isRemoteDelete -> ReplyBody.RemoteDelete(record)
      MessageTypes.isStoryReaction(record.type) -> ReplyBody.Reaction(record)
      else -> ReplyBody.Text(
        ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(AppDependencies.application, record, threadRecipient)
      )
    }
  }
}

package io.zonarosa.messenger.stories.viewer.page

import android.content.Context
import android.net.Uri
import androidx.annotation.CheckResult
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.BreakIteratorCompat
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.conversation.ConversationMessage
import io.zonarosa.messenger.database.DatabaseObserver
import io.zonarosa.messenger.database.GroupReceiptTable
import io.zonarosa.messenger.database.NoSuchMessageException
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.MessageId
import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.database.model.databaseprotos.StoryTextPost
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.MultiDeviceViewedUpdateJob
import io.zonarosa.messenger.jobs.SendViewedReceiptJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.sms.MessageSender
import io.zonarosa.messenger.stories.Stories

/**
 * Open for testing.
 */
open class StoryViewerPageRepository(context: Context, private val storyViewStateCache: StoryViewStateCache) {

  companion object {
    private val TAG = Log.tag(StoryViewerPageRepository::class.java)
  }

  private val context = context.applicationContext

  fun isReadReceiptsEnabled(): Boolean = ZonaRosaStore.story.viewedReceiptsEnabled

  private fun getStoryRecords(recipientId: RecipientId, isOutgoingOnly: Boolean): Observable<List<MessageRecord>> {
    return Observable.create { emitter ->
      val recipient = Recipient.resolved(recipientId)

      fun refresh() {
        val stories = if (recipient.isMyStory) {
          ZonaRosaDatabase.messages.getAllOutgoingStories(false, 100)
        } else if (isOutgoingOnly) {
          ZonaRosaDatabase.messages.getOutgoingStoriesTo(recipientId)
        } else {
          ZonaRosaDatabase.messages.getAllStoriesFor(recipientId, 100)
        }

        val results = stories.filterNot {
          recipient.isMyStory && it.toRecipient.isGroup
        }

        emitter.onNext(results)
      }

      val storyObserver = DatabaseObserver.Observer {
        refresh()
      }

      AppDependencies.databaseObserver.registerStoryObserver(recipientId, storyObserver)
      emitter.setCancellable {
        AppDependencies.databaseObserver.unregisterObserver(storyObserver)
      }

      refresh()
    }
  }

  private fun getStoryPostFromRecord(recipientId: RecipientId, originalRecord: MessageRecord): Observable<StoryPost> {
    return Observable.create { emitter ->
      fun refresh(record: MessageRecord) {
        val recipient = Recipient.resolved(recipientId)
        val viewedCount = ZonaRosaDatabase.groupReceipts.getGroupReceiptInfo(record.id).filter { it.status == GroupReceiptTable.STATUS_VIEWED }.size
        val story = StoryPost(
          id = record.id,
          sender = record.fromRecipient,
          group = if (recipient.isGroup) recipient else null,
          distributionList = if (record.toRecipient.isDistributionList) record.toRecipient else null,
          viewCount = viewedCount,
          replyCount = ZonaRosaDatabase.messages.getNumberOfStoryReplies(record.id),
          dateInMilliseconds = record.dateSent,
          content = getContent(record as MmsMessageRecord),
          conversationMessage = ConversationMessage.ConversationMessageFactory.createWithUnresolvedData(context, record, recipient),
          allowsReplies = record.storyType.isStoryWithReplies,
          hasSelfViewed = storyViewStateCache.getOrPut(record.id, if (record.isOutgoing) true else record.isViewed())
        )

        emitter.onNext(story)
      }

      val recordId = originalRecord.id
      val threadId = originalRecord.threadId
      val recipient = Recipient.resolved(recipientId)

      val messageUpdateObserver = DatabaseObserver.MessageObserver {
        if (it.id == recordId) {
          try {
            val messageRecord = ZonaRosaDatabase.messages.getMessageRecord(recordId)
            if (messageRecord.isRemoteDelete) {
              emitter.onComplete()
            } else {
              refresh(messageRecord)
            }
          } catch (e: NoSuchMessageException) {
            emitter.onComplete()
          }
        }
      }

      val conversationObserver = DatabaseObserver.Observer {
        try {
          refresh(ZonaRosaDatabase.messages.getMessageRecord(recordId))
        } catch (e: NoSuchMessageException) {
          Log.w(TAG, "Message deleted during content refresh.", e)
        }
      }

      AppDependencies.databaseObserver.registerConversationObserver(threadId, conversationObserver)
      AppDependencies.databaseObserver.registerMessageUpdateObserver(messageUpdateObserver)

      val messageInsertObserver = DatabaseObserver.MessageObserver {
        refresh(ZonaRosaDatabase.messages.getMessageRecord(recordId))
      }

      if (recipient.isGroup) {
        AppDependencies.databaseObserver.registerMessageInsertObserver(threadId, messageInsertObserver)
      }

      emitter.setCancellable {
        AppDependencies.databaseObserver.unregisterObserver(conversationObserver)
        AppDependencies.databaseObserver.unregisterObserver(messageUpdateObserver)

        if (recipient.isGroup) {
          AppDependencies.databaseObserver.unregisterObserver(messageInsertObserver)
        }
      }

      refresh(originalRecord)
    }
  }

  fun forceDownload(post: StoryPost): Completable {
    return Stories.enqueueAttachmentsFromStoryForDownload(post.conversationMessage.messageRecord as MmsMessageRecord, true)
  }

  fun getStoryPostsFor(recipientId: RecipientId, isOutgoingOnly: Boolean): Observable<List<StoryPost>> {
    return getStoryRecords(recipientId, isOutgoingOnly)
      .switchMap { records ->
        val posts: List<Observable<StoryPost>> = records.map {
          getStoryPostFromRecord(recipientId, it).distinctUntilChanged()
        }
        if (posts.isEmpty()) {
          Observable.just(emptyList())
        } else {
          Observable.combineLatest(posts) { it.filterIsInstance<StoryPost>() }
        }
      }.observeOn(Schedulers.io())
  }

  fun hideStory(recipientId: RecipientId): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.recipients.setHideStory(recipientId, true)
    }.subscribeOn(Schedulers.io())
  }

  fun unhideStory(recipientId: RecipientId): Completable {
    return Completable.fromAction {
      ZonaRosaDatabase.recipients.setHideStory(recipientId, false)
    }.subscribeOn(Schedulers.io())
  }

  fun markViewed(storyPost: StoryPost) {
    if (!storyPost.conversationMessage.messageRecord.isOutgoing) {
      ZonaRosaExecutors.SERIAL.execute {
        val markedMessageInfo = ZonaRosaDatabase.messages.setIncomingMessageViewed(storyPost.id)
        if (markedMessageInfo != null) {
          AppDependencies.databaseObserver.notifyConversationListListeners()

          if (storyPost.sender.isReleaseNotes) {
            ZonaRosaStore.story.userHasViewedOnboardingStory = true
            Stories.onStorySettingsChanged(Recipient.self().id)
          } else {
            AppDependencies.jobManager.add(
              SendViewedReceiptJob(
                markedMessageInfo.threadId,
                storyPost.sender.id,
                markedMessageInfo.syncMessageId.timetamp,
                MessageId(storyPost.id)
              )
            )
            MultiDeviceViewedUpdateJob.enqueue(listOf(markedMessageInfo.syncMessageId))

            val recipientId = storyPost.group?.id ?: storyPost.sender.id
            ZonaRosaDatabase.recipients.updateLastStoryViewTimestamp(recipientId)
            Stories.enqueueNextStoriesForDownload(recipientId, true, 5)
          }
        }
      }
    }
  }

  @CheckResult
  fun resend(messageRecord: MessageRecord): Completable {
    return Completable.fromAction {
      MessageSender.resend(AppDependencies.application, messageRecord)
    }.subscribeOn(Schedulers.io())
  }

  private fun getContent(record: MmsMessageRecord): StoryPost.Content {
    return if (record.storyType.isTextStory || record.slideDeck.asAttachments().isEmpty()) {
      StoryPost.Content.TextContent(
        uri = Uri.parse("story_text_post://${record.id}"),
        recordId = record.id,
        hasBody = canParseToTextStory(record.body),
        length = getTextStoryLength(record.body)
      )
    } else {
      StoryPost.Content.AttachmentContent(
        attachment = record.slideDeck.asAttachments().first()
      )
    }
  }

  private fun getTextStoryLength(body: String): Int {
    return if (canParseToTextStory(body)) {
      val breakIteratorCompat = BreakIteratorCompat.getInstance()
      breakIteratorCompat.setText(StoryTextPost.ADAPTER.decode(Base64.decode(body)).body)
      breakIteratorCompat.countBreaks()
    } else {
      0
    }
  }

  private fun canParseToTextStory(body: String): Boolean {
    return if (body.isNotEmpty()) {
      try {
        StoryTextPost.ADAPTER.decode(Base64.decode(body))
        return true
      } catch (e: Exception) {
        false
      }
    } else {
      false
    }
  }
}

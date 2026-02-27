package io.zonarosa.messenger.components.settings.app.internal

import android.content.Context
import org.json.JSONObject
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.database.MessageTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.RemoteMegaphoneRecord
import io.zonarosa.messenger.database.model.addButton
import io.zonarosa.messenger.database.model.addStyle
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.emoji.EmojiFiles
import io.zonarosa.messenger.jobs.AttachmentDownloadJob
import io.zonarosa.messenger.jobs.CreateReleaseChannelJob
import io.zonarosa.messenger.jobs.FetchRemoteMegaphoneImageJob
import io.zonarosa.messenger.jobs.InAppPaymentRecurringContextJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.notifications.v2.ConversationId
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.releasechannel.ReleaseChannel
import java.util.UUID
import kotlin.time.Duration.Companion.days

class InternalSettingsRepository(context: Context) {

  private val context = context.applicationContext

  fun getEmojiVersionInfo(consumer: (EmojiFiles.Version?) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      consumer(EmojiFiles.Version.readVersion(context))
    }
  }

  fun enqueueSubscriptionRedemption() {
    ZonaRosaExecutors.BOUNDED.execute {
      val latest = ZonaRosaDatabase.inAppPayments.getByLatestEndOfPeriod(InAppPaymentType.RECURRING_DONATION)
      if (latest != null) {
        InAppPaymentRecurringContextJob.createJobChain(latest).enqueue()
      }
    }
  }

  fun addSampleReleaseNote(callToAction: String) {
    ZonaRosaExecutors.UNBOUNDED.execute {
      AppDependencies.jobManager.runSynchronously(CreateReleaseChannelJob.create(), 5000)

      val title = "Release Note Title"
      val bodyText = "Release note body. Aren't I awesome?"
      val body = "$title\n\n$bodyText"
      val bodyRangeList = BodyRangeList.Builder()
        .addStyle(BodyRangeList.BodyRange.Style.BOLD, 0, title.length)

      bodyRangeList.addButton("Call to Action Text", callToAction, body.lastIndex, 0)

      val recipientId = ZonaRosaStore.releaseChannel.releaseChannelRecipientId!!
      val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(Recipient.resolved(recipientId))

      val insertResult: MessageTable.InsertResult? = ReleaseChannel.insertReleaseChannelMessage(
        recipientId = recipientId,
        body = body,
        threadId = threadId,
        messageRanges = bodyRangeList.build(),
        media = "/static/release-notes/zonarosa.png",
        mediaWidth = 1800,
        mediaHeight = 720
      )

      ZonaRosaDatabase.messages.insertBoostRequestMessage(recipientId, threadId)

      if (insertResult != null) {
        ZonaRosaDatabase.attachments.getAttachmentsForMessage(insertResult.messageId)
          .forEach { AppDependencies.jobManager.add(AttachmentDownloadJob(insertResult.messageId, it.attachmentId, false)) }

        AppDependencies.messageNotifier.updateNotification(context, ConversationId.forConversation(insertResult.threadId))
      }
    }
  }

  fun addRemoteMegaphone(actionId: RemoteMegaphoneRecord.ActionId) {
    ZonaRosaExecutors.UNBOUNDED.execute {
      val record = RemoteMegaphoneRecord(
        uuid = UUID.randomUUID().toString(),
        priority = 100,
        countries = "*:1000000",
        minimumVersion = 1,
        doNotShowBefore = System.currentTimeMillis() - 2.days.inWholeMilliseconds,
        doNotShowAfter = System.currentTimeMillis() + 28.days.inWholeMilliseconds,
        showForNumberOfDays = 30,
        conditionalId = null,
        primaryActionId = actionId,
        secondaryActionId = RemoteMegaphoneRecord.ActionId.SNOOZE,
        imageUrl = "/static/release-notes/donate-heart.png",
        title = "Donate Test",
        body = "Donate body test.",
        primaryActionText = "Donate",
        secondaryActionText = "Snooze",
        primaryActionData = null,
        secondaryActionData = JSONObject("{ \"snoozeDurationDays\": [5, 7, 100] }")
      )

      ZonaRosaDatabase.remoteMegaphones.insert(record)

      if (record.imageUrl != null) {
        AppDependencies.jobManager.add(FetchRemoteMegaphoneImageJob(record.uuid, record.imageUrl))
      }
    }
  }
}

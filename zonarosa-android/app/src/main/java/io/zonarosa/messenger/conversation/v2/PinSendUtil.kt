package io.zonarosa.messenger.conversation.v2

import android.content.Context
import io.zonarosa.core.models.ServiceId
import io.zonarosa.messenger.crypto.ProfileKeyUtil
import io.zonarosa.messenger.database.MessageTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.GroupRecord
import io.zonarosa.messenger.database.model.MessageId
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.groups.GroupAccessControl
import io.zonarosa.messenger.groups.GroupNotAMemberException
import io.zonarosa.messenger.messages.GroupSendUtil
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.transport.UndeliverableMessageException
import io.zonarosa.messenger.util.GroupUtil
import io.zonarosa.service.api.crypto.ContentHint
import io.zonarosa.service.api.messages.SendMessageResult
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage.Companion.newBuilder
import java.io.IOException
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.milliseconds

/**
 * Functions used when pinning/unpinning messages
 */
object PinSendUtil {

  private val PIN_TERMINATE_TIMEOUT = 7000.milliseconds

  @Throws(IOException::class, GroupNotAMemberException::class, UndeliverableMessageException::class)
  fun sendPinMessage(applicationContext: Context, threadRecipient: Recipient, message: OutgoingMessage, destinations: List<Recipient>, includeSelf: Boolean, relatedMessageId: Long): List<SendMessageResult?> {
    val builder = newBuilder()
    val groupId = if (threadRecipient.isPushV2Group) threadRecipient.requireGroupId().requireV2() else null

    if (groupId != null) {
      val groupRecord: GroupRecord? = ZonaRosaDatabase.groups.getGroup(groupId).getOrNull()
      if (groupRecord != null && groupRecord.attributesAccessControl == GroupAccessControl.ONLY_ADMINS && !groupRecord.isAdmin(Recipient.self())) {
        throw UndeliverableMessageException("Non-admins cannot pin messages!")
      }
      GroupUtil.setDataMessageGroupContext(AppDependencies.application, builder, groupId)
    }

    val sentTime = System.currentTimeMillis()
    val message = builder
      .withTimestamp(sentTime)
      .withExpiration((message.expiresIn / 1000).toInt())
      .withProfileKey(ProfileKeyUtil.getSelfProfileKey().serialize())
      .withPinnedMessage(
        ZonaRosaServiceDataMessage.PinnedMessage(
          targetAuthor = ServiceId.parseOrThrow(message.messageExtras!!.pinnedMessage!!.targetAuthorAci),
          targetSentTimestamp = message.messageExtras.pinnedMessage.targetTimestamp,
          pinDurationInSeconds = message.messageExtras.pinnedMessage.pinDurationInSeconds.takeIf { it != MessageTable.PIN_FOREVER }?.toInt(),
          forever = (message.messageExtras.pinnedMessage.pinDurationInSeconds == MessageTable.PIN_FOREVER).takeIf { it }
        )
      )
      .build()

    return if (includeSelf) {
      listOf(AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(message))
    } else {
      GroupSendUtil.sendResendableDataMessage(
        applicationContext,
        groupId,
        null,
        destinations,
        false,
        ContentHint.RESENDABLE,
        MessageId(relatedMessageId),
        message,
        false,
        false,
        null
      ) { System.currentTimeMillis() - sentTime > PIN_TERMINATE_TIMEOUT.inWholeMilliseconds }
    }
  }

  @Throws(IOException::class, GroupNotAMemberException::class, UndeliverableMessageException::class)
  fun sendUnpinMessage(applicationContext: Context, threadRecipient: Recipient, targetAuthor: ServiceId, targetSentTimestamp: Long, destinations: List<Recipient>, includeSelf: Boolean, relatedMessageId: Long): List<SendMessageResult?> {
    val builder = newBuilder()
    val groupId = if (threadRecipient.isPushV2Group) threadRecipient.requireGroupId().requireV2() else null
    if (groupId != null) {
      val groupRecord: GroupRecord? = ZonaRosaDatabase.groups.getGroup(groupId).getOrNull()
      if (groupRecord != null && groupRecord.attributesAccessControl == GroupAccessControl.ONLY_ADMINS && !groupRecord.isAdmin(Recipient.self())) {
        throw UndeliverableMessageException("Non-admins cannot pin messages!")
      }

      GroupUtil.setDataMessageGroupContext(AppDependencies.application, builder, groupId)
    }

    val sentTime = System.currentTimeMillis()
    val message = builder
      .withTimestamp(sentTime)
      .withProfileKey(ProfileKeyUtil.getSelfProfileKey().serialize())
      .withUnpinnedMessage(
        ZonaRosaServiceDataMessage.UnpinnedMessage(
          targetAuthor = targetAuthor,
          targetSentTimestamp = targetSentTimestamp
        )
      )
      .build()

    return if (includeSelf) {
      listOf(AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(message))
    } else {
      GroupSendUtil.sendResendableDataMessage(
        applicationContext,
        groupId,
        null,
        destinations,
        false,
        ContentHint.RESENDABLE,
        MessageId(relatedMessageId),
        message,
        false,
        false,
        null
      ) { System.currentTimeMillis() - sentTime > PIN_TERMINATE_TIMEOUT.inWholeMilliseconds }
    }
  }
}

package io.zonarosa.messenger.messages

import android.content.Context
import com.mobilecoin.lib.exceptions.SerializationException
import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.models.backup.MediaRootBackupKey
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Hex
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.isNotEmpty
import io.zonarosa.core.util.orNull
import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.InvalidKeyException
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.ringrtc.CallException
import io.zonarosa.ringrtc.CallId
import io.zonarosa.ringrtc.CallLinkRootKey
import io.zonarosa.messenger.attachments.Attachment
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.attachments.TombstoneAttachment
import io.zonarosa.messenger.components.emoji.EmojiUtil
import io.zonarosa.messenger.contactshare.Contact
import io.zonarosa.messenger.crypto.SecurityEvent
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.database.CallTable
import io.zonarosa.messenger.database.GroupReceiptTable
import io.zonarosa.messenger.database.GroupTable
import io.zonarosa.messenger.database.MessageTable
import io.zonarosa.messenger.database.MessageTable.MarkedMessageInfo
import io.zonarosa.messenger.database.NoSuchMessageException
import io.zonarosa.messenger.database.PaymentMetaDataUtil
import io.zonarosa.messenger.database.SentStorySyncManifest
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.DistributionListId
import io.zonarosa.messenger.database.model.Mention
import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.database.model.ParentStoryId
import io.zonarosa.messenger.database.model.ParentStoryId.DirectReply
import io.zonarosa.messenger.database.model.ParentStoryId.GroupReply
import io.zonarosa.messenger.database.model.StickerPackId
import io.zonarosa.messenger.database.model.StoryType
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList
import io.zonarosa.messenger.database.model.databaseprotos.GiftBadge
import io.zonarosa.messenger.database.model.databaseprotos.MessageExtras
import io.zonarosa.messenger.database.model.databaseprotos.PinnedMessage
import io.zonarosa.messenger.database.model.databaseprotos.PollTerminate
import io.zonarosa.messenger.database.model.toBodyRangeList
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.groups.BadGroupIdException
import io.zonarosa.messenger.groups.GroupChangeBusyException
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.jobs.AttachmentDownloadJob
import io.zonarosa.messenger.jobs.AttachmentUploadJob
import io.zonarosa.messenger.jobs.MultiDeviceAttachmentBackfillMissingJob
import io.zonarosa.messenger.jobs.MultiDeviceAttachmentBackfillUpdateJob
import io.zonarosa.messenger.jobs.MultiDeviceBlockedUpdateJob
import io.zonarosa.messenger.jobs.MultiDeviceConfigurationUpdateJob
import io.zonarosa.messenger.jobs.MultiDeviceContactSyncJob
import io.zonarosa.messenger.jobs.MultiDeviceContactUpdateJob
import io.zonarosa.messenger.jobs.MultiDeviceKeysUpdateJob
import io.zonarosa.messenger.jobs.MultiDeviceStickerPackSyncJob
import io.zonarosa.messenger.jobs.PushProcessEarlyMessagesJob
import io.zonarosa.messenger.jobs.RefreshCallLinkDetailsJob
import io.zonarosa.messenger.jobs.RefreshOwnProfileJob
import io.zonarosa.messenger.jobs.StickerPackDownloadJob
import io.zonarosa.messenger.jobs.StorageSyncJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.linkpreview.LinkPreview
import io.zonarosa.messenger.messages.MessageContentProcessor.Companion.log
import io.zonarosa.messenger.messages.MessageContentProcessor.Companion.warn
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.expireTimerDuration
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.groupId
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.groupMasterKey
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.hasGroupContext
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.hasRemoteDelete
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.isEmptyGroupV2Message
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.isEndSession
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.isExpirationUpdate
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.isGroupV2Update
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.isMediaMessage
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.isUnidentified
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.serviceIdsToUnidentifiedStatus
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.toMobileCoinMoney
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.toPointer
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.toPointersWithinLimit
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.toZonaRosaServiceAttachmentPointer
import io.zonarosa.messenger.messages.ZonaRosaServiceProtoUtil.type
import io.zonarosa.messenger.mms.MmsException
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.messenger.mms.QuoteModel
import io.zonarosa.messenger.notifications.MarkReadReceiver
import io.zonarosa.messenger.payments.MobileCoinPublicAddress
import io.zonarosa.messenger.polls.Poll
import io.zonarosa.messenger.ratelimit.RateLimitUtil
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.RecipientUtil
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkState
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.messenger.util.EarlyMessageCacheEntry
import io.zonarosa.messenger.util.IdentityUtil
import io.zonarosa.messenger.util.MediaUtil
import io.zonarosa.messenger.util.MessageConstraintsUtil
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.messenger.util.ZonaRosaE164Util
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.messenger.util.hasGiftBadge
import io.zonarosa.service.api.crypto.EnvelopeMetadata
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer
import io.zonarosa.service.api.push.DistributionId
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import io.zonarosa.service.internal.push.AddressableMessage
import io.zonarosa.service.internal.push.Content
import io.zonarosa.service.internal.push.ConversationIdentifier
import io.zonarosa.service.internal.push.DataMessage
import io.zonarosa.service.internal.push.EditMessage
import io.zonarosa.service.internal.push.Envelope
import io.zonarosa.service.internal.push.StoryMessage
import io.zonarosa.service.internal.push.SyncMessage
import io.zonarosa.service.internal.push.SyncMessage.Blocked
import io.zonarosa.service.internal.push.SyncMessage.CallLinkUpdate
import io.zonarosa.service.internal.push.SyncMessage.CallLogEvent
import io.zonarosa.service.internal.push.SyncMessage.Configuration
import io.zonarosa.service.internal.push.SyncMessage.FetchLatest
import io.zonarosa.service.internal.push.SyncMessage.MessageRequestResponse
import io.zonarosa.service.internal.push.SyncMessage.Read
import io.zonarosa.service.internal.push.SyncMessage.Request
import io.zonarosa.service.internal.push.SyncMessage.Sent
import io.zonarosa.service.internal.push.SyncMessage.StickerPackOperation
import io.zonarosa.service.internal.push.SyncMessage.ViewOnceOpen
import io.zonarosa.service.internal.push.Verified
import java.io.IOException
import java.util.Optional
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import io.zonarosa.service.internal.util.Util as Utils

object SyncMessageProcessor {

  fun process(
    context: Context,
    senderRecipient: Recipient,
    threadRecipient: Recipient,
    envelope: Envelope,
    content: Content,
    metadata: EnvelopeMetadata,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ) {
    val syncMessage = content.syncMessage!!

    when {
      syncMessage.sent != null -> handleSynchronizeSentMessage(context, envelope, content, metadata, syncMessage.sent!!, senderRecipient, threadRecipient, earlyMessageCacheEntry)
      syncMessage.request != null -> handleSynchronizeRequestMessage(context, syncMessage.request!!, envelope.timestamp!!)
      syncMessage.read.isNotEmpty() -> handleSynchronizeReadMessage(context, syncMessage.read, envelope.timestamp!!, earlyMessageCacheEntry)
      syncMessage.viewed.isNotEmpty() -> handleSynchronizeViewedMessage(context, syncMessage.viewed, envelope.timestamp!!)
      syncMessage.viewOnceOpen != null -> handleSynchronizeViewOnceOpenMessage(context, syncMessage.viewOnceOpen!!, envelope.timestamp!!, earlyMessageCacheEntry)
      syncMessage.verified != null -> handleSynchronizeVerifiedMessage(context, syncMessage.verified!!)
      syncMessage.stickerPackOperation.isNotEmpty() -> handleSynchronizeStickerPackOperation(syncMessage.stickerPackOperation, envelope.timestamp!!)
      syncMessage.configuration != null -> handleSynchronizeConfigurationMessage(context, syncMessage.configuration!!, envelope.timestamp!!)
      syncMessage.blocked != null -> handleSynchronizeBlockedListMessage(syncMessage.blocked!!, envelope.timestamp!!)
      syncMessage.fetchLatest?.type != null -> handleSynchronizeFetchMessage(syncMessage.fetchLatest!!.type!!, envelope.timestamp!!)
      syncMessage.messageRequestResponse != null -> handleSynchronizeMessageRequestResponse(syncMessage.messageRequestResponse!!, envelope.timestamp!!)
      syncMessage.outgoingPayment != null -> handleSynchronizeOutgoingPayment(syncMessage.outgoingPayment!!, envelope.timestamp!!)
      syncMessage.contacts != null -> handleSynchronizeContacts(syncMessage.contacts!!, envelope.timestamp!!)
      syncMessage.keys != null -> handleSynchronizeKeys(syncMessage.keys!!, envelope.timestamp!!)
      syncMessage.callEvent != null -> handleSynchronizeCallEvent(syncMessage.callEvent!!, envelope.timestamp!!)
      syncMessage.callLinkUpdate != null -> handleSynchronizeCallLink(syncMessage.callLinkUpdate!!, envelope.timestamp!!)
      syncMessage.callLogEvent != null -> handleSynchronizeCallLogEvent(syncMessage.callLogEvent!!, envelope.timestamp!!)
      syncMessage.deleteForMe != null -> handleSynchronizeDeleteForMe(context, syncMessage.deleteForMe!!, envelope.timestamp!!, earlyMessageCacheEntry)
      syncMessage.attachmentBackfillRequest != null -> handleSynchronizeAttachmentBackfillRequest(syncMessage.attachmentBackfillRequest!!, envelope.timestamp!!)
      syncMessage.attachmentBackfillResponse != null -> warn(envelope.timestamp!!, "Contains a backfill response, but we don't handle these!")
      else -> warn(envelope.timestamp!!, "Contains no known sync types...")
    }
  }

  @Throws(StorageFailedException::class, BadGroupIdException::class, IOException::class, GroupChangeBusyException::class)
  private fun handleSynchronizeSentMessage(
    context: Context,
    envelope: Envelope,
    content: Content,
    metadata: EnvelopeMetadata,
    sent: Sent,
    senderRecipient: Recipient,
    threadRecipient: Recipient,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ) {
    log(envelope.timestamp!!, "Processing sent transcript for message with ID ${sent.timestamp!!}")

    try {
      handlePniIdentityKeys(envelope, sent)

      if (sent.storyMessage != null || sent.storyMessageRecipients.isNotEmpty()) {
        handleSynchronizeSentStoryMessage(envelope, sent)
        return
      }

      if (sent.editMessage != null) {
        handleSynchronizeSentEditMessage(context, envelope, sent, senderRecipient, earlyMessageCacheEntry)
        return
      }

      if (sent.isRecipientUpdate == true) {
        handleGroupRecipientUpdate(sent, envelope.timestamp!!)
        return
      }

      val dataMessage = if (sent.message != null) {
        sent.message!!
      } else {
        warn(envelope.timestamp!!, "Sync message missing nested message to sync")
        return
      }

      val groupId: GroupId.V2? = if (dataMessage.hasGroupContext) GroupId.v2(dataMessage.groupV2!!.groupMasterKey) else null

      if (groupId != null) {
        if (MessageContentProcessor.handleGv2PreProcessing(context, envelope.timestamp!!, content, metadata, groupId, dataMessage.groupV2!!, senderRecipient) == MessageContentProcessor.Gv2PreProcessResult.IGNORE) {
          return
        }
      }

      var threadId: Long = -1
      when {
        dataMessage.isEndSession -> threadId = handleSynchronizeSentEndSessionMessage(context, sent, envelope.timestamp!!)
        dataMessage.isGroupV2Update -> {
          handleSynchronizeSentGv2Update(context, envelope, sent)
          threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(getSyncMessageDestination(sent))
        }
        dataMessage.groupCallUpdate != null -> DataMessageProcessor.handleGroupCallUpdateMessage(envelope, senderRecipient.id, groupId)
        dataMessage.isEmptyGroupV2Message -> warn(envelope.timestamp!!, "Empty GV2 message! Doing nothing.")
        dataMessage.isExpirationUpdate -> threadId = handleSynchronizeSentExpirationUpdate(sent)
        dataMessage.storyContext != null -> threadId = handleSynchronizeSentStoryReply(sent, envelope.timestamp!!)
        dataMessage.reaction != null -> {
          DataMessageProcessor.handleReaction(context, envelope, dataMessage, senderRecipient.id, earlyMessageCacheEntry)
          threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(getSyncMessageDestination(sent))
        }
        dataMessage.hasRemoteDelete -> DataMessageProcessor.handleRemoteDelete(context, envelope, dataMessage, senderRecipient.id, earlyMessageCacheEntry)
        dataMessage.isMediaMessage -> threadId = handleSynchronizeSentMediaMessage(context, sent, envelope.timestamp!!, senderRecipient, threadRecipient)
        dataMessage.pollCreate != null -> threadId = handleSynchronizedPollCreate(envelope, dataMessage, sent, senderRecipient)
        dataMessage.pollVote != null -> {
          DataMessageProcessor.handlePollVote(context, envelope, dataMessage, senderRecipient, earlyMessageCacheEntry)
          threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(getSyncMessageDestination(sent))
        }
        dataMessage.pollTerminate != null -> threadId = handleSynchronizedPollEnd(envelope, dataMessage, sent, senderRecipient, earlyMessageCacheEntry)
        dataMessage.pinMessage != null -> threadId = handleSynchronizedPinMessage(envelope, dataMessage, sent, senderRecipient, earlyMessageCacheEntry)
        dataMessage.unpinMessage != null -> {
          DataMessageProcessor.handleUnpinMessage(envelope, dataMessage, senderRecipient, threadRecipient, earlyMessageCacheEntry)
          threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(getSyncMessageDestination(sent))
        }
        dataMessage.adminDelete != null -> {
          DataMessageProcessor.handleAdminRemoteDelete(envelope, dataMessage, senderRecipient, threadRecipient, earlyMessageCacheEntry)
          threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(getSyncMessageDestination(sent))
        }
        else -> threadId = handleSynchronizeSentTextMessage(sent, envelope.timestamp!!)
      }

      if (groupId != null && ZonaRosaDatabase.groups.isUnknownGroup(groupId)) {
        DataMessageProcessor.handleUnknownGroupMessage(envelope.timestamp!!, dataMessage.groupV2!!)
      }

      if (dataMessage.profileKey.isNotEmpty()) {
        val recipient: Recipient = getSyncMessageDestination(sent)
        if (!recipient.isSystemContact && !recipient.isProfileSharing) {
          ZonaRosaDatabase.recipients.setProfileSharing(recipient.id, true)
        }
      }

      if (threadId != -1L) {
        ZonaRosaDatabase.threads.setRead(threadId)
        AppDependencies.messageNotifier.updateNotification(context)
      }

      if (ZonaRosaStore.rateLimit.needsRecaptcha()) {
        log(envelope.timestamp!!, "Got a sent transcript while in reCAPTCHA mode. Assuming we're good to message again.")
        RateLimitUtil.retryAllRateLimitedMessages(context)
      }

      AppDependencies.messageNotifier.setLastDesktopActivityTimestamp(sent.timestamp!!)
    } catch (e: MmsException) {
      throw StorageFailedException(e, metadata.sourceServiceId.toString(), metadata.sourceDeviceId)
    }
  }

  private fun handlePniIdentityKeys(envelope: Envelope, sent: Sent) {
    for (status in sent.unidentifiedStatus) {
      if (status.destinationPniIdentityKey == null) {
        continue
      }

      val pni = PNI.parsePrefixedOrNull(status.destinationServiceId, status.destinationServiceIdBinary)
      if (pni == null) {
        continue
      }

      val address = ZonaRosaProtocolAddress(pni.toString(), ZonaRosaServiceAddress.DEFAULT_DEVICE_ID)

      if (AppDependencies.protocolStore.aci().identities().getIdentity(address) != null) {
        log(envelope.timestamp!!, "Ignoring identity on sent transcript for $pni because we already have one.")
        continue
      }

      try {
        log(envelope.timestamp!!, "Saving identity from sent transcript for $pni")
        val identityKey = IdentityKey(status.destinationPniIdentityKey!!.toByteArray())
        AppDependencies.protocolStore.aci().identities().saveIdentity(address, identityKey)
      } catch (e: InvalidKeyException) {
        warn(envelope.timestamp!!, "Failed to deserialize identity key for $pni")
      }
    }
  }

  private fun getSyncMessageDestination(message: Sent): Recipient {
    return if (message.message.hasGroupContext) {
      Recipient.externalPossiblyMigratedGroup(GroupId.v2(message.message!!.groupV2!!.groupMasterKey))
    } else {
      Recipient.externalPush(ZonaRosaServiceAddress(ServiceId.parseOrThrow(message.destinationServiceId, message.destinationServiceIdBinary), message.destinationE164))
    }
  }

  @Throws(MmsException::class)
  private fun handleSynchronizeSentEditMessage(
    context: Context,
    envelope: Envelope,
    sent: Sent,
    senderRecipient: Recipient,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ) {
    val editMessage: EditMessage = sent.editMessage!!
    val targetSentTimestamp: Long = editMessage.targetSentTimestamp!!
    val targetMessage: MessageRecord? = ZonaRosaDatabase.messages.getMessageFor(targetSentTimestamp, senderRecipient.id)
    val senderRecipientId = senderRecipient.id

    if (targetMessage == null) {
      warn(envelope.timestamp!!, "[handleSynchronizeSentEditMessage] Could not find matching message! targetTimestamp: $targetSentTimestamp  author: $senderRecipientId")
      if (earlyMessageCacheEntry != null) {
        AppDependencies.earlyMessageCache.store(senderRecipientId, targetSentTimestamp, earlyMessageCacheEntry)
        PushProcessEarlyMessagesJob.enqueue()
      }
    } else if (MessageConstraintsUtil.isValidEditMessageReceive(targetMessage, senderRecipient, envelope.serverTimestamp!!)) {
      val message: DataMessage = editMessage.dataMessage!!
      val toRecipient: Recipient = if (message.hasGroupContext) {
        Recipient.externalPossiblyMigratedGroup(GroupId.v2(message.groupV2!!.groupMasterKey))
      } else {
        Recipient.externalPush(ServiceId.parseOrThrow(sent.destinationServiceId, sent.destinationServiceIdBinary))
      }

      if (message.isMediaMessage) {
        handleSynchronizeSentEditMediaMessage(targetMessage, toRecipient, sent, message, envelope.timestamp!!)
      } else {
        handleSynchronizeSentEditTextMessage(targetMessage, toRecipient, sent, message, envelope.timestamp!!)
      }
    } else {
      warn(envelope.timestamp!!, "[handleSynchronizeSentEditMessage] Invalid message edit! editTime: ${envelope.serverTimestamp}, targetTime: ${targetMessage.serverTimestamp}, sendAuthor: $senderRecipientId, targetAuthor: ${targetMessage.fromRecipient.id}")
    }
  }

  private fun handleSynchronizeSentEditTextMessage(
    targetMessage: MessageRecord,
    toRecipient: Recipient,
    sent: Sent,
    message: DataMessage,
    envelopeTimestamp: Long
  ) {
    log(envelopeTimestamp, "Synchronize sent edit text message for message: ${targetMessage.id}")

    val body = message.body ?: ""
    val bodyRanges = message.bodyRanges.filter { Utils.allAreNull(it.mentionAci, it.mentionAciBinary) }.toBodyRangeList()

    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(toRecipient)
    val isGroup = toRecipient.isGroup
    val messageId: Long

    if (isGroup) {
      val outgoingMessage = OutgoingMessage(
        recipient = toRecipient,
        body = body,
        timestamp = sent.timestamp!!,
        expiresIn = targetMessage.expiresIn,
        expireTimerVersion = targetMessage.expireTimerVersion,
        isSecure = true,
        bodyRanges = bodyRanges,
        messageToEdit = targetMessage.id
      )

      messageId = ZonaRosaDatabase.messages.insertMessageOutbox(outgoingMessage, threadId, false, GroupReceiptTable.STATUS_UNKNOWN, null).messageId
      updateGroupReceiptStatus(sent, messageId, toRecipient.requireGroupId())
    } else {
      val outgoingTextMessage = OutgoingMessage(
        threadRecipient = toRecipient,
        sentTimeMillis = sent.timestamp!!,
        body = body,
        expiresIn = targetMessage.expiresIn,
        expireTimerVersion = targetMessage.expireTimerVersion,
        isUrgent = true,
        isSecure = true,
        bodyRanges = bodyRanges,
        messageToEdit = targetMessage.id
      )
      messageId = ZonaRosaDatabase.messages.insertMessageOutbox(outgoingTextMessage, threadId, false, null).messageId
      ZonaRosaDatabase.messages.markUnidentified(messageId, sent.isUnidentified(toRecipient.serviceId.orNull()))
    }

    ZonaRosaDatabase.messages.markAsSent(messageId, true)
    if (targetMessage.expireStarted > 0) {
      ZonaRosaDatabase.messages.markExpireStarted(messageId, targetMessage.expireStarted)
      AppDependencies.expiringMessageManager.scheduleDeletion(messageId, true, targetMessage.expireStarted, targetMessage.expireStarted)
    }

    if (toRecipient.isSelf) {
      ZonaRosaDatabase.messages.incrementDeliveryReceiptCount(sent.timestamp!!, toRecipient.id, System.currentTimeMillis())
      ZonaRosaDatabase.messages.incrementReadReceiptCount(sent.timestamp!!, toRecipient.id, System.currentTimeMillis())
    }
  }

  private fun handleSynchronizeSentEditMediaMessage(
    targetMessage: MessageRecord,
    toRecipient: Recipient,
    sent: Sent,
    message: DataMessage,
    envelopeTimestamp: Long
  ) {
    log(envelopeTimestamp, "Synchronize sent edit media message for: ${targetMessage.id}")

    val targetQuote = (targetMessage as? MmsMessageRecord)?.quote
    val quote: QuoteModel? = if (targetQuote != null && message.quote != null) {
      QuoteModel(
        id = targetQuote.id,
        author = targetQuote.author,
        text = targetQuote.displayText.toString(),
        isOriginalMissing = targetQuote.isOriginalMissing,
        attachment = null,
        mentions = null,
        type = targetQuote.quoteType,
        bodyRanges = null
      )
    } else {
      null
    }

    val sharedContacts: List<Contact> = DataMessageProcessor.getContacts(message)
    val previews: List<LinkPreview> = DataMessageProcessor.getLinkPreviews(message.preview, message.body ?: "", false)
    val mentions: List<Mention> = DataMessageProcessor.getMentions(message.bodyRanges)
    val viewOnce: Boolean = message.isViewOnce == true
    val bodyRanges: BodyRangeList? = message.bodyRanges.toBodyRangeList()

    val syncAttachments = message.attachments.toPointersWithinLimit().filter {
      MediaUtil.SlideType.LONG_TEXT == MediaUtil.getSlideTypeFromContentType(it.contentType)
    }

    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(toRecipient)
    val mediaMessage = OutgoingMessage(
      recipient = toRecipient,
      body = message.body ?: "",
      attachments = syncAttachments.ifEmpty { (targetMessage as? MmsMessageRecord)?.slideDeck?.asAttachments() ?: emptyList() },
      timestamp = sent.timestamp!!,
      expiresIn = targetMessage.expiresIn,
      expireTimerVersion = targetMessage.expireTimerVersion,
      viewOnce = viewOnce,
      quote = quote,
      contacts = sharedContacts,
      previews = previews,
      mentions = mentions,
      bodyRanges = bodyRanges,
      isSecure = true,
      messageToEdit = targetMessage.id
    )

    val messageId: Long = ZonaRosaDatabase.messages.insertMessageOutbox(mediaMessage, threadId, false, GroupReceiptTable.STATUS_UNKNOWN, null).messageId

    if (toRecipient.isGroup) {
      updateGroupReceiptStatus(sent, messageId, toRecipient.requireGroupId())
    } else {
      ZonaRosaDatabase.messages.markUnidentified(messageId, sent.isUnidentified(toRecipient.serviceId.orNull()))
    }

    ZonaRosaDatabase.messages.markAsSent(messageId, true)

    val attachments: List<DatabaseAttachment> = ZonaRosaDatabase.attachments.getAttachmentsForMessage(messageId)

    if (targetMessage.expireStarted > 0) {
      ZonaRosaDatabase.messages.markExpireStarted(messageId, targetMessage.expireStarted)
      AppDependencies.expiringMessageManager.scheduleDeletion(messageId, true, targetMessage.expireStarted, targetMessage.expireStarted)
    }

    if (toRecipient.isSelf) {
      ZonaRosaDatabase.messages.incrementDeliveryReceiptCount(sent.timestamp!!, toRecipient.id, System.currentTimeMillis())
      ZonaRosaDatabase.messages.incrementReadReceiptCount(sent.timestamp!!, toRecipient.id, System.currentTimeMillis())
    }

    if (syncAttachments.isNotEmpty()) {
      ZonaRosaDatabase.runPostSuccessfulTransaction {
        val downloadJobs: List<AttachmentDownloadJob> = attachments.map { AttachmentDownloadJob(messageId = messageId, attachmentId = it.attachmentId, forceDownload = it.isSticker) }
        AppDependencies.jobManager.addAll(downloadJobs)
      }
    }
  }

  @Throws(MmsException::class)
  private fun handleSynchronizeSentStoryMessage(envelope: Envelope, sent: Sent) {
    log(envelope.timestamp!!, "Synchronize sent story message for " + sent.timestamp)

    val manifest = SentStorySyncManifest.fromRecipientsSet(sent.storyMessageRecipients)

    if (sent.isRecipientUpdate == true) {
      log(envelope.timestamp!!, "Processing recipient update for story message and exiting...")
      ZonaRosaDatabase.storySends.applySentStoryManifest(manifest, sent.timestamp!!)
      return
    }

    val storyMessage: StoryMessage = sent.storyMessage!!
    val distributionIds: Set<DistributionId> = manifest.getDistributionIdSet()
    val groupId: GroupId.V2? = storyMessage.group?.groupId
    val textStoryBody: String? = StoryMessageProcessor.serializeTextAttachment(storyMessage)
    val bodyRanges: BodyRangeList? = storyMessage.bodyRanges.toBodyRangeList()
    val storyType: StoryType = storyMessage.type

    val linkPreviews: List<LinkPreview> = DataMessageProcessor.getLinkPreviews(
      previews = listOfNotNull(storyMessage.textAttachment?.preview),
      body = "",
      isStoryEmbed = true
    )

    val attachments: List<Attachment> = listOfNotNull(storyMessage.fileAttachment?.toPointer())

    for (distributionId in distributionIds) {
      val distributionRecipientId = ZonaRosaDatabase.distributionLists.getOrCreateByDistributionId(distributionId, manifest)
      val distributionListRecipient = Recipient.resolved(distributionRecipientId)
      insertSentStoryMessage(sent, distributionListRecipient, null, textStoryBody, attachments, sent.timestamp!!, storyType, linkPreviews, bodyRanges)
    }

    if (groupId != null) {
      val groupRecipient: Optional<RecipientId> = ZonaRosaDatabase.recipients.getByGroupId(groupId)
      if (groupRecipient.isPresent) {
        insertSentStoryMessage(sent, Recipient.resolved(groupRecipient.get()), groupId, textStoryBody, attachments, sent.timestamp!!, storyType, linkPreviews, bodyRanges)
      }
    }

    ZonaRosaDatabase.storySends.applySentStoryManifest(manifest, sent.timestamp!!)
  }

  @Throws(MmsException::class)
  private fun insertSentStoryMessage(
    sent: Sent,
    recipient: Recipient,
    groupId: GroupId.V2?,
    textStoryBody: String?,
    pendingAttachments: List<Attachment>,
    sentAtTimestamp: Long,
    storyType: StoryType,
    linkPreviews: List<LinkPreview>,
    bodyRanges: BodyRangeList?
  ) {
    if (ZonaRosaDatabase.messages.isOutgoingStoryAlreadyInDatabase(recipient.id, sentAtTimestamp)) {
      warn(sentAtTimestamp, "Already inserted this story.")
      return
    }

    val mediaMessage = OutgoingMessage(
      recipient = recipient,
      body = textStoryBody,
      attachments = pendingAttachments,
      timestamp = sentAtTimestamp,
      storyType = storyType,
      previews = linkPreviews,
      bodyRanges = bodyRanges,
      isSecure = true
    )

    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId: Long = ZonaRosaDatabase.messages.insertMessageOutbox(mediaMessage, threadId, false, GroupReceiptTable.STATUS_UNDELIVERED, null).messageId

    if (groupId != null) {
      updateGroupReceiptStatus(sent, messageId, recipient.requireGroupId())
    } else if (recipient.distributionListId.isPresent) {
      updateGroupReceiptStatusForDistributionList(sent, messageId, recipient.distributionListId.get())
    } else {
      ZonaRosaDatabase.messages.markUnidentified(messageId, sent.isUnidentified(recipient.serviceId.orNull()))
    }

    ZonaRosaDatabase.messages.markAsSent(messageId, true)

    val allAttachments = ZonaRosaDatabase.attachments.getAttachmentsForMessage(messageId)
    val attachments: List<DatabaseAttachment> = allAttachments.filterNot { it.isSticker }

    if (recipient.isSelf) {
      ZonaRosaDatabase.messages.incrementDeliveryReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
      ZonaRosaDatabase.messages.incrementReadReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
    }

    ZonaRosaDatabase.runPostSuccessfulTransaction {
      val downloadJobs: List<AttachmentDownloadJob> = attachments.map { AttachmentDownloadJob(messageId = messageId, attachmentId = it.attachmentId, forceDownload = false) }
      AppDependencies.jobManager.addAll(downloadJobs)
    }
  }

  private fun handleGroupRecipientUpdate(sent: Sent, envelopeTimestamp: Long) {
    log(envelopeTimestamp, "Group recipient update.")

    val recipient = getSyncMessageDestination(sent)
    if (!recipient.isGroup) {
      warn("Got recipient update for a non-group message! Skipping.")
      return
    }

    val record = ZonaRosaDatabase.messages.getMessageFor(sent.timestamp!!, Recipient.self().id)
    if (record == null) {
      warn("Got recipient update for non-existing message! Skipping.")
      return
    }

    updateGroupReceiptStatus(sent, record.id, recipient.requireGroupId())
  }

  private fun updateGroupReceiptStatus(sent: Sent, messageId: Long, groupString: GroupId) {
    val messageRecipientIds: Map<RecipientId, Boolean> = sent.serviceIdsToUnidentifiedStatus.mapKeys { RecipientId.from(it.key) }
    val members: List<RecipientId> = ZonaRosaDatabase.groups.getGroupMembers(groupString, GroupTable.MemberSet.FULL_MEMBERS_EXCLUDING_SELF).map { it.id }
    val localReceipts: Map<RecipientId, Int> = ZonaRosaDatabase.groupReceipts.getGroupReceiptInfo(messageId).associate { it.recipientId to it.status }

    for (messageRecipientId in messageRecipientIds.keys) {
      if ((localReceipts[messageRecipientId] ?: GroupReceiptTable.STATUS_UNKNOWN) < GroupReceiptTable.STATUS_UNDELIVERED) {
        ZonaRosaDatabase.groupReceipts.update(messageRecipientId, messageId, GroupReceiptTable.STATUS_UNDELIVERED, sent.timestamp!!)
      } else if (!localReceipts.containsKey(messageRecipientId)) {
        ZonaRosaDatabase.groupReceipts.insert(listOf(messageRecipientId), messageId, GroupReceiptTable.STATUS_UNDELIVERED, sent.timestamp!!)
      }
    }

    val unidentifiedStatus = members.map { Pair(it, messageRecipientIds[it] ?: false) }

    ZonaRosaDatabase.groupReceipts.setUnidentified(unidentifiedStatus, messageId)
  }

  private fun updateGroupReceiptStatusForDistributionList(sent: Sent, messageId: Long, distributionListId: DistributionListId) {
    val messageRecipientIds: Map<RecipientId, Boolean> = sent.serviceIdsToUnidentifiedStatus.mapKeys { RecipientId.from(it.key) }
    val members: List<RecipientId> = ZonaRosaDatabase.distributionLists.getMembers(distributionListId)
    val localReceipts: Map<RecipientId, Int> = ZonaRosaDatabase.groupReceipts.getGroupReceiptInfo(messageId).associate { it.recipientId to it.status }

    for (messageRecipientId in messageRecipientIds.keys) {
      if ((localReceipts[messageRecipientId] ?: GroupReceiptTable.STATUS_UNKNOWN) < GroupReceiptTable.STATUS_UNDELIVERED) {
        ZonaRosaDatabase.groupReceipts.update(messageRecipientId, messageId, GroupReceiptTable.STATUS_UNDELIVERED, sent.timestamp!!)
      } else if (!localReceipts.containsKey(messageRecipientId)) {
        ZonaRosaDatabase.groupReceipts.insert(listOf(messageRecipientId), messageId, GroupReceiptTable.STATUS_UNDELIVERED, sent.timestamp!!)
      }
    }

    val unidentifiedStatus = members.map { Pair(it, messageRecipientIds[it] ?: false) }

    ZonaRosaDatabase.groupReceipts.setUnidentified(unidentifiedStatus, messageId)
  }

  @Throws(MmsException::class)
  private fun handleSynchronizeSentEndSessionMessage(context: Context, sent: Sent, envelopeTimestamp: Long): Long {
    log(envelopeTimestamp, "Synchronize end session message.")

    val recipient: Recipient = getSyncMessageDestination(sent)
    val outgoingEndSessionMessage: OutgoingMessage = OutgoingMessage.endSessionMessage(recipient, sent.timestamp!!)
    val threadId: Long = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)

    if (!recipient.isGroup) {
      AppDependencies.protocolStore.aci().deleteAllSessions(recipient.requireServiceId().toString())
      SecurityEvent.broadcastSecurityUpdateEvent(context)
      val messageId = ZonaRosaDatabase.messages.insertMessageOutbox(
        outgoingEndSessionMessage,
        threadId,
        false,
        null
      ).messageId

      ZonaRosaDatabase.messages.markAsSent(messageId, true)
    }

    return threadId
  }

  @Throws(IOException::class, GroupChangeBusyException::class)
  private fun handleSynchronizeSentGv2Update(context: Context, envelope: Envelope, sent: Sent) {
    log(envelope.timestamp!!, "Synchronize sent GV2 update for message with timestamp " + sent.timestamp!!)

    val dataMessage: DataMessage = sent.message!!
    val groupId: GroupId.V2? = dataMessage.groupV2?.groupId

    if (groupId == null) {
      warn(envelope.timestamp!!, "GV2 update missing group id")
      return
    }

    if (MessageContentProcessor.updateGv2GroupFromServerOrP2PChange(context, envelope.timestamp!!, dataMessage.groupV2!!, ZonaRosaDatabase.groups.getGroup(groupId)) == null) {
      log(envelope.timestamp!!, "Ignoring GV2 message for group we are not currently in $groupId")
    }
  }

  @Throws(MmsException::class)
  private fun handleSynchronizeSentExpirationUpdate(sent: Sent, sideEffect: Boolean = false): Long {
    log(sent.timestamp!!, "Synchronize sent expiration update. sideEffect: $sideEffect")

    val groupId: GroupId? = getSyncMessageDestination(sent).groupId.orNull()

    if (groupId != null && groupId.isV2) {
      warn(sent.timestamp!!, "Expiration update received for GV2. Ignoring.")
      return -1
    }

    val recipient: Recipient = getSyncMessageDestination(sent)
    val threadId: Long = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    val expirationUpdateMessage: OutgoingMessage = OutgoingMessage.expirationUpdateMessage(
      threadRecipient = recipient,
      sentTimeMillis = if (sideEffect) sent.timestamp!! - 1 else sent.timestamp!!,
      expiresIn = sent.message!!.expireTimerDuration.inWholeMilliseconds,
      expireTimerVersion = sent.message!!.expireTimerVersion ?: 1
    )

    if (sent.message?.expireTimerVersion == null) {
      // TODO [expireVersion] After unsupported builds expire, we can remove this branch
      ZonaRosaDatabase.recipients.setExpireMessagesWithoutIncrementingVersion(recipient.id, sent.message!!.expireTimerDuration.inWholeSeconds.toInt())
      val messageId: Long = ZonaRosaDatabase.messages.insertMessageOutbox(expirationUpdateMessage, threadId, false, null).messageId
      ZonaRosaDatabase.messages.markAsSent(messageId, true)
    } else if (sent.message!!.expireTimerVersion!! >= recipient.expireTimerVersion) {
      ZonaRosaDatabase.recipients.setExpireMessages(recipient.id, sent.message!!.expireTimerDuration.inWholeSeconds.toInt(), sent.message!!.expireTimerVersion!!)

      if (sent.message!!.expireTimerDuration != recipient.expiresInSeconds.seconds) {
        log(sent.timestamp!!, "Not inserted update message as timer value did not change")
        val messageId: Long = ZonaRosaDatabase.messages.insertMessageOutbox(expirationUpdateMessage, threadId, false, null).messageId
        ZonaRosaDatabase.messages.markAsSent(messageId, true)
      }
    } else {
      warn(sent.timestamp!!, "[SynchronizeExpiration] Ignoring expire timer update with old version. Received: ${sent.message!!.expireTimerVersion}, Current: ${recipient.expireTimerVersion}")
    }

    return threadId
  }

  @Throws(MmsException::class, BadGroupIdException::class)
  private fun handleSynchronizeSentStoryReply(sent: Sent, envelopeTimestamp: Long): Long {
    log(envelopeTimestamp, "Synchronize sent story reply for " + sent.timestamp!!)

    try {
      val dataMessage: DataMessage = sent.message!!
      val storyContext: DataMessage.StoryContext = dataMessage.storyContext!!

      val reaction: DataMessage.Reaction? = dataMessage.reaction
      val parentStoryId: ParentStoryId
      val authorServiceId: ServiceId = ACI.parseOrThrow(storyContext.authorAci, storyContext.authorAciBinary)
      val sentTimestamp: Long = storyContext.sentTimestamp!!
      val recipient: Recipient = getSyncMessageDestination(sent)
      var quoteModel: QuoteModel? = null
      var expiresInMillis = 0L
      val storyAuthorRecipient: RecipientId = RecipientId.from(authorServiceId)
      val storyMessageId: Long = ZonaRosaDatabase.messages.getStoryId(storyAuthorRecipient, sentTimestamp).id
      val story: MmsMessageRecord = ZonaRosaDatabase.messages.getMessageRecord(storyMessageId) as MmsMessageRecord
      val threadRecipientId: RecipientId? = ZonaRosaDatabase.threads.getRecipientForThreadId(story.threadId)?.id
      val groupStory: Boolean = threadRecipientId != null && (ZonaRosaDatabase.groups.getGroup(threadRecipientId).orNull()?.isActive ?: false)
      var bodyRanges: BodyRangeList? = null

      val body: String? = if (EmojiUtil.isEmoji(reaction?.emoji)) {
        reaction!!.emoji
      } else if (dataMessage.body != null) {
        bodyRanges = dataMessage.bodyRanges.toBodyRangeList()
        dataMessage.body
      } else {
        null
      }

      if (dataMessage.hasGroupContext) {
        parentStoryId = GroupReply(storyMessageId)
      } else if (groupStory || story.storyType.isStoryWithReplies) {
        parentStoryId = DirectReply(storyMessageId)

        var quoteBody = ""
        var bodyBodyRanges: BodyRangeList? = null
        if (story.storyType.isTextStory) {
          quoteBody = story.body
          bodyBodyRanges = story.messageRanges
        }
        quoteModel = QuoteModel(sentTimestamp, storyAuthorRecipient, quoteBody, false, story.slideDeck.asAttachments().firstOrNull(), emptyList(), QuoteModel.Type.NORMAL, bodyBodyRanges)
        expiresInMillis = dataMessage.expireTimerDuration.inWholeMilliseconds
      } else {
        warn(envelopeTimestamp, "Story has replies disabled. Dropping reply.")
        return -1L
      }

      val mediaMessage = OutgoingMessage(
        recipient = recipient,
        body = body,
        timestamp = sent.timestamp!!,
        expiresIn = expiresInMillis,
        parentStoryId = parentStoryId,
        isStoryReaction = reaction != null,
        quote = quoteModel,
        mentions = DataMessageProcessor.getMentions(dataMessage.bodyRanges),
        bodyRanges = bodyRanges,
        isSecure = true
      )

      if (recipient.expiresInSeconds != dataMessage.expireTimerDuration.inWholeSeconds.toInt() || ((dataMessage.expireTimerVersion ?: -1) > recipient.expireTimerVersion)) {
        handleSynchronizeSentExpirationUpdate(sent, sideEffect = true)
      }

      val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
      val messageId: Long = ZonaRosaDatabase.messages.insertMessageOutbox(mediaMessage, threadId, false, GroupReceiptTable.STATUS_UNKNOWN, null).messageId

      if (recipient.isGroup) {
        updateGroupReceiptStatus(sent, messageId, recipient.requireGroupId())
      } else {
        ZonaRosaDatabase.messages.markUnidentified(messageId, sent.isUnidentified(recipient.serviceId.orNull()))
      }

      ZonaRosaDatabase.messages.markAsSent(messageId, true)
      if (dataMessage.expireTimerDuration > Duration.ZERO) {
        ZonaRosaDatabase.messages.markExpireStarted(messageId, sent.expirationStartTimestamp ?: 0)

        AppDependencies
          .expiringMessageManager
          .scheduleDeletion(messageId, true, sent.expirationStartTimestamp ?: 0, dataMessage.expireTimerDuration.inWholeMilliseconds)
      }
      if (recipient.isSelf) {
        ZonaRosaDatabase.messages.incrementDeliveryReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
        ZonaRosaDatabase.messages.incrementReadReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
      }

      return threadId
    } catch (e: NoSuchMessageException) {
      warn(envelopeTimestamp, "Couldn't find story for reply.", e)
      return -1L
    }
  }

  @Throws(MmsException::class, BadGroupIdException::class)
  private fun handleSynchronizeSentMediaMessage(context: Context, sent: Sent, envelopeTimestamp: Long, senderRecipient: Recipient, threadRecipient: Recipient): Long {
    log(envelopeTimestamp, "Synchronize sent media message for " + sent.timestamp!!)

    val recipient: Recipient = getSyncMessageDestination(sent)
    val dataMessage: DataMessage = sent.message!!
    val quoteModel: QuoteModel? = DataMessageProcessor.getValidatedQuote(context, envelopeTimestamp, dataMessage, senderRecipient, threadRecipient)
    val sticker: Attachment? = DataMessageProcessor.getStickerAttachment(envelopeTimestamp, dataMessage)
    val sharedContacts: List<Contact> = DataMessageProcessor.getContacts(dataMessage)
    val previews: List<LinkPreview> = DataMessageProcessor.getLinkPreviews(dataMessage.preview, dataMessage.body ?: "", false)
    val mentions: List<Mention> = DataMessageProcessor.getMentions(dataMessage.bodyRanges)
    val giftBadge: GiftBadge? = if (dataMessage.giftBadge?.receiptCredentialPresentation != null) GiftBadge.Builder().redemptionToken(dataMessage.giftBadge!!.receiptCredentialPresentation!!).build() else null
    val viewOnce: Boolean = dataMessage.isViewOnce == true
    val bodyRanges: BodyRangeList? = dataMessage.bodyRanges.toBodyRangeList()
    val syncAttachments: List<Attachment> = listOfNotNull(sticker) + if (viewOnce) listOf<Attachment>(TombstoneAttachment.forNonQuote(MediaUtil.VIEW_ONCE)) else dataMessage.attachments.toPointersWithinLimit()

    val mediaMessage = OutgoingMessage(
      recipient = recipient,
      body = dataMessage.body ?: "",
      attachments = syncAttachments,
      timestamp = sent.timestamp!!,
      expiresIn = dataMessage.expireTimerDuration.inWholeMilliseconds,
      viewOnce = viewOnce,
      quote = quoteModel,
      contacts = sharedContacts,
      previews = previews,
      mentions = mentions,
      giftBadge = giftBadge,
      bodyRanges = bodyRanges,
      isSecure = true
    )

    if (recipient.expiresInSeconds != dataMessage.expireTimerDuration.inWholeSeconds.toInt() || ((dataMessage.expireTimerVersion ?: -1) > recipient.expireTimerVersion)) {
      handleSynchronizeSentExpirationUpdate(sent, sideEffect = true)
    }

    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    val messageId: Long = ZonaRosaDatabase.messages.insertMessageOutbox(mediaMessage, threadId, false, GroupReceiptTable.STATUS_UNKNOWN, null).messageId
    log(envelopeTimestamp, "Inserted sync message as messageId $messageId")

    if (recipient.isGroup) {
      updateGroupReceiptStatus(sent, messageId, recipient.requireGroupId())
    } else {
      ZonaRosaDatabase.messages.markUnidentified(messageId, sent.isUnidentified(recipient.serviceId.orNull()))
    }

    ZonaRosaDatabase.messages.markAsSent(messageId, true)

    val attachments: List<DatabaseAttachment> = ZonaRosaDatabase.attachments.getAttachmentsForMessage(messageId)

    if (dataMessage.expireTimerDuration > Duration.ZERO) {
      ZonaRosaDatabase.messages.markExpireStarted(messageId, sent.expirationStartTimestamp ?: 0)

      AppDependencies.expiringMessageManager.scheduleDeletion(messageId, true, sent.expirationStartTimestamp ?: 0, dataMessage.expireTimerDuration.inWholeMilliseconds)
    }
    if (recipient.isSelf) {
      ZonaRosaDatabase.messages.incrementDeliveryReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
      ZonaRosaDatabase.messages.incrementReadReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
    }

    ZonaRosaDatabase.runPostSuccessfulTransaction {
      val downloadJobs: List<AttachmentDownloadJob> = attachments.map { AttachmentDownloadJob(messageId = messageId, attachmentId = it.attachmentId, forceDownload = it.isSticker) }
      AppDependencies.jobManager.addAll(downloadJobs)
    }

    return threadId
  }

  @Throws(MmsException::class, BadGroupIdException::class)
  private fun handleSynchronizeSentTextMessage(sent: Sent, envelopeTimestamp: Long): Long {
    log(envelopeTimestamp, "Synchronize sent text message for " + sent.timestamp!!)

    val recipient = getSyncMessageDestination(sent)
    val dataMessage: DataMessage = sent.message!!
    val body = dataMessage.body ?: ""
    val expiresInMillis = dataMessage.expireTimerDuration.inWholeMilliseconds
    val bodyRanges = dataMessage.bodyRanges.filter { Utils.allAreNull(it.mentionAci, it.mentionAciBinary) }.toBodyRangeList()

    if (recipient.expiresInSeconds != dataMessage.expireTimerDuration.inWholeSeconds.toInt() || ((dataMessage.expireTimerVersion ?: -1) > recipient.expireTimerVersion)) {
      handleSynchronizeSentExpirationUpdate(sent, sideEffect = true)
    }

    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)
    val isGroup = recipient.isGroup
    val messageId: Long

    if (isGroup) {
      val outgoingMessage = OutgoingMessage(
        recipient = recipient,
        body = body,
        timestamp = sent.timestamp!!,
        expiresIn = expiresInMillis,
        isSecure = true,
        bodyRanges = bodyRanges
      )

      messageId = ZonaRosaDatabase.messages.insertMessageOutbox(outgoingMessage, threadId, false, GroupReceiptTable.STATUS_UNKNOWN, null).messageId
      updateGroupReceiptStatus(sent, messageId, recipient.requireGroupId())
    } else {
      val outgoingTextMessage = OutgoingMessage.text(threadRecipient = recipient, body = body, expiresIn = expiresInMillis, sentTimeMillis = sent.timestamp!!, bodyRanges = bodyRanges)
      messageId = ZonaRosaDatabase.messages.insertMessageOutbox(outgoingTextMessage, threadId, false, null).messageId
      ZonaRosaDatabase.messages.markUnidentified(messageId, sent.isUnidentified(recipient.serviceId.orNull()))
    }

    log(envelopeTimestamp, "Inserted sync message as messageId $messageId")

    ZonaRosaDatabase.messages.markAsSent(messageId, true)

    if (expiresInMillis > 0) {
      ZonaRosaDatabase.messages.markExpireStarted(messageId, sent.expirationStartTimestamp ?: 0)
      AppDependencies.expiringMessageManager.scheduleDeletion(messageId, isGroup, sent.expirationStartTimestamp ?: 0, expiresInMillis)
    }

    if (recipient.isSelf) {
      ZonaRosaDatabase.messages.incrementDeliveryReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
      ZonaRosaDatabase.messages.incrementReadReceiptCount(sent.timestamp!!, recipient.id, System.currentTimeMillis())
    }

    return threadId
  }

  private fun handleSynchronizeRequestMessage(context: Context, message: Request, envelopeTimestamp: Long) {
    if (ZonaRosaStore.account.isPrimaryDevice) {
      log(envelopeTimestamp, "Synchronize request message.")
    } else {
      log(envelopeTimestamp, "Linked device ignoring synchronize request message.")
      return
    }

    when (message.type) {
      Request.Type.CONTACTS -> AppDependencies.jobManager.add(MultiDeviceContactUpdateJob(true))
      Request.Type.BLOCKED -> AppDependencies.jobManager.add(MultiDeviceBlockedUpdateJob())
      Request.Type.CONFIGURATION -> {
        AppDependencies.jobManager.add(
          MultiDeviceConfigurationUpdateJob(
            ZonaRosaPreferences.isReadReceiptsEnabled(context),
            ZonaRosaPreferences.isTypingIndicatorsEnabled(context),
            ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
            ZonaRosaStore.settings.isLinkPreviewsEnabled
          )
        )
        AppDependencies.jobManager.add(MultiDeviceStickerPackSyncJob())
      }
      Request.Type.KEYS -> AppDependencies.jobManager.add(MultiDeviceKeysUpdateJob())
      else -> warn(envelopeTimestamp, "Unknown request type: ${message.type}")
    }
  }

  private fun handleSynchronizeReadMessage(
    context: Context,
    readMessages: List<Read>,
    envelopeTimestamp: Long,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ) {
    log(envelopeTimestamp, "Synchronize read message. Count: ${readMessages.size}, Timestamps: ${readMessages.map { it.timestamp }}")

    val threadToLatestRead: MutableMap<Long, Long> = HashMap()
    val unhandled: Collection<MessageTable.SyncMessageId> = ZonaRosaDatabase.messages.setTimestampReadFromSyncMessage(readMessages, envelopeTimestamp, threadToLatestRead)
    val markedMessages: List<MarkedMessageInfo> = ZonaRosaDatabase.threads.setReadSince(threadToLatestRead)

    if (Util.hasItems(markedMessages)) {
      log("Updating past ZonaRosaDatabase.messages: " + markedMessages.size)
      MarkReadReceiver.process(markedMessages)
    }

    for (id in unhandled) {
      warn(envelopeTimestamp, "[handleSynchronizeReadMessage] Could not find matching message! timestamp: ${id.timetamp}  author: ${id.recipientId}")
      if (earlyMessageCacheEntry != null) {
        AppDependencies.earlyMessageCache.store(id.recipientId, id.timetamp, earlyMessageCacheEntry)
      }
    }

    if (unhandled.isNotEmpty() && earlyMessageCacheEntry != null) {
      PushProcessEarlyMessagesJob.enqueue()
    }

    AppDependencies
      .messageNotifier
      .apply {
        setLastDesktopActivityTimestamp(envelopeTimestamp)
        cancelDelayedNotifications()
        updateNotification(context)
      }
  }

  private fun handleSynchronizeViewedMessage(context: Context, viewedMessages: List<SyncMessage.Viewed>, envelopeTimestamp: Long) {
    log(envelopeTimestamp, "Synchronize view message. Count: ${viewedMessages.size}, Timestamps: ${viewedMessages.map { it.timestamp }}")

    val records = viewedMessages
      .mapNotNull { message ->
        val author = Recipient.externalPush(ACI.parseOrThrow(message.senderAci, message.senderAciBinary)).id
        if (message.timestamp != null) {
          ZonaRosaDatabase.messages.getMessageFor(message.timestamp!!, author)
        } else {
          warn(envelopeTimestamp, "Message timestamp null")
          null
        }
      }

    val toMarkViewed = records.map { it.id }

    val toEnqueueDownload = records
      .map { it as MmsMessageRecord }
      .filter { it.storyType.isStory && !it.storyType.isTextStory }

    for (mediaMmsMessageRecord in toEnqueueDownload) {
      Stories.enqueueAttachmentsFromStoryForDownloadSync(mediaMmsMessageRecord, false)
    }

    ZonaRosaDatabase.messages.setIncomingMessagesViewed(toMarkViewed)
    ZonaRosaDatabase.messages.setOutgoingGiftsRevealed(toMarkViewed)

    AppDependencies.messageNotifier.apply {
      setLastDesktopActivityTimestamp(envelopeTimestamp)
      cancelDelayedNotifications()
      updateNotification(context)
    }
  }

  private fun handleSynchronizeViewOnceOpenMessage(context: Context, openMessage: ViewOnceOpen, envelopeTimestamp: Long, earlyMessageCacheEntry: EarlyMessageCacheEntry?) {
    log(envelopeTimestamp, "Handling a view-once open for message: " + openMessage.timestamp)

    val author: RecipientId = Recipient.externalPush(ACI.parseOrThrow(openMessage.senderAci, openMessage.senderAciBinary)).id
    val timestamp: Long = if (openMessage.timestamp != null) {
      openMessage.timestamp!!
    } else {
      warn(envelopeTimestamp, "Open message missing timestamp")
      return
    }
    val record: MessageRecord? = ZonaRosaDatabase.messages.getMessageFor(timestamp, author)

    if (record != null) {
      ZonaRosaDatabase.attachments.deleteAttachmentFilesForViewOnceMessage(record.id)
    } else {
      warn(envelopeTimestamp.toString(), "Got a view-once open message for a message we don't have!")
      if (earlyMessageCacheEntry != null) {
        AppDependencies.earlyMessageCache.store(author, timestamp, earlyMessageCacheEntry)
        PushProcessEarlyMessagesJob.enqueue()
      }
    }

    AppDependencies.messageNotifier.apply {
      setLastDesktopActivityTimestamp(envelopeTimestamp)
      cancelDelayedNotifications()
      updateNotification(context)
    }
  }

  private fun handleSynchronizeVerifiedMessage(context: Context, verifiedMessage: Verified) {
    log("Synchronize verified message.")

    IdentityUtil.processVerifiedMessage(context, verifiedMessage)
  }

  private fun handleSynchronizeStickerPackOperation(stickerPackOperations: List<StickerPackOperation>, envelopeTimestamp: Long) {
    log(envelopeTimestamp, "Synchronize sticker pack operation.")

    val jobManager = AppDependencies.jobManager

    for (operation in stickerPackOperations) {
      if (operation.packId != null && operation.packKey != null && operation.type != null) {
        val packId = Hex.toStringCondensed(operation.packId!!.toByteArray())
        val packKey = Hex.toStringCondensed(operation.packKey!!.toByteArray())

        when (operation.type!!) {
          StickerPackOperation.Type.INSTALL -> jobManager.add(StickerPackDownloadJob.forInstall(packId, packKey, false))
          StickerPackOperation.Type.REMOVE -> ZonaRosaDatabase.stickers.uninstallPacks(setOf(StickerPackId(packId)))
        }
      } else {
        warn("Received incomplete sticker pack operation sync.")
      }
    }
  }

  private fun handleSynchronizeConfigurationMessage(context: Context, configurationMessage: Configuration, envelopeTimestamp: Long) {
    log(envelopeTimestamp, "Synchronize configuration message.")

    if (configurationMessage.readReceipts != null) {
      ZonaRosaPreferences.setReadReceiptsEnabled(context, configurationMessage.readReceipts!!)
    }

    if (configurationMessage.unidentifiedDeliveryIndicators != null) {
      ZonaRosaPreferences.setShowUnidentifiedDeliveryIndicatorsEnabled(context, configurationMessage.unidentifiedDeliveryIndicators!!)
    }

    if (configurationMessage.typingIndicators != null) {
      ZonaRosaPreferences.setTypingIndicatorsEnabled(context, configurationMessage.typingIndicators!!)
    }

    if (configurationMessage.linkPreviews != null) {
      ZonaRosaStore.settings.isLinkPreviewsEnabled = configurationMessage.linkPreviews!!
    }
  }

  private fun handleSynchronizeBlockedListMessage(blockMessage: Blocked, envelopeTimestamp: Long) {
    val blockedAcis = if (blockMessage.acisBinary.isNotEmpty()) { blockMessage.acisBinary.mapNotNull { ACI.parseOrNull(it) } } else blockMessage.acis.mapNotNull { ACI.parseOrNull(it) }
    val blockedE164s = blockMessage.numbers
    val blockedGroupIds = blockMessage.groupIds.map { it.toByteArray() }
    log(envelopeTimestamp, "Synchronize block message. Counts: (ACI: ${blockedAcis.size}, E164: ${blockedE164s.size}, Group: ${blockedGroupIds.size})")

    ZonaRosaDatabase.recipients.applyBlockedUpdate(blockedE164s, blockedAcis, blockedGroupIds)
  }

  private fun handleSynchronizeFetchMessage(fetchType: FetchLatest.Type, envelopeTimestamp: Long) {
    log(envelopeTimestamp, "Received fetch request with type: $fetchType")
    when (fetchType) {
      FetchLatest.Type.LOCAL_PROFILE -> AppDependencies.jobManager.add(RefreshOwnProfileJob())
      FetchLatest.Type.STORAGE_MANIFEST -> AppDependencies.jobManager.add(StorageSyncJob.forRemoteChange())
      FetchLatest.Type.SUBSCRIPTION_STATUS -> warn(envelopeTimestamp, "Dropping subscription status fetch message.")
      else -> warn(envelopeTimestamp, "Received a fetch message for an unknown type.")
    }
  }

  @Throws(BadGroupIdException::class)
  private fun handleSynchronizeMessageRequestResponse(response: MessageRequestResponse, envelopeTimestamp: Long) {
    log(envelopeTimestamp, "Synchronize message request response.")

    val recipient: Recipient = if (Utils.anyNotNull(response.threadAci, response.threadAciBinary)) {
      Recipient.externalPush(ACI.parseOrThrow(response.threadAci, response.threadAciBinary))
    } else if (response.groupId != null) {
      val groupId: GroupId = GroupId.push(response.groupId!!)
      Recipient.externalPossiblyMigratedGroup(groupId)
    } else {
      warn("Message request response was missing a thread recipient! Skipping.")
      return
    }

    val threadId: Long = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)

    when (response.type) {
      MessageRequestResponse.Type.ACCEPT -> {
        val wasBlocked = recipient.isBlocked
        ZonaRosaDatabase.recipients.setProfileSharing(recipient.id, true)
        ZonaRosaDatabase.recipients.setBlocked(recipient.id, false)
        if (wasBlocked) {
          ZonaRosaDatabase.messages.insertMessageOutbox(
            message = OutgoingMessage.unblockedMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong())),
            threadId = threadId
          )
        } else {
          ZonaRosaDatabase.messages.insertMessageOutbox(
            message = OutgoingMessage.messageRequestAcceptMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong())),
            threadId = threadId
          )
        }
      }
      MessageRequestResponse.Type.DELETE -> {
        ZonaRosaDatabase.recipients.setProfileSharing(recipient.id, false)
        if (threadId > 0) {
          ZonaRosaDatabase.threads.deleteConversation(threadId, syncThreadDelete = false)
        }
      }
      MessageRequestResponse.Type.BLOCK -> {
        ZonaRosaDatabase.recipients.setBlocked(recipient.id, true)
        RecipientUtil.updateProfileSharingAfterBlock(recipient, true)
        ZonaRosaDatabase.messages.insertMessageOutbox(
          message = OutgoingMessage.blockedMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong())),
          threadId = threadId
        )
      }
      MessageRequestResponse.Type.BLOCK_AND_DELETE -> {
        ZonaRosaDatabase.recipients.setBlocked(recipient.id, true)
        RecipientUtil.updateProfileSharingAfterBlock(recipient, true)
        if (threadId > 0) {
          ZonaRosaDatabase.threads.deleteConversation(threadId, syncThreadDelete = false)
        }
      }
      MessageRequestResponse.Type.SPAM -> {
        ZonaRosaDatabase.messages.insertMessageOutbox(
          message = OutgoingMessage.reportSpamMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong())),
          threadId = threadId
        )
      }
      MessageRequestResponse.Type.BLOCK_AND_SPAM -> {
        ZonaRosaDatabase.recipients.setBlocked(recipient.id, true)
        RecipientUtil.updateProfileSharingAfterBlock(recipient, true)
        ZonaRosaDatabase.messages.insertMessageOutbox(
          message = OutgoingMessage.reportSpamMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong())),
          threadId = threadId
        )
        ZonaRosaDatabase.messages.insertMessageOutbox(
          message = OutgoingMessage.blockedMessage(recipient, System.currentTimeMillis(), TimeUnit.SECONDS.toMillis(recipient.expiresInSeconds.toLong())),
          threadId = threadId
        )
      }
      else -> warn("Got an unknown response type! Skipping")
    }
  }

  private fun handleSynchronizeOutgoingPayment(outgoingPayment: SyncMessage.OutgoingPayment, envelopeTimestamp: Long) {
    log(envelopeTimestamp, "Synchronize outgoing payment.")

    val mobileCoin = if (outgoingPayment.mobileCoin != null) {
      outgoingPayment.mobileCoin!!
    } else {
      log(envelopeTimestamp, "Unknown outgoing payment, ignoring.")
      return
    }

    var recipientId: RecipientId? = ServiceId.parseOrNull(outgoingPayment.recipientServiceId)?.let { RecipientId.from(it) }

    var timestamp: Long = mobileCoin.ledgerBlockTimestamp ?: 0L
    if (timestamp == 0L) {
      timestamp = System.currentTimeMillis()
    }

    var address: MobileCoinPublicAddress? = if (mobileCoin.recipientAddress != null) {
      MobileCoinPublicAddress.fromBytes(mobileCoin.recipientAddress!!.toByteArray())
    } else {
      null
    }

    if (address == null && recipientId == null) {
      log(envelopeTimestamp, "Inserting defrag")
      address = AppDependencies.payments.wallet.mobileCoinPublicAddress
      recipientId = Recipient.self().id
    }

    val uuid = UUID.randomUUID()
    try {
      ZonaRosaDatabase.payments
        .createSuccessfulPayment(
          uuid,
          recipientId,
          address!!,
          timestamp,
          mobileCoin.ledgerBlockIndex!!,
          outgoingPayment.note ?: "",
          mobileCoin.amountPicoMob!!.toMobileCoinMoney(),
          mobileCoin.feePicoMob!!.toMobileCoinMoney(),
          mobileCoin.receipt!!.toByteArray(),
          PaymentMetaDataUtil.fromKeysAndImages(mobileCoin.outputPublicKeys, mobileCoin.spentKeyImages)
        )
    } catch (e: SerializationException) {
      warn(envelopeTimestamp, "Ignoring synchronized outgoing payment with bad data.", e)
    }

    log("Inserted synchronized payment $uuid")
  }

  private fun handleSynchronizeKeys(keys: SyncMessage.Keys, envelopeTimestamp: Long) {
    if (ZonaRosaStore.account.isLinkedDevice) {
      log(envelopeTimestamp, "Synchronize keys.")
    } else {
      log(envelopeTimestamp, "Primary device ignores synchronize keys.")
      return
    }

    if (keys.accountEntropyPool != null) {
      ZonaRosaStore.account.setAccountEntropyPoolFromPrimaryDevice(AccountEntropyPool(keys.accountEntropyPool!!))
    }

    if (keys.mediaRootBackupKey != null) {
      ZonaRosaStore.backup.mediaRootBackupKey = MediaRootBackupKey(keys.mediaRootBackupKey!!.toByteArray())
    }
  }

  @Throws(IOException::class)
  private fun handleSynchronizeContacts(contactsMessage: SyncMessage.Contacts, envelopeTimestamp: Long) {
    if (ZonaRosaStore.account.isLinkedDevice) {
      log(envelopeTimestamp, "Synchronize contacts.")
    } else {
      log(envelopeTimestamp, "Primary device ignores synchronize contacts.")
      return
    }

    if (contactsMessage.blob == null) {
      log(envelopeTimestamp, "Contact blob is null")
      return
    }

    val attachment: ZonaRosaServiceAttachmentPointer = contactsMessage.blob!!.toZonaRosaServiceAttachmentPointer()

    AppDependencies.jobManager.add(MultiDeviceContactSyncJob(attachment))
  }

  private fun handleSynchronizeCallEvent(callEvent: SyncMessage.CallEvent, envelopeTimestamp: Long) {
    if (callEvent.callId == null) {
      log(envelopeTimestamp, "Synchronize call event missing call id, ignoring. type: ${callEvent.type}")
      return
    }

    if (callEvent.type == SyncMessage.CallEvent.Type.GROUP_CALL || callEvent.type == SyncMessage.CallEvent.Type.AD_HOC_CALL) {
      handleSynchronizeGroupOrAdHocCallEvent(callEvent, envelopeTimestamp)
    } else {
      handleSynchronizeOneToOneCallEvent(callEvent, envelopeTimestamp)
    }
  }

  private fun handleSynchronizeCallLogEvent(callLogEvent: CallLogEvent, envelopeTimestamp: Long) {
    val timestamp = callLogEvent.timestamp
    val callId = callLogEvent.callId?.let { CallId(it) }
    val peer: RecipientId? = callLogEvent.conversationId?.let { byteString ->
      ACI.parseOrNull(byteString)?.let { RecipientId.from(it) }
        ?: GroupId.pushOrNull(byteString.toByteArray())?.let { ZonaRosaDatabase.recipients.getByGroupId(it).orNull() }
        ?: CallLinkRoomId.fromBytes(byteString.toByteArray()).let { ZonaRosaDatabase.recipients.getByCallLinkRoomId(it).orNull() }
    }

    if (callId != null && peer != null) {
      val call = ZonaRosaDatabase.calls.getCallById(callId.longValue(), peer)

      if (call != null) {
        log(envelopeTimestamp, "Synchronizing call log event with exact call data.")
        synchronizeCallLogEventViaTimestamp(envelopeTimestamp, callLogEvent.type, call.timestamp, peer)
        return
      }
    }

    if (timestamp != null) {
      warn(envelopeTimestamp, "Synchronize call log event using timestamp instead of exact values")
      synchronizeCallLogEventViaTimestamp(envelopeTimestamp, callLogEvent.type, timestamp, peer)
    } else {
      log(envelopeTimestamp, "Failed to synchronize call log event, not enough information.")
    }
  }

  private fun synchronizeCallLogEventViaTimestamp(envelopeTimestamp: Long, eventType: CallLogEvent.Type?, timestamp: Long, peer: RecipientId?) {
    when (eventType) {
      CallLogEvent.Type.CLEAR -> {
        ZonaRosaDatabase.calls.deleteNonAdHocCallEventsOnOrBefore(timestamp)
        ZonaRosaDatabase.callLinks.deleteNonAdminCallLinksOnOrBefore(timestamp)
      }

      CallLogEvent.Type.MARKED_AS_READ -> {
        ZonaRosaDatabase.calls.markAllCallEventsRead(timestamp)
      }

      CallLogEvent.Type.MARKED_AS_READ_IN_CONVERSATION -> {
        if (peer == null) {
          warn(envelopeTimestamp, "Cannot synchronize conversation calls, missing peer.")
          return
        }

        ZonaRosaDatabase.calls.markAllCallEventsWithPeerBeforeTimestampRead(peer, timestamp)
      }

      else -> log(envelopeTimestamp, "Synchronize call log event has an invalid type $eventType, ignoring.")
    }
  }

  private fun handleSynchronizeCallLink(callLinkUpdate: CallLinkUpdate, envelopeTimestamp: Long) {
    if (callLinkUpdate.rootKey == null) {
      log(envelopeTimestamp, "Synchronize call link missing root key, ignoring.")
      return
    }

    val callLinkRootKey = try {
      CallLinkRootKey(callLinkUpdate.rootKey!!.toByteArray())
    } catch (e: CallException) {
      log(envelopeTimestamp, "Synchronize call link has invalid root key, ignoring.")
      return
    }

    val roomId = CallLinkRoomId.fromCallLinkRootKey(callLinkRootKey)

    if (ZonaRosaDatabase.callLinks.callLinkExists(roomId)) {
      log(envelopeTimestamp, "Synchronize call link for a link we already know about. Updating credentials.")
      ZonaRosaDatabase.callLinks.updateCallLinkCredentials(
        roomId,
        CallLinkCredentials(
          callLinkUpdate.rootKey!!.toByteArray(),
          callLinkUpdate.adminPasskey?.toByteArray()
        )
      )
    } else {
      log(envelopeTimestamp, "Synchronize call link for a link we do not know about. Inserting.")
      ZonaRosaDatabase.callLinks.insertCallLink(
        CallLinkTable.CallLink(
          recipientId = RecipientId.UNKNOWN,
          roomId = roomId,
          credentials = CallLinkCredentials(
            linkKeyBytes = callLinkRootKey.keyBytes,
            adminPassBytes = callLinkUpdate.adminPasskey?.toByteArray()
          ),
          state = ZonaRosaCallLinkState(),
          deletionTimestamp = 0L
        )
      )

      AppDependencies.jobManager.add(StorageSyncJob.forRemoteChange())
    }

    AppDependencies.jobManager.add(RefreshCallLinkDetailsJob(callLinkUpdate))
  }

  private fun handleSynchronizeOneToOneCallEvent(callEvent: SyncMessage.CallEvent, envelopeTimestamp: Long) {
    val callId: Long = callEvent.callId!!
    val timestamp: Long = callEvent.timestamp ?: 0L
    val type: CallTable.Type? = CallTable.Type.from(callEvent.type)
    val direction: CallTable.Direction? = CallTable.Direction.from(callEvent.direction)
    val event: CallTable.Event? = CallTable.Event.from(callEvent.event)

    if (timestamp == 0L || type == null || direction == null || event == null || callEvent.conversationId == null) {
      warn(envelopeTimestamp, "Call event sync message is not valid, ignoring. timestamp: " + timestamp + " type: " + type + " direction: " + direction + " event: " + event + " hasPeer: " + (callEvent.conversationId != null))
      return
    }

    val aci = ACI.parseOrThrow(callEvent.conversationId!!)
    val recipientId = RecipientId.from(aci)

    log(envelopeTimestamp, "Synchronize call event call: $callId")

    val call = ZonaRosaDatabase.calls.getCallById(callId, recipientId)
    if (call != null) {
      val typeMismatch = call.type != type
      val directionMismatch = call.direction != direction
      val eventDowngrade = call.event == CallTable.Event.ACCEPTED && event != CallTable.Event.ACCEPTED && event != CallTable.Event.DELETE
      val peerMismatch = call.peer != recipientId

      if (typeMismatch || directionMismatch || peerMismatch || eventDowngrade) {
        warn(envelopeTimestamp, "Call event sync message is not valid for existing call record, ignoring. type: $type direction: $direction  event: $event peerMismatch: $peerMismatch")
      } else if (event == CallTable.Event.DELETE) {
        ZonaRosaDatabase.calls.markCallDeletedFromSyncEvent(call)
      } else {
        ZonaRosaDatabase.calls.updateOneToOneCall(callId, event)
      }
    } else if (event == CallTable.Event.DELETE) {
      ZonaRosaDatabase.calls.insertDeletedCallFromSyncEvent(callId, recipientId, type, direction, timestamp)
    } else {
      ZonaRosaDatabase.calls.insertOneToOneCall(callId, timestamp, recipientId, type, direction, event)
    }
  }

  @Throws(BadGroupIdException::class)
  private fun handleSynchronizeGroupOrAdHocCallEvent(callEvent: SyncMessage.CallEvent, envelopeTimestamp: Long) {
    val callId: Long = callEvent.callId!!
    val timestamp: Long = callEvent.timestamp ?: 0L
    val type: CallTable.Type? = CallTable.Type.from(callEvent.type)
    val direction: CallTable.Direction? = CallTable.Direction.from(callEvent.direction)
    val event: CallTable.Event? = CallTable.Event.from(callEvent.event)
    val hasConversationId: Boolean = callEvent.conversationId != null

    if (hasConversationId && type == CallTable.Type.AD_HOC_CALL && callEvent.event == SyncMessage.CallEvent.Event.OBSERVED && direction != null) {
      log(envelopeTimestamp, "Handling OBSERVED ad-hoc calling event")
      if (direction == CallTable.Direction.OUTGOING) {
        warn("Received an OBSERVED sync message for an outgoing event. Dropping.")
        return
      }

      val recipient = resolveCallLinkRecipient(callEvent)
      ZonaRosaDatabase.calls.insertOrUpdateAdHocCallFromRemoteObserveEvent(
        callRecipient = recipient,
        timestamp = callEvent.timestamp!!,
        callId = callId
      )

      return
    }

    if (timestamp == 0L || type == null || direction == null || event == null || !hasConversationId) {
      warn(envelopeTimestamp, "Group/Ad-hoc call event sync message is not valid, ignoring. timestamp: $timestamp type: $type direction: $direction event: $event hasPeer: $hasConversationId")
      return
    }

    val recipient: Recipient? = when (type) {
      CallTable.Type.AD_HOC_CALL -> {
        resolveCallLinkRecipient(callEvent)
      }
      CallTable.Type.GROUP_CALL -> {
        val groupId: GroupId = GroupId.push(callEvent.conversationId!!.toByteArray())
        Recipient.externalGroupExact(groupId)
      }
      else -> {
        warn(envelopeTimestamp, "Unexpected type $type. Ignoring.")
        null
      }
    }

    if (recipient == null) {
      warn(envelopeTimestamp, "Could not process conversation id.")
      return
    }

    val call = ZonaRosaDatabase.calls.getCallById(callId, recipient.id)

    if (call != null) {
      if (call.type !== type) {
        warn(envelopeTimestamp, "Group/Ad-hoc call event type mismatch, ignoring. timestamp: $timestamp type: $type direction: $direction event: $event hasPeer: $hasConversationId")
        return
      }
      when (event) {
        CallTable.Event.DELETE -> ZonaRosaDatabase.calls.markCallDeletedFromSyncEvent(call)
        CallTable.Event.ACCEPTED -> {
          if (call.timestamp > timestamp) {
            ZonaRosaDatabase.calls.setTimestamp(call.callId, recipient.id, timestamp)
          }
          if (direction == CallTable.Direction.INCOMING) {
            ZonaRosaDatabase.calls.acceptIncomingGroupCall(call)
          } else {
            ZonaRosaDatabase.calls.acceptOutgoingGroupCall(call)
          }
        }
        CallTable.Event.NOT_ACCEPTED -> {
          if (call.timestamp > timestamp) {
            ZonaRosaDatabase.calls.setTimestamp(call.callId, recipient.id, timestamp)
          }
          if (callEvent.direction == SyncMessage.CallEvent.Direction.INCOMING) {
            ZonaRosaDatabase.calls.declineIncomingGroupCall(call)
          } else {
            warn(envelopeTimestamp, "Invalid direction OUTGOING for event NOT_ACCEPTED")
          }
        }
        else -> warn("Unsupported event type $event. Ignoring. timestamp: $timestamp type: $type direction: $direction event: $event hasPeer: $hasConversationId")
      }
    } else {
      when (event) {
        CallTable.Event.DELETE -> ZonaRosaDatabase.calls.insertDeletedCallFromSyncEvent(callEvent.callId!!, recipient.id, type, direction, timestamp)
        CallTable.Event.ACCEPTED -> ZonaRosaDatabase.calls.insertAcceptedGroupCall(callEvent.callId!!, recipient.id, direction, timestamp)
        CallTable.Event.NOT_ACCEPTED -> {
          if (callEvent.direction == SyncMessage.CallEvent.Direction.INCOMING) {
            ZonaRosaDatabase.calls.insertDeclinedGroupCall(callEvent.callId!!, recipient.id, timestamp)
          } else {
            warn(envelopeTimestamp, "Invalid direction OUTGOING for event NOT_ACCEPTED for non-existing call")
          }
        }
        else -> warn("Unsupported event type $event. Ignoring. timestamp: $timestamp type: $type direction: $direction event: $event hasPeer: $hasConversationId call: null")
      }
    }
  }

  private fun resolveCallLinkRecipient(callEvent: SyncMessage.CallEvent): Recipient {
    val callLinkRoomId = CallLinkRoomId.fromBytes(callEvent.conversationId!!.toByteArray())
    val callLink = ZonaRosaDatabase.callLinks.getOrCreateCallLinkByRoomId(callLinkRoomId)
    return Recipient.resolved(callLink.recipientId)
  }

  private fun handleSynchronizeDeleteForMe(context: Context, deleteForMe: SyncMessage.DeleteForMe, envelopeTimestamp: Long, earlyMessageCacheEntry: EarlyMessageCacheEntry?) {
    log(envelopeTimestamp, "Synchronize delete message messageDeletes=${deleteForMe.messageDeletes.size} conversationDeletes=${deleteForMe.conversationDeletes.size} localOnlyConversationDeletes=${deleteForMe.localOnlyConversationDeletes.size}")

    if (deleteForMe.messageDeletes.isNotEmpty()) {
      handleSynchronizeMessageDeletes(deleteForMe.messageDeletes, envelopeTimestamp, earlyMessageCacheEntry)
    }

    if (deleteForMe.conversationDeletes.isNotEmpty()) {
      handleSynchronizeConversationDeletes(deleteForMe.conversationDeletes, envelopeTimestamp)
    }

    if (deleteForMe.localOnlyConversationDeletes.isNotEmpty()) {
      handleSynchronizeLocalOnlyConversationDeletes(deleteForMe.localOnlyConversationDeletes, envelopeTimestamp)
    }

    if (deleteForMe.attachmentDeletes.isNotEmpty()) {
      handleSynchronizeAttachmentDeletes(deleteForMe.attachmentDeletes, envelopeTimestamp, earlyMessageCacheEntry)
    }

    AppDependencies.messageNotifier.updateNotification(context)
  }

  private fun handleSynchronizeMessageDeletes(messageDeletes: List<SyncMessage.DeleteForMe.MessageDeletes>, envelopeTimestamp: Long, earlyMessageCacheEntry: EarlyMessageCacheEntry?) {
    val messagesToDelete: List<MessageTable.SyncMessageId> = messageDeletes
      .asSequence()
      .map { it.messages }
      .flatten()
      .mapNotNull { it.toSyncMessageId(envelopeTimestamp) }
      .toList()

    val unhandled: List<MessageTable.SyncMessageId> = ZonaRosaDatabase.messages.deleteMessages(messagesToDelete)

    for (syncMessage in unhandled) {
      warn(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Could not find matching message! timestamp: ${syncMessage.timetamp}  author: ${syncMessage.recipientId}")
      if (earlyMessageCacheEntry != null) {
        AppDependencies.earlyMessageCache.store(syncMessage.recipientId, syncMessage.timetamp, earlyMessageCacheEntry)
      }
    }

    if (unhandled.isNotEmpty() && earlyMessageCacheEntry != null) {
      PushProcessEarlyMessagesJob.enqueue()
    }
  }

  private fun handleSynchronizeConversationDeletes(conversationDeletes: List<SyncMessage.DeleteForMe.ConversationDelete>, envelopeTimestamp: Long) {
    for (delete in conversationDeletes) {
      val threadRecipientId: RecipientId? = delete.conversation?.toRecipientId()

      if (threadRecipientId == null) {
        warn(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Could not find matching conversation recipient")
        continue
      }

      val threadId = ZonaRosaDatabase.threads.getThreadIdFor(threadRecipientId)
      if (threadId == null) {
        log(envelopeTimestamp, "[handleSynchronizeDeleteForMe] No thread for matching conversation for recipient: $threadRecipientId")
        continue
      }

      var latestReceivedAt = ZonaRosaDatabase.messages.getLatestReceivedAt(threadId, delete.mostRecentMessages.mapNotNull { it.toSyncMessageId(envelopeTimestamp) })

      if (latestReceivedAt == null && delete.mostRecentNonExpiringMessages.isNotEmpty()) {
        log(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Using backup non-expiring messages")
        latestReceivedAt = ZonaRosaDatabase.messages.getLatestReceivedAt(threadId, delete.mostRecentNonExpiringMessages.mapNotNull { it.toSyncMessageId(envelopeTimestamp) })
      }

      if (latestReceivedAt != null) {
        ZonaRosaDatabase.threads.trimThread(threadId = threadId, syncThreadTrimDeletes = false, trimBeforeDate = latestReceivedAt, inclusive = true)

        if (delete.isFullDelete == true) {
          val deleted = ZonaRosaDatabase.threads.deleteConversationIfContainsOnlyLocal(threadId)

          if (deleted) {
            log(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Deleted thread with only local remaining")
          }
        }
      } else {
        warn(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Unable to find most recent received at timestamp for recipient: $threadRecipientId thread: $threadId")
      }
    }
  }

  private fun handleSynchronizeLocalOnlyConversationDeletes(conversationDeletes: List<SyncMessage.DeleteForMe.LocalOnlyConversationDelete>, envelopeTimestamp: Long) {
    for (delete in conversationDeletes) {
      val threadRecipientId: RecipientId? = delete.conversation?.toRecipientId()

      if (threadRecipientId == null) {
        warn(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Could not find matching conversation recipient")
        continue
      }

      val threadId = ZonaRosaDatabase.threads.getThreadIdFor(threadRecipientId)
      if (threadId == null) {
        log(envelopeTimestamp, "[handleSynchronizeDeleteForMe] No thread for matching conversation for recipient: $threadRecipientId")
        continue
      }

      val deleted = ZonaRosaDatabase.threads.deleteConversationIfContainsOnlyLocal(threadId)
      if (!deleted) {
        log(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Thread is not local only or already empty recipient: $threadRecipientId thread: $threadId")
      }
    }
  }

  private fun handleSynchronizeAttachmentDeletes(attachmentDeletes: List<SyncMessage.DeleteForMe.AttachmentDelete>, envelopeTimestamp: Long, earlyMessageCacheEntry: EarlyMessageCacheEntry?) {
    val toDelete: List<AttachmentTable.SyncAttachmentId> = attachmentDeletes
      .mapNotNull { delete ->
        delete.toSyncAttachmentId(delete.targetMessage?.toSyncMessageId(envelopeTimestamp), envelopeTimestamp)
      }

    val unhandled: List<MessageTable.SyncMessageId> = ZonaRosaDatabase.attachments.deleteAttachments(toDelete)

    for (syncMessage in unhandled) {
      warn(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Could not find matching message for attachment delete! timestamp: ${syncMessage.timetamp}  author: ${syncMessage.recipientId}")
      if (earlyMessageCacheEntry != null) {
        AppDependencies.earlyMessageCache.store(syncMessage.recipientId, syncMessage.timetamp, earlyMessageCacheEntry)
      }
    }

    if (unhandled.isNotEmpty() && earlyMessageCacheEntry != null) {
      PushProcessEarlyMessagesJob.enqueue()
    }
  }

  private fun handleSynchronizeAttachmentBackfillRequest(request: SyncMessage.AttachmentBackfillRequest, timestamp: Long) {
    if (request.targetMessage == null || request.targetConversation == null) {
      warn(timestamp, "[AttachmentBackfillRequest] Target message or target conversation was unset! Can't formulate a response, ignoring.")
      return
    }

    val syncMessageId = request.targetMessage!!.toSyncMessageId(timestamp)
    if (syncMessageId == null) {
      warn(timestamp, "[AttachmentBackfillRequest] Invalid targetMessageId! Can't formulate a response, ignoring.")
      MultiDeviceAttachmentBackfillMissingJob.enqueue(request.targetMessage!!, request.targetConversation!!)
      return
    }

    val conversationRecipientId: RecipientId? = request.targetConversation!!.toRecipientId()
    if (conversationRecipientId == null) {
      warn(timestamp, "[AttachmentBackfillRequest] Failed to find the target conversation! Enqueuing a 'missing' response.")
      MultiDeviceAttachmentBackfillMissingJob.enqueue(request.targetMessage!!, request.targetConversation!!)
      return
    }

    val threadId = ZonaRosaDatabase.threads.getThreadIdFor(conversationRecipientId)
    if (threadId == null) {
      warn(timestamp, "[AttachmentBackfillRequest] No thread exists for the conversation! Enqueuing a 'missing' response.")
      MultiDeviceAttachmentBackfillMissingJob.enqueue(request.targetMessage!!, request.targetConversation!!)
      return
    }

    val messageId: Long? = ZonaRosaDatabase.messages.getMessageIdOrNull(syncMessageId, threadId)
    if (messageId == null) {
      warn(timestamp, "[AttachmentBackfillRequest] Unable to find message! Enqueuing a 'missing' response.")
      MultiDeviceAttachmentBackfillMissingJob.enqueue(request.targetMessage!!, request.targetConversation!!)
      return
    }

    val attachments: List<DatabaseAttachment> = ZonaRosaDatabase.attachments.getAttachmentsForMessage(messageId).filterNot { it.quote }.sortedBy { it.displayOrder }
    if (attachments.isEmpty()) {
      warn(timestamp, "[AttachmentBackfillRequest] There were no attachments found for the message! Enqueuing a 'missing' response.")
      MultiDeviceAttachmentBackfillMissingJob.enqueue(request.targetMessage!!, request.targetConversation!!)
      return
    }

    val now = System.currentTimeMillis()
    val needsUpload = attachments.filter { now - it.uploadTimestamp > 3.days.inWholeMilliseconds }
    log(timestamp, "[AttachmentBackfillRequest] ${needsUpload.size}/${attachments.size} attachments need to be re-uploaded.")

    for (attachment in needsUpload) {
      AppDependencies.jobManager
        .startChain(AttachmentUploadJob(attachment.attachmentId))
        .then(MultiDeviceAttachmentBackfillUpdateJob(request.targetMessage!!, request.targetConversation!!, messageId))
        .enqueue()
    }

    // Enqueueing an update immediately to tell the requesting device that the primary is online.
    MultiDeviceAttachmentBackfillUpdateJob.enqueue(request.targetMessage!!, request.targetConversation!!, messageId)
  }

  private fun handleSynchronizedPollCreate(
    envelope: Envelope,
    message: DataMessage,
    sent: Sent,
    senderRecipient: Recipient
  ): Long {
    log(envelope.timestamp!!, "Synchronize sent poll creation message.")

    val recipient = getSyncMessageDestination(sent)
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)

    val expiresInMillis = message.expireTimerDuration.inWholeMilliseconds
    if (recipient.expiresInSeconds != message.expireTimerDuration.inWholeSeconds.toInt() || ((message.expireTimerVersion ?: -1) > recipient.expireTimerVersion)) {
      handleSynchronizeSentExpirationUpdate(sent, sideEffect = true)
    }

    val poll: DataMessage.PollCreate = message.pollCreate!!
    val outgoingMessage = OutgoingMessage.pollMessage(
      threadRecipient = recipient,
      sentTimeMillis = sent.timestamp!!,
      expiresIn = recipient.expiresInSeconds.seconds.inWholeMilliseconds,
      poll = Poll(
        question = poll.question!!,
        allowMultipleVotes = poll.allowMultiple!!,
        pollOptions = poll.options,
        authorId = senderRecipient.id.toLong()
      ),
      question = poll.question!!
    )

    val receiptStatus = if (recipient.isGroup) GroupReceiptTable.STATUS_UNKNOWN else GroupReceiptTable.STATUS_UNDELIVERED
    val messageId = ZonaRosaDatabase.messages.insertMessageOutbox(outgoingMessage, threadId, false, receiptStatus, null).messageId

    if (recipient.isGroup) {
      updateGroupReceiptStatus(sent, messageId, recipient.requireGroupId())
    }

    log(envelope.timestamp!!, "Inserted sync poll create message as messageId $messageId")

    ZonaRosaDatabase.messages.markAsSent(messageId, true)

    if (expiresInMillis > 0) {
      ZonaRosaDatabase.messages.markExpireStarted(messageId, sent.expirationStartTimestamp ?: 0)
      AppDependencies.expiringMessageManager.scheduleDeletion(messageId, recipient.isGroup, sent.expirationStartTimestamp ?: 0, expiresInMillis)
    }

    return threadId
  }

  private fun handleSynchronizedPollEnd(
    envelope: Envelope,
    message: DataMessage,
    sent: Sent,
    senderRecipient: Recipient,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ): Long {
    log(envelope.timestamp!!, "Synchronize sent poll terminate message")

    val recipient = getSyncMessageDestination(sent)
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)

    val expiresInMillis = message.expireTimerDuration.inWholeMilliseconds
    if (recipient.expiresInSeconds != message.expireTimerDuration.inWholeSeconds.toInt() || ((message.expireTimerVersion ?: -1) > recipient.expireTimerVersion)) {
      handleSynchronizeSentExpirationUpdate(sent, sideEffect = true)
    }

    val pollTerminate = message.pollTerminate!!
    val targetMessage = ZonaRosaDatabase.messages.getMessageFor(pollTerminate.targetSentTimestamp!!, Recipient.self().id)
    if (targetMessage == null) {
      warn(envelope.timestamp!!, "Unable to find target message for poll termination. Putting in early message cache.")
      if (earlyMessageCacheEntry != null) {
        AppDependencies.earlyMessageCache.store(senderRecipient.id, pollTerminate.targetSentTimestamp!!, earlyMessageCacheEntry)
        PushProcessEarlyMessagesJob.enqueue()
      }
      return -1
    }
    val poll = ZonaRosaDatabase.polls.getPoll(targetMessage.id)
    if (poll == null) {
      warn(envelope.timestamp!!, "Unable to find poll for poll termination. Dropping.")
      return -1
    }

    val outgoingMessage = OutgoingMessage.pollTerminateMessage(
      threadRecipient = recipient,
      sentTimeMillis = sent.timestamp!!,
      expiresIn = recipient.expiresInSeconds.seconds.inWholeMilliseconds,
      messageExtras = MessageExtras(
        pollTerminate = PollTerminate(
          question = poll.question,
          messageId = poll.messageId,
          targetTimestamp = pollTerminate.targetSentTimestamp!!
        )
      )
    )

    val receiptStatus = if (recipient.isGroup) GroupReceiptTable.STATUS_UNKNOWN else GroupReceiptTable.STATUS_UNDELIVERED
    val messageId = ZonaRosaDatabase.messages.insertMessageOutbox(outgoingMessage, threadId, false, receiptStatus, null).messageId
    ZonaRosaDatabase.messages.markAsSent(messageId, true)

    log(envelope.timestamp!!, "Inserted sync poll end message as messageId $messageId")

    if (expiresInMillis > 0) {
      ZonaRosaDatabase.messages.markExpireStarted(messageId, sent.expirationStartTimestamp ?: 0)
      AppDependencies.expiringMessageManager.scheduleDeletion(messageId, recipient.isGroup, sent.expirationStartTimestamp ?: 0, expiresInMillis)
    }

    return threadId
  }

  private fun handleSynchronizedPinMessage(
    envelope: Envelope,
    message: DataMessage,
    sent: Sent,
    senderRecipient: Recipient,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ): Long {
    if (!RemoteConfig.receivePinnedMessages) {
      log(envelope.timestamp!!, "Sync pinned messages not allowed due to remote config.")
    }

    log(envelope.timestamp!!, "Synchronize pinned message")

    val recipient = getSyncMessageDestination(sent)
    val threadId = ZonaRosaDatabase.threads.getOrCreateThreadIdFor(recipient)

    val expiresInMillis = message.expireTimerDuration.inWholeMilliseconds
    if (recipient.expiresInSeconds != message.expireTimerDuration.inWholeSeconds.toInt() || ((message.expireTimerVersion ?: -1) > recipient.expireTimerVersion)) {
      handleSynchronizeSentExpirationUpdate(sent, sideEffect = true)
    }

    val pinMessage = message.pinMessage!!
    val targetAuthorServiceId: ServiceId = ACI.parseOrThrow(pinMessage.targetAuthorAciBinary!!)
    if (targetAuthorServiceId.isUnknown) {
      warn(envelope.timestamp!!, "Unknown author")
      return -1
    }

    val targetAuthor = Recipient.externalPush(targetAuthorServiceId)
    val targetMessage = ZonaRosaDatabase.messages.getMessageFor(pinMessage.targetSentTimestamp!!, targetAuthor.id)
    if (targetMessage == null) {
      warn(envelope.timestamp!!, "Unable to find target message for sync message. Putting in early message cache.")
      if (earlyMessageCacheEntry != null) {
        AppDependencies.earlyMessageCache.store(senderRecipient.id, pinMessage.targetSentTimestamp!!, earlyMessageCacheEntry)
        PushProcessEarlyMessagesJob.enqueue()
      }
      return -1
    }

    if (targetMessage.hasGiftBadge()) {
      warn(envelope.timestamp!!, "Cannot pin gift badge")
      return -1
    }

    val targetMessageId = (targetMessage as? MmsMessageRecord)?.latestRevisionId?.id ?: targetMessage.id
    val duration = if (pinMessage.pinDurationForever == true) MessageTable.PIN_FOREVER else pinMessage.pinDurationSeconds!!.toLong()
    val outgoingMessage = OutgoingMessage.pinMessage(
      threadRecipient = recipient,
      sentTimeMillis = sent.timestamp!!,
      expiresIn = recipient.expiresInSeconds.seconds.inWholeMilliseconds,
      messageExtras = MessageExtras(pinnedMessage = PinnedMessage(pinnedMessageId = targetMessageId, targetAuthorAci = pinMessage.targetAuthorAciBinary!!, targetTimestamp = pinMessage.targetSentTimestamp!!, pinDurationInSeconds = duration))
    )

    val messageId = ZonaRosaDatabase.messages.insertMessageOutbox(outgoingMessage, threadId, false, GroupReceiptTable.STATUS_UNKNOWN, null).messageId
    ZonaRosaDatabase.messages.markAsSent(messageId, true)

    log(envelope.timestamp!!, "Inserted sync pin message as messageId $messageId")

    if (expiresInMillis > 0) {
      ZonaRosaDatabase.messages.markExpireStarted(messageId, sent.expirationStartTimestamp ?: 0)
      AppDependencies.expiringMessageManager.scheduleDeletion(messageId, recipient.isGroup, sent.expirationStartTimestamp ?: 0, expiresInMillis)
    }

    return threadId
  }

  private fun ConversationIdentifier.toRecipientId(): RecipientId? {
    val threadServiceId = ServiceId.parseOrNull(this.threadServiceId, this.threadServiceIdBinary)
    return when {
      threadGroupId != null -> {
        try {
          val groupId: GroupId = GroupId.push(threadGroupId!!)
          Recipient.externalPossiblyMigratedGroup(groupId).id
        } catch (e: BadGroupIdException) {
          null
        }
      }

      threadServiceId != null -> {
        ZonaRosaDatabase.recipients.getOrInsertFromServiceId(threadServiceId)
      }

      threadE164 != null -> {
        ZonaRosaE164Util.formatAsE164(threadE164!!)?.let {
          ZonaRosaDatabase.recipients.getOrInsertFromE164(threadE164!!)
        }
      }

      else -> null
    }
  }

  private fun AddressableMessage.toSyncMessageId(envelopeTimestamp: Long): MessageTable.SyncMessageId? {
    return if (this.sentTimestamp != null && Utils.anyNotNull(this.authorServiceId, this.authorServiceIdBinary) || this.authorE164 != null) {
      val serviceId = ServiceId.parseOrNull(this.authorServiceId, this.authorServiceIdBinary)
      val id = if (serviceId != null) {
        ZonaRosaDatabase.recipients.getOrInsertFromServiceId(serviceId)
      } else {
        ZonaRosaDatabase.recipients.getOrInsertFromE164(this.authorE164!!)
      }

      MessageTable.SyncMessageId(id, this.sentTimestamp!!)
    } else {
      warn(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Invalid delete sync missing timestamp or author")
      null
    }
  }

  private fun SyncMessage.DeleteForMe.AttachmentDelete.toSyncAttachmentId(syncMessageId: MessageTable.SyncMessageId?, envelopeTimestamp: Long): AttachmentTable.SyncAttachmentId? {
    val uuid = UuidUtil.fromByteStringOrNull(clientUuid)
    val digest = fallbackDigest?.toByteArray()
    val plaintextHash = fallbackPlaintextHash?.let { Base64.encodeWithPadding(it.toByteArray()) }

    if (syncMessageId == null || (uuid == null && digest == null && plaintextHash == null)) {
      warn(envelopeTimestamp, "[handleSynchronizeDeleteForMe] Invalid delete sync attachment missing identifiers")
      return null
    } else {
      return AttachmentTable.SyncAttachmentId(syncMessageId, uuid, digest, plaintextHash)
    }
  }
}

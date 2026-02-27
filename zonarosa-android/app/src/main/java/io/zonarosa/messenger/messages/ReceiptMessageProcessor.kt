package io.zonarosa.messenger.messages

import android.annotation.SuppressLint
import android.content.Context
import io.zonarosa.core.util.Stopwatch
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.PushProcessEarlyMessagesJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.messages.MessageContentProcessor.Companion.log
import io.zonarosa.messenger.messages.MessageContentProcessor.Companion.warn
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.util.EarlyMessageCacheEntry
import io.zonarosa.messenger.util.ZonaRosaTrace
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.crypto.EnvelopeMetadata
import io.zonarosa.service.internal.push.Content
import io.zonarosa.service.internal.push.Envelope
import io.zonarosa.service.internal.push.ReceiptMessage

object ReceiptMessageProcessor {
  private val TAG = MessageContentProcessor.TAG

  private const val VERBOSE = false

  fun process(context: Context, senderRecipient: Recipient, envelope: Envelope, content: Content, metadata: EnvelopeMetadata, earlyMessageCacheEntry: EarlyMessageCacheEntry?, batchCache: BatchCache) {
    val receiptMessage = content.receiptMessage!!

    when (receiptMessage.type) {
      ReceiptMessage.Type.DELIVERY -> handleDeliveryReceipt(envelope, metadata, receiptMessage, senderRecipient.id, batchCache)
      ReceiptMessage.Type.READ -> handleReadReceipt(context, senderRecipient.id, envelope, metadata, receiptMessage, earlyMessageCacheEntry)
      ReceiptMessage.Type.VIEWED -> handleViewedReceipt(context, envelope, metadata, receiptMessage, senderRecipient.id, earlyMessageCacheEntry)
      else -> warn(envelope.timestamp!!, "Unknown recipient message type ${receiptMessage.type}")
    }
  }

  @SuppressLint("DefaultLocale")
  private fun handleDeliveryReceipt(
    envelope: Envelope,
    metadata: EnvelopeMetadata,
    deliveryReceipt: ReceiptMessage,
    senderRecipientId: RecipientId,
    batchCache: BatchCache
  ) {
    log(envelope.timestamp!!, "Processing delivery receipts. Sender: $senderRecipientId, Device: ${metadata.sourceDeviceId}, Timestamps: ${deliveryReceipt.timestamp.joinToString(", ")}")
    val stopwatch: Stopwatch? = if (VERBOSE) Stopwatch("delivery-receipt", decimalPlaces = 2) else null

    ZonaRosaTrace.beginSection("ReceiptMessageProcessor#incrementDeliveryReceiptCounts")
    val missingTargetTimestamps: Set<Long> = ZonaRosaDatabase.messages.incrementDeliveryReceiptCounts(deliveryReceipt.timestamp, senderRecipientId, envelope.timestamp!!, stopwatch)
    ZonaRosaTrace.endSection()

    for (targetTimestamp in missingTargetTimestamps) {
      warn(envelope.timestamp!!, "[handleDeliveryReceipt] Could not find matching message! targetTimestamp: $targetTimestamp, receiptAuthor: $senderRecipientId")
      // Early delivery receipts are special-cased in the database methods
    }

    if (missingTargetTimestamps.isNotEmpty()) {
      PushProcessEarlyMessagesJob.enqueue()
    }

    ZonaRosaDatabase.pendingPniSignatureMessages.acknowledgeReceipts(senderRecipientId, deliveryReceipt.timestamp, metadata.sourceDeviceId)
    stopwatch?.split("pni-signatures")

    batchCache.addMslDelete(senderRecipientId, metadata.sourceDeviceId, deliveryReceipt.timestamp)
    stopwatch?.split("msl")

    stopwatch?.stop(TAG)
  }

  @SuppressLint("DefaultLocale")
  private fun handleReadReceipt(
    context: Context,
    senderRecipientId: RecipientId,
    envelope: Envelope,
    metadata: EnvelopeMetadata,
    readReceipt: ReceiptMessage,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ) {
    if (!ZonaRosaPreferences.isReadReceiptsEnabled(context)) {
      log(envelope.timestamp!!, "Ignoring read receipts for IDs: " + readReceipt.timestamp.joinToString(", "))
      return
    }

    log(envelope.timestamp!!, "Processing read receipts. Sender: $senderRecipientId, Device: ${metadata.sourceDeviceId}, Timestamps: ${readReceipt.timestamp.joinToString(", ")}")

    ZonaRosaTrace.beginSection("ReceiptMessageProcessor#incrementReadReceiptCounts")
    val missingTargetTimestamps: Set<Long> = ZonaRosaDatabase.messages.incrementReadReceiptCounts(readReceipt.timestamp, senderRecipientId, envelope.timestamp!!)
    ZonaRosaTrace.endSection()

    if (missingTargetTimestamps.isNotEmpty()) {
      val selfId = Recipient.self().id

      for (targetTimestamp in missingTargetTimestamps) {
        warn(envelope.timestamp!!, "[handleReadReceipt] Could not find matching message! targetTimestamp: $targetTimestamp, receiptAuthor: $senderRecipientId | Receipt, so associating with message from self ($selfId)")
        if (earlyMessageCacheEntry != null) {
          AppDependencies.earlyMessageCache.store(selfId, targetTimestamp, earlyMessageCacheEntry)
        }
      }
    }

    if (missingTargetTimestamps.isNotEmpty() && earlyMessageCacheEntry != null) {
      PushProcessEarlyMessagesJob.enqueue()
    }
  }

  private fun handleViewedReceipt(
    context: Context,
    envelope: Envelope,
    metadata: EnvelopeMetadata,
    viewedReceipt: ReceiptMessage,
    senderRecipientId: RecipientId,
    earlyMessageCacheEntry: EarlyMessageCacheEntry?
  ) {
    val readReceipts = ZonaRosaPreferences.isReadReceiptsEnabled(context)
    val storyViewedReceipts = ZonaRosaStore.story.viewedReceiptsEnabled

    if (!readReceipts && !storyViewedReceipts) {
      log(envelope.timestamp!!, "Ignoring viewed receipts for IDs: ${viewedReceipt.timestamp.joinToString(", ")}")
      return
    }

    log(envelope.timestamp!!, "Processing viewed receipts. Sender: $senderRecipientId, Device: ${metadata.sourceDeviceId}, Only Stories: ${!readReceipts}, Timestamps: ${viewedReceipt.timestamp.joinToString(", ")}")

    val missingTargetTimestamps: Set<Long> = if (readReceipts && storyViewedReceipts) {
      ZonaRosaDatabase.messages.incrementViewedReceiptCounts(viewedReceipt.timestamp, senderRecipientId, envelope.timestamp!!)
    } else if (readReceipts) {
      ZonaRosaDatabase.messages.incrementViewedNonStoryReceiptCounts(viewedReceipt.timestamp, senderRecipientId, envelope.timestamp!!)
    } else {
      ZonaRosaDatabase.messages.incrementViewedStoryReceiptCounts(viewedReceipt.timestamp, senderRecipientId, envelope.timestamp!!)
    }

    val foundTargetTimestamps: Set<Long> = viewedReceipt.timestamp.toSet() - missingTargetTimestamps.toSet()
    ZonaRosaDatabase.messages.updateViewedStories(foundTargetTimestamps)

    if (missingTargetTimestamps.isNotEmpty()) {
      val selfId = Recipient.self().id

      for (targetTimestamp in missingTargetTimestamps) {
        warn(envelope.timestamp!!, "[handleViewedReceipt] Could not find matching message! targetTimestamp: $targetTimestamp, receiptAuthor: $senderRecipientId | Receipt so associating with message from self ($selfId)")
        if (earlyMessageCacheEntry != null) {
          AppDependencies.earlyMessageCache.store(selfId, targetTimestamp, earlyMessageCacheEntry)
        }
      }
    }

    if (missingTargetTimestamps.isNotEmpty() && earlyMessageCacheEntry != null) {
      PushProcessEarlyMessagesJob.enqueue()
    }
  }
}

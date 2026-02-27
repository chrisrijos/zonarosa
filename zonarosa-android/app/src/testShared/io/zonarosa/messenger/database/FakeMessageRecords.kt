package io.zonarosa.messenger.database

import io.zonarosa.blurhash.BlurHash
import io.zonarosa.core.models.media.TransformProperties
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.attachments.Cdn
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.audio.AudioHash
import io.zonarosa.messenger.contactshare.Contact
import io.zonarosa.messenger.database.documents.IdentityKeyMismatch
import io.zonarosa.messenger.database.documents.NetworkFailure
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.database.model.ParentStoryId
import io.zonarosa.messenger.database.model.Quote
import io.zonarosa.messenger.database.model.ReactionRecord
import io.zonarosa.messenger.database.model.StoryType
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList
import io.zonarosa.messenger.database.model.databaseprotos.GiftBadge
import io.zonarosa.messenger.linkpreview.LinkPreview
import io.zonarosa.messenger.mms.SlideDeck
import io.zonarosa.messenger.payments.Payment
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.stickers.StickerLocator
import io.zonarosa.messenger.util.MediaUtil

/**
 * Builds MessageRecords and related components for direct usage in unit testing. Does not modify the database.
 */
object FakeMessageRecords {

  fun buildDatabaseAttachment(
    attachmentId: AttachmentId = AttachmentId(1),
    mmsId: Long = 1,
    hasData: Boolean = true,
    hasThumbnail: Boolean = true,
    hasArchiveThumbnail: Boolean = false,
    contentType: String = MediaUtil.IMAGE_JPEG,
    transferProgress: Int = AttachmentTable.TRANSFER_PROGRESS_DONE,
    size: Long = 0L,
    fileName: String = "",
    cdnNumber: Int = 3,
    location: String = "",
    key: String = "",
    iv: ByteArray = byteArrayOf(),
    relay: String = "",
    digest: ByteArray = byteArrayOf(),
    incrementalDigest: ByteArray = byteArrayOf(),
    incrementalMacChunkSize: Int = 0,
    fastPreflightId: String = "",
    voiceNote: Boolean = false,
    borderless: Boolean = false,
    videoGif: Boolean = false,
    width: Int = 0,
    height: Int = 0,
    quote: Boolean = false,
    caption: String? = null,
    stickerLocator: StickerLocator? = null,
    blurHash: BlurHash? = null,
    audioHash: AudioHash? = null,
    transformProperties: TransformProperties? = null,
    displayOrder: Int = 0,
    uploadTimestamp: Long = 200,
    dataHash: String? = null,
    archiveCdn: Int = 0,
    archiveMediaName: String? = null,
    archiveMediaId: String? = null,
    archiveThumbnailId: String? = null,
    thumbnailRestoreState: AttachmentTable.ThumbnailRestoreState = AttachmentTable.ThumbnailRestoreState.NONE,
    archiveTransferState: AttachmentTable.ArchiveTransferState = AttachmentTable.ArchiveTransferState.NONE
  ): DatabaseAttachment {
    return DatabaseAttachment(
      attachmentId = attachmentId,
      mmsId = mmsId,
      hasData = hasData,
      hasThumbnail = hasThumbnail,
      contentType = contentType,
      transferProgress = transferProgress,
      size = size,
      fileName = fileName,
      cdn = Cdn.fromCdnNumber(cdnNumber),
      location = location,
      key = key,
      digest = digest,
      incrementalDigest = incrementalDigest,
      incrementalMacChunkSize = incrementalMacChunkSize,
      fastPreflightId = fastPreflightId,
      voiceNote = voiceNote,
      borderless = borderless,
      videoGif = videoGif,
      width = width,
      height = height,
      quote = quote,
      caption = caption,
      stickerLocator = stickerLocator,
      blurHash = blurHash,
      audioHash = audioHash,
      transformProperties = transformProperties,
      displayOrder = displayOrder,
      uploadTimestamp = uploadTimestamp,
      dataHash = dataHash,
      archiveCdn = archiveCdn,
      thumbnailRestoreState = thumbnailRestoreState,
      archiveTransferState = archiveTransferState,
      uuid = null,
      quoteTargetContentType = null,
      metadata = null
    )
  }

  fun buildLinkPreview(
    url: String = "",
    title: String = "",
    description: String = "",
    date: Long = 200,
    attachmentId: AttachmentId? = null
  ): LinkPreview {
    return LinkPreview(
      url,
      title,
      description,
      date,
      attachmentId
    )
  }

  fun buildMediaMmsMessageRecord(
    id: Long = 1,
    conversationRecipient: Recipient = Recipient.UNKNOWN,
    individualRecipient: Recipient = conversationRecipient,
    recipientDeviceId: Int = 1,
    dateSent: Long = 200,
    dateReceived: Long = 400,
    dateServer: Long = 300,
    hasDeliveryReceipt: Boolean = false,
    threadId: Long = 1,
    body: String = "body",
    slideDeck: SlideDeck = SlideDeck(),
    partCount: Int = slideDeck.slides.count(),
    mailbox: Long = MessageTypes.BASE_INBOX_TYPE,
    mismatches: Set<IdentityKeyMismatch> = emptySet(),
    failures: Set<NetworkFailure> = emptySet(),
    subscriptionId: Int = -1,
    expiresIn: Long = -1,
    expireStarted: Long = -1,
    expireTimerVersion: Int = individualRecipient.expireTimerVersion,
    viewOnce: Boolean = false,
    hasReadReceipt: Boolean = false,
    quote: Quote? = null,
    contacts: List<Contact> = emptyList(),
    linkPreviews: List<LinkPreview> = emptyList(),
    unidentified: Boolean = false,
    reactions: List<ReactionRecord> = emptyList(),
    remoteDelete: Boolean = false,
    mentionsSelf: Boolean = false,
    notifiedTimestamp: Long = 350,
    viewed: Boolean = false,
    receiptTimestamp: Long = 0,
    messageRanges: BodyRangeList? = null,
    storyType: StoryType = StoryType.NONE,
    parentStoryId: ParentStoryId? = null,
    giftBadge: GiftBadge? = null,
    payment: Payment? = null,
    call: CallTable.Call? = null
  ): MmsMessageRecord {
    return MmsMessageRecord(
      id,
      conversationRecipient,
      recipientDeviceId,
      individualRecipient,
      dateSent,
      dateReceived,
      dateServer,
      hasDeliveryReceipt,
      threadId,
      body,
      slideDeck,
      mailbox,
      mismatches,
      failures,
      subscriptionId,
      expiresIn,
      expireStarted,
      expireTimerVersion,
      viewOnce,
      hasReadReceipt,
      quote,
      contacts,
      linkPreviews,
      unidentified,
      reactions,
      mentionsSelf,
      notifiedTimestamp,
      viewed,
      receiptTimestamp,
      messageRanges,
      storyType,
      parentStoryId,
      giftBadge,
      payment,
      call,
      null,
      -1,
      null,
      null,
      0,
      false,
      0,
      null,
      null
    )
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.util

import okio.ByteString
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.UuidUtil
import io.zonarosa.core.util.isNotNullOrBlank
import io.zonarosa.core.util.nullIfBlank
import io.zonarosa.core.util.orNull
import io.zonarosa.libzonarosa.usernames.BaseUsernameException
import io.zonarosa.libzonarosa.usernames.Username
import io.zonarosa.messenger.attachments.ArchivedAttachment
import io.zonarosa.messenger.attachments.Attachment
import io.zonarosa.messenger.attachments.Cdn
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.attachments.PointerAttachment
import io.zonarosa.messenger.attachments.TombstoneAttachment
import io.zonarosa.messenger.backup.v2.BackupMode
import io.zonarosa.messenger.backup.v2.ExportState
import io.zonarosa.messenger.backup.v2.proto.FilePointer
import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.conversation.colors.ChatColors
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.stickers.StickerLocator
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId
import java.util.Optional
import io.zonarosa.messenger.backup.v2.proto.AvatarColor as RemoteAvatarColor

/**
 * Converts a [FilePointer] to a local [Attachment] object for inserting into the database.
 */
fun FilePointer?.toLocalAttachment(
  voiceNote: Boolean = false,
  borderless: Boolean = false,
  gif: Boolean = false,
  wasDownloaded: Boolean = false,
  stickerLocator: StickerLocator? = null,
  contentType: String? = this?.contentType,
  fileName: String? = this?.fileName,
  uuid: ByteString? = null,
  quote: Boolean = false,
  quoteTargetContentType: String? = null
): Attachment? {
  if (this == null || this.locatorInfo == null) return null

  val attachmentType = when {
    this.locatorInfo.plaintextHash != null -> AttachmentType.ARCHIVE
    this.locatorInfo.encryptedDigest != null && this.locatorInfo.transitCdnKey != null -> AttachmentType.TRANSIT
    else -> AttachmentType.INVALID
  }

  return when (attachmentType) {
    AttachmentType.ARCHIVE -> {
      ArchivedAttachment(
        contentType = contentType,
        size = this.locatorInfo.size.toLong(),
        cdn = this.locatorInfo.transitCdnNumber ?: Cdn.CDN_0.cdnNumber,
        uploadTimestamp = this.locatorInfo.transitTierUploadTimestamp ?: 0,
        key = this.locatorInfo.key.toByteArray(),
        cdnKey = this.locatorInfo.transitCdnKey?.nullIfBlank(),
        archiveCdn = this.locatorInfo.mediaTierCdnNumber,
        plaintextHash = this.locatorInfo.plaintextHash!!.toByteArray(),
        incrementalMac = this.incrementalMac?.toByteArray(),
        incrementalMacChunkSize = this.incrementalMacChunkSize,
        width = this.width,
        height = this.height,
        caption = this.caption,
        blurHash = this.blurHash,
        voiceNote = voiceNote,
        borderless = borderless,
        stickerLocator = stickerLocator,
        gif = gif,
        quote = quote,
        quoteTargetContentType = quoteTargetContentType,
        uuid = UuidUtil.fromByteStringOrNull(uuid),
        fileName = fileName,
        localBackupKey = this.locatorInfo.localKey?.toByteArray()
      )
    }
    AttachmentType.TRANSIT -> {
      val zonarosaAttachmentPointer = ZonaRosaServiceAttachmentPointer(
        cdnNumber = this.locatorInfo.transitCdnNumber ?: Cdn.CDN_0.cdnNumber,
        remoteId = ZonaRosaServiceAttachmentRemoteId.from(locatorInfo.transitCdnKey!!),
        contentType = contentType,
        key = this.locatorInfo.key.toByteArray(),
        size = Optional.ofNullable(locatorInfo.size),
        preview = Optional.empty(),
        width = this.width ?: 0,
        height = this.height ?: 0,
        digest = Optional.ofNullable(this.locatorInfo.encryptedDigest!!.toByteArray()),
        incrementalDigest = Optional.ofNullable(this.incrementalMac?.toByteArray()),
        incrementalMacChunkSize = this.incrementalMacChunkSize ?: 0,
        fileName = Optional.ofNullable(fileName),
        voiceNote = voiceNote,
        isBorderless = borderless,
        isGif = gif,
        caption = Optional.ofNullable(this.caption),
        blurHash = Optional.ofNullable(this.blurHash),
        uploadTimestamp = this.locatorInfo.transitTierUploadTimestamp?.clampToValidBackupRange() ?: 0,
        uuid = UuidUtil.fromByteStringOrNull(uuid)
      )
      PointerAttachment.forPointer(
        pointer = Optional.of(zonarosaAttachmentPointer),
        stickerLocator = stickerLocator,
        transferState = if (wasDownloaded) AttachmentTable.TRANSFER_NEEDS_RESTORE else AttachmentTable.TRANSFER_PROGRESS_PENDING,
        quote = quote,
        quoteTargetContentType = quoteTargetContentType
      ).orNull()
    }
    AttachmentType.INVALID -> {
      TombstoneAttachment(
        contentType = contentType,
        incrementalMac = this.incrementalMac?.toByteArray(),
        incrementalMacChunkSize = this.incrementalMacChunkSize,
        width = this.width,
        height = this.height,
        caption = this.caption,
        fileName = this.fileName,
        blurHash = this.blurHash,
        voiceNote = voiceNote,
        borderless = borderless,
        gif = gif,
        quote = quote,
        quoteTargetContentType = quoteTargetContentType,
        stickerLocator = stickerLocator,
        uuid = UuidUtil.fromByteStringOrNull(uuid)
      )
    }
  }
}

fun DatabaseAttachment.toRemoteFilePointer(contentTypeOverride: String? = null, backupMode: BackupMode): FilePointer {
  val builder = FilePointer.Builder()
  builder.contentType = contentTypeOverride ?: this.contentType?.takeUnless { it.isBlank() }
  builder.incrementalMac = this.incrementalDigest?.takeIf { it.isNotEmpty() && this.incrementalMacChunkSize > 0 }?.toByteString()
  builder.incrementalMacChunkSize = this.incrementalMacChunkSize.takeIf { it > 0 && builder.incrementalMac != null }
  builder.fileName = this.fileName
  builder.width = this.width.takeIf { it > 0 }
  builder.height = this.height.takeIf { it > 0 }
  builder.caption = this.caption
  builder.blurHash = this.blurHash?.hash
  builder.locatorInfo = this.toLocatorInfo(backupMode)

  return builder.build()
}

fun DatabaseAttachment.toLocatorInfo(backupMode: BackupMode): FilePointer.LocatorInfo {
  val attachmentType = this.toRemoteAttachmentType()

  if (attachmentType == AttachmentType.INVALID) {
    return FilePointer.LocatorInfo()
  }

  val locatorBuilder = FilePointer.LocatorInfo.Builder()

  val remoteKey = Base64.decode(this.remoteKey!!).toByteString()

  locatorBuilder.key = remoteKey
  locatorBuilder.size = this.size.toInt()

  if (this.remoteLocation.isNotNullOrBlank()) {
    locatorBuilder.transitCdnKey = this.remoteLocation
    locatorBuilder.transitCdnNumber = this.cdn.cdnNumber
    locatorBuilder.transitTierUploadTimestamp = this.uploadTimestamp.takeIf { it > 0 }?.clampToValidBackupRange()
  }

  @Suppress("KotlinConstantConditions")
  when (attachmentType) {
    AttachmentType.ARCHIVE -> {
      locatorBuilder.plaintextHash = Base64.decode(this.dataHash!!).toByteString()
      locatorBuilder.mediaTierCdnNumber = this.archiveCdn
    }
    AttachmentType.TRANSIT -> {
      locatorBuilder.encryptedDigest = this.remoteDigest!!.toByteString()
    }
    AttachmentType.INVALID -> Unit
  }

  if (backupMode.isLocalBackup && this.dataHash != null && this.metadata?.localBackupKey != null) {
    if (locatorBuilder.plaintextHash == null) {
      locatorBuilder.plaintextHash = Base64.decode(this.dataHash).toByteString()
    }

    locatorBuilder.localKey = this.metadata.localBackupKey.toByteString()
  }

  return locatorBuilder.build()
}

fun Long.clampToValidBackupRange(): Long {
  return this.coerceIn(0, 8640000000000000)
}

fun AvatarColor.toRemote(): RemoteAvatarColor {
  return when (this) {
    AvatarColor.A100 -> RemoteAvatarColor.A100
    AvatarColor.A110 -> RemoteAvatarColor.A110
    AvatarColor.A120 -> RemoteAvatarColor.A120
    AvatarColor.A130 -> RemoteAvatarColor.A130
    AvatarColor.A140 -> RemoteAvatarColor.A140
    AvatarColor.A150 -> RemoteAvatarColor.A150
    AvatarColor.A160 -> RemoteAvatarColor.A160
    AvatarColor.A170 -> RemoteAvatarColor.A170
    AvatarColor.A180 -> RemoteAvatarColor.A180
    AvatarColor.A190 -> RemoteAvatarColor.A190
    AvatarColor.A200 -> RemoteAvatarColor.A200
    AvatarColor.A210 -> RemoteAvatarColor.A210
    AvatarColor.UNKNOWN -> RemoteAvatarColor.A100
    AvatarColor.ON_SURFACE_VARIANT -> RemoteAvatarColor.A100
  }
}

fun RemoteAvatarColor.toLocal(): AvatarColor {
  return when (this) {
    RemoteAvatarColor.A100 -> AvatarColor.A100
    RemoteAvatarColor.A110 -> AvatarColor.A110
    RemoteAvatarColor.A120 -> AvatarColor.A120
    RemoteAvatarColor.A130 -> AvatarColor.A130
    RemoteAvatarColor.A140 -> AvatarColor.A140
    RemoteAvatarColor.A150 -> AvatarColor.A150
    RemoteAvatarColor.A160 -> AvatarColor.A160
    RemoteAvatarColor.A170 -> AvatarColor.A170
    RemoteAvatarColor.A180 -> AvatarColor.A180
    RemoteAvatarColor.A190 -> AvatarColor.A190
    RemoteAvatarColor.A200 -> AvatarColor.A200
    RemoteAvatarColor.A210 -> AvatarColor.A210
  }
}

fun ChatColors.Id.isValid(exportState: ExportState): Boolean {
  return this !is ChatColors.Id.Custom || this.longValue in exportState.customChatColorIds
}

private fun DatabaseAttachment.toRemoteAttachmentType(): AttachmentType {
  if (this.remoteKey.isNullOrBlank()) {
    return AttachmentType.INVALID
  }

  if (this.transferState == AttachmentTable.TRANSFER_PROGRESS_PERMANENT_FAILURE && this.archiveTransferState != AttachmentTable.ArchiveTransferState.FINISHED) {
    return AttachmentType.INVALID
  }

  val activelyOnArchiveCdn = this.archiveTransferState == AttachmentTable.ArchiveTransferState.FINISHED
  val couldBeOnArchiveCdn = (this.transferState == AttachmentTable.TRANSFER_PROGRESS_DONE || this.transferState == AttachmentTable.TRANSFER_NEEDS_RESTORE) && this.archiveTransferState != AttachmentTable.ArchiveTransferState.PERMANENT_FAILURE

  if (this.dataHash != null && (activelyOnArchiveCdn || couldBeOnArchiveCdn)) {
    return AttachmentType.ARCHIVE
  }

  if (this.remoteDigest != null && this.remoteLocation.isNotNullOrBlank()) {
    return AttachmentType.TRANSIT
  }

  return AttachmentType.INVALID
}

fun String.isValidUsername(): Boolean {
  if (this.isBlank()) {
    return false
  }

  return try {
    Username(this)
    true
  } catch (e: BaseUsernameException) {
    false
  }
}

private enum class AttachmentType {
  TRANSIT, ARCHIVE, INVALID
}

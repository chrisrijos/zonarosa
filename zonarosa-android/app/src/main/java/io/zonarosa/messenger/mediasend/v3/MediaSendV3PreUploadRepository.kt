/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.mediasend.v3

import android.content.Context
import androidx.annotation.WorkerThread
import io.zonarosa.core.models.media.Media
import io.zonarosa.mediasend.MediaRecipientId
import io.zonarosa.mediasend.preupload.PreUploadRepository
import io.zonarosa.mediasend.preupload.PreUploadResult
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.mediasend.MediaUploadRepository
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.sms.MessageSender

object MediaSendV3PreUploadRepository : PreUploadRepository {

  @WorkerThread
  override fun preUpload(context: Context, media: Media, recipientId: MediaRecipientId?): PreUploadResult? {
    val attachment = MediaUploadRepository.asAttachment(context, media)
    val recipient = recipientId?.let { Recipient.resolved(RecipientId.from(it.id)) }
    val legacyResult = MessageSender.preUploadPushAttachment(context, attachment, recipient, media) ?: return null
    return PreUploadResult(
      legacyResult.media,
      legacyResult.attachmentId.id,
      legacyResult.jobIds.toMutableList()
    )
  }

  @WorkerThread
  override fun cancelJobs(context: Context, jobIds: List<String>) {
    val jobManager = AppDependencies.jobManager
    jobIds.forEach(jobManager::cancel)
  }

  @WorkerThread
  override fun deleteAttachment(context: Context, attachmentId: Long) {
    ZonaRosaDatabase.attachments.deleteAttachment(AttachmentId(attachmentId))
  }

  @WorkerThread
  override fun updateAttachmentCaption(context: Context, attachmentId: Long, caption: String?) {
    ZonaRosaDatabase.attachments.updateAttachmentCaption(AttachmentId(attachmentId), caption)
  }

  @WorkerThread
  override fun updateDisplayOrder(context: Context, orderMap: Map<Long, Int>) {
    val mapped = orderMap.mapKeys { AttachmentId(it.key) }
    ZonaRosaDatabase.attachments.updateDisplayOrder(mapped)
  }

  @WorkerThread
  override fun deleteAbandonedPreuploadedAttachments(context: Context): Int {
    return ZonaRosaDatabase.attachments.deleteAbandonedPreuploadedAttachments()
  }
}

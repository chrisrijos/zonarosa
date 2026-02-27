/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.RestoreAttachmentThumbnailJob

/**
 * Responsible for managing logic around restore prioritization
 */
object BackupRestoreManager {

  private val reprioritizedAttachments: HashSet<AttachmentId> = HashSet()

  /**
   * Raise priority of all attachments for the included message records.
   *
   * This is so we can make certain attachments get downloaded more quickly
   */
  fun prioritizeAttachmentsIfNeeded(messageRecords: List<MessageRecord>) {
    ZonaRosaExecutors.BOUNDED.execute {
      synchronized(this) {
        val restoringAttachments = messageRecords
          .asSequence()
          .mapNotNull { (it as? MmsMessageRecord?)?.slideDeck?.slides }
          .flatten()
          .mapNotNull { it.asAttachment() as? DatabaseAttachment }
          .filter {
            val needThumbnail = it.thumbnailRestoreState == AttachmentTable.ThumbnailRestoreState.NEEDS_RESTORE && it.transferState == AttachmentTable.TRANSFER_RESTORE_IN_PROGRESS
            (needThumbnail || it.thumbnailRestoreState == AttachmentTable.ThumbnailRestoreState.IN_PROGRESS) && !reprioritizedAttachments.contains(it.attachmentId)
          }
          .map { it.attachmentId to it.mmsId }
          .toSet()

        reprioritizedAttachments += restoringAttachments.map { it.first }

        val thumbnailJobs = restoringAttachments.map { (attachmentId, mmsId) ->
          RestoreAttachmentThumbnailJob(attachmentId = attachmentId, messageId = mmsId, highPriority = true)
        }

        if (thumbnailJobs.isNotEmpty()) {
          AppDependencies.jobManager.addAll(thumbnailJobs)
        }
      }
    }
  }
}

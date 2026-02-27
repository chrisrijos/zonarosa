/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.conversation.clicklisteners

import android.view.View
import kotlinx.collections.immutable.toPersistentList
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.AttachmentCompressionJob
import io.zonarosa.messenger.jobs.AttachmentDownloadJob
import io.zonarosa.messenger.jobs.AttachmentUploadJob
import io.zonarosa.messenger.mms.Slide
import io.zonarosa.messenger.mms.SlidesClickedListener

/**
 * Cancels all attachments passed through to the callback.
 *
 * Creates a persistent copy of the handed list of slides to prevent off-thread
 * manipulation.
 */
internal class AttachmentCancelClickListener : SlidesClickedListener {
  override fun onClick(unused: View, slides: List<Slide>) {
    val toCancel = slides.toPersistentList()

    Log.i(TAG, "Canceling compression/upload/download jobs for ${toCancel.size} items")

    ZonaRosaExecutors.BOUNDED_IO.execute {
      var cancelCount = 0
      for (slide in toCancel) {
        val attachmentId = (slide.asAttachment() as DatabaseAttachment).attachmentId
        val jobsToCancel = AppDependencies.jobManager.find {
          when (it.factoryKey) {
            AttachmentDownloadJob.KEY -> AttachmentDownloadJob.jobSpecMatchesAttachmentId(it, attachmentId)
            AttachmentCompressionJob.KEY -> AttachmentCompressionJob.jobSpecMatchesAttachmentId(it, attachmentId)
            AttachmentUploadJob.KEY -> AttachmentUploadJob.jobSpecMatchesAttachmentId(it, attachmentId)
            else -> false
          }
        }
        jobsToCancel.forEach {
          AppDependencies.jobManager.cancel(it.id)
          cancelCount++
        }
      }
      Log.i(TAG, "Canceled $cancelCount jobs.")
    }
  }

  companion object {
    private val TAG = Log.tag(AttachmentCancelClickListener::class.java)
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.jobs

import android.text.TextUtils
import okhttp3.internal.http2.StreamResetException
import org.greenrobot.eventbus.EventBus
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.inRoundedDays
import io.zonarosa.core.util.logging.Log
import io.zonarosa.protos.resumableuploads.ResumableUpload
import io.zonarosa.messenger.R
import io.zonarosa.messenger.attachments.Attachment
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.attachments.AttachmentUploadUtil
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.events.PartProgressEvent
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobmanager.persistence.JobSpec
import io.zonarosa.messenger.jobs.protos.AttachmentUploadJobData
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.NotPushRegisteredException
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.service.AttachmentProgressService
import io.zonarosa.messenger.transport.UndeliverableMessageException
import io.zonarosa.messenger.util.MediaUtil
import io.zonarosa.messenger.util.MessageUtil
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.attachment.AttachmentUploadResult
import io.zonarosa.service.api.crypto.AttachmentCipherStreamUtil
import io.zonarosa.service.api.messages.AttachmentTransferProgress
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentStream
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResumableUploadResponseCodeException
import io.zonarosa.service.api.push.exceptions.ResumeLocationInvalidException
import io.zonarosa.service.internal.crypto.PaddingInputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Uploads an attachment without alteration.
 *
 * Queue [AttachmentCompressionJob] before to compress.
 */
class AttachmentUploadJob private constructor(
  parameters: Parameters,
  private val attachmentId: AttachmentId,
  private var uploadSpec: ResumableUpload?
) : BaseJob(parameters) {

  companion object {
    const val KEY = "AttachmentUploadJobV3"

    private val TAG = Log.tag(AttachmentUploadJob::class.java)

    private val NETWORK_RESET_THRESHOLD = 1.minutes.inWholeMilliseconds

    val UPLOAD_REUSE_THRESHOLD = 3.days.inWholeMilliseconds

    @JvmStatic
    val maxPlaintextSize: Long
      get() {
        val maxCipherTextSize = RemoteConfig.maxAttachmentSizeBytes
        val maxPaddedSize = AttachmentCipherStreamUtil.getPlaintextLength(maxCipherTextSize)
        return PaddingInputStream.getMaxUnpaddedSize(maxPaddedSize)
      }

    @JvmStatic
    fun jobSpecMatchesAttachmentId(jobSpec: JobSpec, attachmentId: AttachmentId): Boolean {
      if (KEY != jobSpec.factoryKey) {
        return false
      }
      val serializedData = jobSpec.serializedData ?: return false
      val data = AttachmentUploadJobData.ADAPTER.decode(serializedData)
      val parsed = AttachmentId(data.attachmentId)
      return attachmentId == parsed
    }
  }

  constructor(attachmentId: AttachmentId) : this(
    Parameters.Builder()
      .addConstraint(NetworkConstraint.KEY)
      .setLifespan(TimeUnit.DAYS.toMillis(1))
      .setMaxAttempts(Parameters.UNLIMITED)
      .build(),
    attachmentId,
    null
  )

  override fun serialize(): ByteArray {
    return AttachmentUploadJobData(
      attachmentId = attachmentId.id,
      uploadSpec = uploadSpec
    ).encode()
  }

  override fun getFactoryKey(): String = KEY

  override fun shouldTrace(): Boolean = true

  override fun onAdded() {
    Log.i(TAG, "onAdded() $attachmentId")

    val database = ZonaRosaDatabase.attachments
    val attachment = database.getAttachment(attachmentId)

    if (attachment == null) {
      Log.w(TAG, "Could not fetch attachment from database.")
      return
    }

    val pending = attachment.transferState != AttachmentTable.TRANSFER_PROGRESS_DONE && attachment.transferState != AttachmentTable.TRANSFER_PROGRESS_PERMANENT_FAILURE

    if (pending) {
      Log.i(TAG, "onAdded() Marking attachment progress as 'started'")
      database.setTransferState(attachment.mmsId, attachmentId, AttachmentTable.TRANSFER_PROGRESS_STARTED)
    }
  }

  @Throws(Exception::class)
  public override fun onRun() {
    if (!Recipient.self().isRegistered) {
      throw NotPushRegisteredException()
    }

    ZonaRosaDatabase.attachments.createRemoteKeyIfNecessary(attachmentId)

    val databaseAttachment = ZonaRosaDatabase.attachments.getAttachment(attachmentId) ?: throw InvalidAttachmentException("Cannot find the specified attachment.")

    if (MediaUtil.isLongTextType(databaseAttachment.contentType) && databaseAttachment.size > MessageUtil.MAX_TOTAL_BODY_SIZE_BYTES) {
      throw UndeliverableMessageException("Long text attachment is too long! Max size: ${MessageUtil.MAX_TOTAL_BODY_SIZE_BYTES} bytes, Actual size: ${databaseAttachment.size} bytes.")
    }

    val timeSinceUpload = System.currentTimeMillis() - databaseAttachment.uploadTimestamp
    if (timeSinceUpload < UPLOAD_REUSE_THRESHOLD && !TextUtils.isEmpty(databaseAttachment.remoteLocation)) {
      Log.i(TAG, "We can re-use an already-uploaded file. It was uploaded $timeSinceUpload ms (${timeSinceUpload.milliseconds.inRoundedDays()} days) ago. Skipping.")
      ZonaRosaDatabase.attachments.setTransferState(databaseAttachment.mmsId, attachmentId, AttachmentTable.TRANSFER_PROGRESS_DONE)
      if (ZonaRosaStore.account.isPrimaryDevice && BackupRepository.shouldCopyAttachmentToArchive(databaseAttachment.attachmentId, databaseAttachment.mmsId)) {
        Log.i(TAG, "[$attachmentId] The re-used file was not copied to the archive. Copying now.")
        AppDependencies.jobManager.add(CopyAttachmentToArchiveJob(attachmentId))
      }
      return
    } else if (databaseAttachment.uploadTimestamp > 0) {
      Log.i(TAG, "This file was previously-uploaded, but too long ago to be re-used. Age: $timeSinceUpload ms (${timeSinceUpload.milliseconds.inRoundedDays()} days)")
      if (databaseAttachment.archiveTransferState != AttachmentTable.ArchiveTransferState.NONE) {
        ZonaRosaDatabase.attachments.clearArchiveData(attachmentId)
      }
    }

    if (uploadSpec != null && System.currentTimeMillis() > uploadSpec!!.timeout) {
      Log.w(TAG, "Upload spec expired! Clearing.")
      uploadSpec = null
    }

    if (uploadSpec == null) {
      Log.d(TAG, "Need an upload spec. Fetching...")
      uploadSpec = ZonaRosaNetwork.attachments
        .getAttachmentV4UploadForm()
        .then { form ->
          ZonaRosaNetwork.attachments.getResumableUploadSpec(
            key = Base64.decode(databaseAttachment.remoteKey!!),
            iv = Util.getSecretBytes(16),
            uploadForm = form
          )
        }
        .successOrThrow()
        .toProto()
    } else {
      Log.d(TAG, "Re-using existing upload spec.")
    }

    Log.i(TAG, "Uploading attachment for message " + databaseAttachment.mmsId + " with ID " + databaseAttachment.attachmentId)
    try {
      getAttachmentNotificationIfNeeded(databaseAttachment).use { notification ->
        buildAttachmentStream(databaseAttachment, notification, uploadSpec!!).use { localAttachment ->
          val uploadResult: AttachmentUploadResult = ZonaRosaNetwork.attachments.uploadAttachmentV4(localAttachment).successOrThrow()
          ZonaRosaDatabase.attachments.finalizeAttachmentAfterUpload(databaseAttachment.attachmentId, uploadResult)
          if (ZonaRosaStore.backup.backsUpMedia) {
            val messageId = ZonaRosaDatabase.attachments.getMessageId(databaseAttachment.attachmentId)
            when {
              messageId == AttachmentTable.PREUPLOAD_MESSAGE_ID -> {
                Log.i(TAG, "[$attachmentId] Avoid uploading preuploaded attachments to archive. Skipping.")
              }
              ZonaRosaDatabase.messages.isStory(messageId) -> {
                Log.i(TAG, "[$attachmentId] Attachment is a story. Skipping.")
              }
              ZonaRosaDatabase.messages.isViewOnce(messageId) -> {
                Log.i(TAG, "[$attachmentId] Attachment is view-once. Skipping.")
              }
              ZonaRosaDatabase.messages.willMessageExpireBeforeCutoff(messageId) -> {
                Log.i(TAG, "[$attachmentId] Message will expire within 24hrs. Skipping.")
              }
              databaseAttachment.contentType == MediaUtil.LONG_TEXT -> {
                Log.i(TAG, "[$attachmentId] Long text attachment. Skipping.")
              }
              ZonaRosaStore.account.isLinkedDevice -> {
                Log.i(TAG, "[$attachmentId] Linked device. Skipping archive.")
              }
              else -> {
                Log.i(TAG, "[$attachmentId] Enqueuing job to copy to archive.")
                AppDependencies.jobManager.add(CopyAttachmentToArchiveJob(attachmentId))
              }
            }
          }
        }
      }
    } catch (e: StreamResetException) {
      val lastReset = ZonaRosaStore.misc.lastNetworkResetDueToStreamResets
      val now = System.currentTimeMillis()

      if (lastReset > now || lastReset + NETWORK_RESET_THRESHOLD > now) {
        Log.w(TAG, "Our existing connections is getting repeatedly denied by the server, reset network to establish new connections")
        AppDependencies.resetNetwork()
        AppDependencies.startNetwork()
        ZonaRosaStore.misc.lastNetworkResetDueToStreamResets = now
      } else {
        Log.i(TAG, "Stream reset during upload, not resetting network yet, last reset: $lastReset")
      }

      resetProgressListeners(databaseAttachment)

      throw e
    } catch (e: NonSuccessfulResumableUploadResponseCodeException) {
      if (e.code == 400) {
        Log.w(TAG, "Failed to upload due to a 400 when getting resumable upload information. Clearing upload spec.", e)
        uploadSpec = null
      }

      resetProgressListeners(databaseAttachment)

      throw e
    } catch (e: ResumeLocationInvalidException) {
      Log.w(TAG, "Resume location invalid. Clearing upload spec.", e)
      uploadSpec = null

      resetProgressListeners(databaseAttachment)

      throw e
    }
  }

  private fun getAttachmentNotificationIfNeeded(attachment: Attachment): AttachmentProgressService.Controller? {
    return if (attachment.size >= AttachmentUploadUtil.FOREGROUND_LIMIT_BYTES) {
      AttachmentProgressService.start(context, context.getString(R.string.AttachmentUploadJob_uploading_media))
    } else {
      null
    }
  }

  private fun resetProgressListeners(attachment: DatabaseAttachment) {
    EventBus.getDefault().postSticky(PartProgressEvent(attachment, PartProgressEvent.Type.NETWORK, 0, -1))
  }

  override fun onFailure() {
    val database = ZonaRosaDatabase.attachments
    val databaseAttachment = database.getAttachment(attachmentId)
    if (databaseAttachment == null) {
      Log.i(TAG, "Could not find attachment in DB for upload job upon failure/cancellation.")
      return
    }

    database.setTransferProgressFailed(attachmentId, databaseAttachment.mmsId)
  }

  override fun onShouldRetry(exception: Exception): Boolean {
    return exception is IOException && exception !is NotPushRegisteredException
  }

  @Throws(InvalidAttachmentException::class)
  private fun buildAttachmentStream(attachment: Attachment, notification: AttachmentProgressService.Controller?, resumableUploadSpec: ResumableUpload): ZonaRosaServiceAttachmentStream {
    if (attachment.uri == null || attachment.size == 0L) {
      throw InvalidAttachmentException(IOException("Outgoing attachment has no data!"))
    }

    return try {
      AttachmentUploadUtil.buildZonaRosaServiceAttachmentStream(
        context = context,
        attachment = attachment,
        uploadSpec = resumableUploadSpec,
        cancellationZonaRosa = { isCanceled },
        progressListener = object : ZonaRosaServiceAttachment.ProgressListener {
          override fun onAttachmentProgress(progress: AttachmentTransferProgress) {
            ZonaRosaExecutors.BOUNDED_IO.execute {
              EventBus.getDefault().postSticky(PartProgressEvent(attachment, PartProgressEvent.Type.NETWORK, progress))
              notification?.updateProgress(progress.value)
            }
          }

          override fun shouldCancel(): Boolean {
            return isCanceled
          }
        }
      )
    } catch (e: IOException) {
      throw InvalidAttachmentException(e)
    }
  }

  private inner class InvalidAttachmentException : Exception {
    constructor(message: String?) : super(message)
    constructor(e: Exception?) : super(e)
  }

  class Factory : Job.Factory<AttachmentUploadJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AttachmentUploadJob {
      val data = AttachmentUploadJobData.ADAPTER.decode(serializedData!!)
      return AttachmentUploadJob(
        parameters = parameters,
        attachmentId = AttachmentId(data.attachmentId),
        data.uploadSpec
      )
    }
  }
}

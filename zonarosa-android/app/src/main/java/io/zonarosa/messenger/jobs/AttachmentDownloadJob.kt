/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.jobs

import androidx.annotation.MainThread
import okio.Source
import okio.buffer
import org.greenrobot.eventbus.EventBus
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Hex
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.InvalidMacException
import io.zonarosa.libzonarosa.protocol.InvalidMessageException
import io.zonarosa.messenger.attachments.Attachment
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.attachments.Cdn
import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.attachments.InvalidAttachmentException
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.events.PartProgressEvent
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.Job.Parameters
import io.zonarosa.messenger.jobmanager.JobLogger.format
import io.zonarosa.messenger.jobmanager.JsonJobData
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobmanager.impl.NotInCallConstraint
import io.zonarosa.messenger.jobmanager.persistence.JobSpec
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.mms.MmsException
import io.zonarosa.messenger.notifications.v2.ConversationId.Companion.forConversation
import io.zonarosa.messenger.s3.S3
import io.zonarosa.messenger.transport.RetryLaterException
import io.zonarosa.messenger.util.AttachmentUtil
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.crypto.AttachmentCipherInputStream.IntegrityCheck
import io.zonarosa.service.api.messages.AttachmentTransferProgress
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentRemoteId
import io.zonarosa.service.api.push.exceptions.MissingConfigurationException
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException
import io.zonarosa.service.api.push.exceptions.PushNetworkException
import io.zonarosa.service.api.push.exceptions.RangeException
import java.io.File
import java.io.IOException
import java.util.Optional
import java.util.concurrent.TimeUnit

/**
 * Download attachment from locations as specified in their record.
 */
class AttachmentDownloadJob private constructor(
  parameters: Parameters,
  private val messageId: Long,
  private val attachmentId: AttachmentId,
  private val forceDownload: Boolean
) : BaseJob(parameters) {

  companion object {
    const val KEY = "AttachmentDownloadJob"
    private val TAG = Log.tag(AttachmentDownloadJob::class.java)

    private const val KEY_MESSAGE_ID = "message_id"
    private const val KEY_ATTACHMENT_ID = "part_row_id"
    private const val KEY_FORCE_DOWNLOAD = "part_manual"

    @JvmStatic
    fun constructQueueString(attachmentId: AttachmentId): String {
      return "AttachmentDownloadJob-" + attachmentId.id
    }

    fun jobSpecMatchesAttachmentId(jobSpec: JobSpec, attachmentId: AttachmentId): Boolean {
      if (KEY != jobSpec.factoryKey) {
        return false
      }

      val serializedData = jobSpec.serializedData ?: return false
      val data = JsonJobData.deserialize(serializedData)
      val parsed = AttachmentId(data.getLong(KEY_ATTACHMENT_ID))
      return attachmentId == parsed
    }

    @JvmStatic
    @MainThread
    fun downloadAttachmentIfNeeded(databaseAttachment: DatabaseAttachment): String? {
      return when (val transferState = databaseAttachment.transferState) {
        AttachmentTable.TRANSFER_PROGRESS_DONE -> null

        AttachmentTable.TRANSFER_RESTORE_IN_PROGRESS,
        AttachmentTable.TRANSFER_RESTORE_OFFLOADED,
        AttachmentTable.TRANSFER_NEEDS_RESTORE -> RestoreAttachmentJob.forManualRestore(databaseAttachment)

        AttachmentTable.TRANSFER_PROGRESS_PENDING,
        AttachmentTable.TRANSFER_PROGRESS_FAILED -> {
          if (ZonaRosaStore.backup.backsUpMedia && (databaseAttachment.remoteLocation == null || databaseAttachment.remoteDigest == null)) {
            if (databaseAttachment.dataHash != null) {
              Log.i(TAG, "Trying to restore attachment from archive cdn")
              RestoreAttachmentJob.forManualRestore(databaseAttachment)
            } else {
              Log.w(TAG, "No remote location and no plaintext hash. Can't download.")
              null
            }
          } else {
            val downloadJob = AttachmentDownloadJob(
              messageId = databaseAttachment.mmsId,
              attachmentId = databaseAttachment.attachmentId,
              forceDownload = true
            )
            AppDependencies.jobManager.add(downloadJob)
            downloadJob.id
          }
        }

        AttachmentTable.TRANSFER_PROGRESS_STARTED -> {
          Log.i(TAG, "${databaseAttachment.attachmentId} is in started state, enqueueing force download in case existing job is constraint-blocked")
          val downloadJob = AttachmentDownloadJob(
            messageId = databaseAttachment.mmsId,
            attachmentId = databaseAttachment.attachmentId,
            forceDownload = true
          )
          AppDependencies.jobManager.add(downloadJob)
          downloadJob.id
        }

        AttachmentTable.TRANSFER_PROGRESS_PERMANENT_FAILURE -> {
          Log.d(TAG, "${databaseAttachment.attachmentId} is permanently failed, transferState: $transferState")
          null
        }

        else -> {
          Log.w(TAG, "Attempted to download attachment with unknown transfer state: $transferState")
          null
        }
      }
    }
  }

  constructor(messageId: Long, attachmentId: AttachmentId, forceDownload: Boolean) : this(
    Parameters.Builder()
      .setQueue(constructQueueString(attachmentId))
      .addConstraint(NetworkConstraint.KEY)
      .maybeApplyNotInCallConstraint(forceDownload)
      .setLifespan(TimeUnit.DAYS.toMillis(1))
      .setMaxAttempts(Parameters.UNLIMITED)
      .setQueuePriority(if (forceDownload) Parameters.PRIORITY_HIGH else Parameters.PRIORITY_DEFAULT)
      .build(),
    messageId,
    attachmentId,
    forceDownload
  )

  override fun serialize(): ByteArray? {
    return JsonJobData.Builder()
      .putLong(KEY_MESSAGE_ID, messageId)
      .putLong(KEY_ATTACHMENT_ID, attachmentId.id)
      .putBoolean(KEY_FORCE_DOWNLOAD, forceDownload)
      .serialize()
  }

  override fun getFactoryKey(): String {
    return KEY
  }

  override fun onAdded() {
    Log.i(TAG, "onAdded() messageId: $messageId  attachmentId: $attachmentId  manual: $forceDownload")

    val attachment = ZonaRosaDatabase.attachments.getAttachment(attachmentId)
    val pending = attachment != null && attachment.transferState != AttachmentTable.TRANSFER_PROGRESS_DONE && attachment.transferState != AttachmentTable.TRANSFER_PROGRESS_PERMANENT_FAILURE

    if (pending && (forceDownload || AttachmentUtil.isAutoDownloadPermitted(context, attachment))) {
      Log.i(TAG, "onAdded() Marking attachment progress as 'started'")
      ZonaRosaDatabase.attachments.setTransferState(messageId, attachmentId, AttachmentTable.TRANSFER_PROGRESS_STARTED)
    }
  }

  @Throws(Exception::class)
  public override fun onRun() {
    doWork()

    if (!ZonaRosaDatabase.messages.isStory(messageId)) {
      AppDependencies.messageNotifier.updateNotification(context, forConversation(0))
    }
  }

  @Throws(IOException::class, RetryLaterException::class)
  fun doWork() {
    Log.i(TAG, "onRun() messageId: $messageId  attachmentId: $attachmentId  manual: $forceDownload")

    val attachment = ZonaRosaDatabase.attachments.getAttachment(attachmentId)

    if (attachment == null) {
      Log.w(TAG, "attachment no longer exists.")
      return
    }

    if (attachment.isPermanentlyFailed) {
      Log.w(TAG, "Attachment was marked as a permanent failure. Refusing to download.")
      return
    }

    if (!attachment.isInProgress) {
      Log.w(TAG, "Attachment was already downloaded.")
      return
    }

    if (!forceDownload && !AttachmentUtil.isAutoDownloadPermitted(context, attachment)) {
      Log.w(TAG, "Attachment can't be auto downloaded...")
      ZonaRosaDatabase.attachments.setTransferState(messageId, attachmentId, AttachmentTable.TRANSFER_PROGRESS_PENDING)
      return
    }

    if (ZonaRosaStore.backup.backsUpMedia && attachment.remoteLocation == null) {
      if (attachment.archiveTransferState != AttachmentTable.ArchiveTransferState.FINISHED) {
        throw InvalidAttachmentException("No remote location, and the archive transfer state is unfinished. Can't download.")
      }

      Log.i(TAG, "Trying to restore attachment from archive cdn instead")
      RestoreAttachmentJob.forManualRestore(attachment)

      return
    }

    Log.i(TAG, "Downloading push part $attachmentId")
    ZonaRosaDatabase.attachments.setTransferState(messageId, attachmentId, AttachmentTable.TRANSFER_PROGRESS_STARTED)

    when (attachment.cdn) {
      Cdn.S3 -> retrieveAttachmentForReleaseChannel(messageId, attachmentId, attachment)
      else -> retrieveAttachment(messageId, attachmentId, attachment)
    }

    if (ZonaRosaStore.backup.backsUpMedia) {
      val isStory = ZonaRosaDatabase.messages.isStory(messageId)
      when {
        attachment.archiveTransferState == AttachmentTable.ArchiveTransferState.FINISHED -> {
          Log.i(TAG, "[$attachmentId] Already archived. Skipping.")
        }

        attachment.cdn !in CopyAttachmentToArchiveJob.ALLOWED_SOURCE_CDNS -> {
          Log.i(TAG, "[$attachmentId] Attachment CDN doesn't support copying to archive. Re-uploading to archive.")
          AppDependencies.jobManager.add(UploadAttachmentToArchiveJob(attachmentId))
        }

        isStory -> {
          Log.i(TAG, "[$attachmentId] Attachment is a story. Skipping.")
        }

        ZonaRosaDatabase.messages.isViewOnce(messageId) -> {
          Log.i(TAG, "[$attachmentId] View-once. Skipping.")
        }

        ZonaRosaDatabase.messages.willMessageExpireBeforeCutoff(messageId) -> {
          Log.i(TAG, "[$attachmentId] Message will expire within 24hrs. Skipping.")
        }

        ZonaRosaStore.account.isLinkedDevice -> {
          Log.i(TAG, "[$attachmentId] Linked device. Skipping.")
        }

        else -> {
          Log.i(TAG, "[$attachmentId] Enqueuing job to copy to archive.")
          AppDependencies.jobManager.add(CopyAttachmentToArchiveJob(attachmentId))
        }
      }
    }
  }

  override fun onFailure() {
    Log.w(TAG, format(this, "onFailure() messageId: $messageId  attachmentId: $attachmentId  manual: $forceDownload"))

    markFailed(messageId, attachmentId)
  }

  override fun onShouldRetry(exception: Exception): Boolean {
    return exception is PushNetworkException ||
      exception is RetryLaterException
  }

  /**
   * @return True if the digest changed as part of downloading, otherwise false.
   */
  @Throws(IOException::class, RetryLaterException::class)
  private fun retrieveAttachment(
    messageId: Long,
    attachmentId: AttachmentId,
    attachment: DatabaseAttachment
  ) {
    val maxReceiveSize: Long = RemoteConfig.maxAttachmentReceiveSizeBytes
    val attachmentFile: File = ZonaRosaDatabase.attachments.getOrCreateTransferFile(attachmentId)

    try {
      if (attachment.size > maxReceiveSize) {
        throw MmsException("[$attachmentId] Attachment too large, failing download")
      }

      val pointer = createAttachmentPointer(attachment)

      val progressListener = object : ZonaRosaServiceAttachment.ProgressListener {
        override fun onAttachmentProgress(progress: AttachmentTransferProgress) {
          EventBus.getDefault().postSticky(PartProgressEvent(attachment, PartProgressEvent.Type.NETWORK, progress))
        }

        override fun shouldCancel(): Boolean {
          return this@AttachmentDownloadJob.isCanceled
        }
      }

      if (attachment.remoteDigest == null && attachment.dataHash == null) {
        Log.w(TAG, "[$attachmentId] Attachment has no integrity check!")
        throw InvalidAttachmentException("Attachment has no integrity check!")
      }

      val decryptingStream = AppDependencies
        .zonarosaServiceMessageReceiver
        .retrieveAttachment(
          pointer,
          attachmentFile,
          maxReceiveSize,
          IntegrityCheck.forEncryptedDigestAndPlaintextHash(attachment.remoteDigest, attachment.dataHash),
          progressListener
        )

      decryptingStream.use { input ->
        ZonaRosaDatabase.attachments.finalizeAttachmentAfterDownload(messageId, attachmentId, input)
      }
    } catch (e: RangeException) {
      Log.w(TAG, "[$attachmentId] Range exception, file size " + attachmentFile.length(), e)
      if (attachmentFile.delete()) {
        Log.i(TAG, "[$attachmentId] Deleted temp download file to recover")
        throw RetryLaterException(e)
      } else {
        throw IOException("[$attachmentId] Failed to delete temp download file following range exception")
      }
    } catch (e: InvalidAttachmentException) {
      Log.w(TAG, "[$attachmentId] Experienced exception while trying to download an attachment.", e)
      markFailed(messageId, attachmentId)
    } catch (e: NonSuccessfulResponseCodeException) {
      if (ZonaRosaStore.backup.backsUpMedia && e.code == 404 && attachment.archiveTransferState === AttachmentTable.ArchiveTransferState.FINISHED) {
        Log.i(TAG, "[$attachmentId] Retrying download from archive CDN")
        RestoreAttachmentJob.forManualRestore(attachment)
        return
      }

      Log.w(TAG, "[$attachmentId] Experienced exception while trying to download an attachment.", e)
      markFailed(messageId, attachmentId)
    } catch (e: MmsException) {
      Log.w(TAG, "[$attachmentId] Experienced exception while trying to download an attachment.", e)
      markFailed(messageId, attachmentId)
    } catch (e: MissingConfigurationException) {
      Log.w(TAG, "[$attachmentId] Experienced exception while trying to download an attachment.", e)
      markFailed(messageId, attachmentId)
    } catch (e: InvalidMessageException) {
      Log.w(TAG, "[$attachmentId] Experienced an InvalidMessageException while trying to download an attachment.", e)
      if (e.cause is InvalidMacException) {
        Log.w(TAG, "[$attachmentId] Detected an invalid mac. Treating as a permanent failure.")
        markPermanentlyFailed(messageId, attachmentId)
      } else {
        markFailed(messageId, attachmentId)
      }
    } catch (e: io.zonarosa.libzonarosa.protocol.incrementalmac.InvalidMacException) {
      Log.w(TAG, "[$attachmentId] Detected an invalid incremental mac. Clearing and marking as a temporary failure, requiring the user to manually try again.")
      ZonaRosaDatabase.attachments.clearIncrementalMacsForAttachmentAndAnyDuplicates(attachmentId, attachment.remoteKey, attachment.dataHash)
      markFailed(messageId, attachmentId)
    }

    attachmentFile.delete()
  }

  @Throws(InvalidAttachmentException::class)
  private fun createAttachmentPointer(attachment: DatabaseAttachment): ZonaRosaServiceAttachmentPointer {
    if (attachment.remoteKey.isNullOrEmpty()) {
      throw InvalidAttachmentException("empty encrypted key")
    }

    if (attachment.remoteLocation.isNullOrEmpty()) {
      throw InvalidAttachmentException("empty content id")
    }

    return try {
      val remoteId = ZonaRosaServiceAttachmentRemoteId.from(attachment.remoteLocation)
      val cdnNumber = attachment.cdn.cdnNumber

      val key = Base64.decode(attachment.remoteKey)

      if (attachment.remoteDigest != null) {
        Log.i(TAG, "Downloading attachment with digest: " + Hex.toString(attachment.remoteDigest))
      } else {
        throw InvalidAttachmentException("Null remote digest for $attachmentId")
      }

      ZonaRosaServiceAttachmentPointer(
        cdnNumber,
        remoteId,
        null,
        key,
        Optional.of(Util.toIntExact(attachment.size)),
        Optional.empty(),
        0,
        0,
        Optional.ofNullable(attachment.remoteDigest),
        Optional.ofNullable(attachment.getIncrementalDigest()),
        attachment.incrementalMacChunkSize,
        Optional.ofNullable(attachment.fileName),
        attachment.voiceNote,
        attachment.borderless,
        attachment.videoGif,
        Optional.empty(),
        Optional.ofNullable(attachment.blurHash).map { it.hash },
        attachment.uploadTimestamp,
        attachment.uuid
      )
    } catch (e: IOException) {
      Log.w(TAG, e)
      throw InvalidAttachmentException(e)
    } catch (e: ArithmeticException) {
      Log.w(TAG, e)
      throw InvalidAttachmentException(e)
    }
  }

  @Throws(IOException::class)
  private fun retrieveAttachmentForReleaseChannel(
    messageId: Long,
    attachmentId: AttachmentId,
    attachment: Attachment
  ) {
    try {
      S3.getObject(attachment.fileName!!).use { response ->
        val body = response.body
        if (body.contentLength() > RemoteConfig.maxAttachmentReceiveSizeBytes) {
          throw MmsException("Attachment too large, failing download")
        }

        ZonaRosaDatabase.attachments.createRemoteKeyIfNecessary(attachmentId)

        ZonaRosaDatabase.attachments.finalizeAttachmentAfterDownload(
          messageId,
          attachmentId,
          (body.source() as Source).buffer().inputStream()
        )
      }
    } catch (e: MmsException) {
      Log.w(TAG, "Experienced exception while trying to download an attachment.", e)
      markFailed(messageId, attachmentId)
    }
  }

  private fun markFailed(messageId: Long, attachmentId: AttachmentId) {
    ZonaRosaDatabase.attachments.setTransferProgressFailed(attachmentId, messageId)
  }

  private fun markPermanentlyFailed(messageId: Long, attachmentId: AttachmentId) {
    ZonaRosaDatabase.attachments.setTransferProgressPermanentFailure(attachmentId, messageId)
  }

  class Factory : Job.Factory<AttachmentDownloadJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AttachmentDownloadJob {
      val data = JsonJobData.deserialize(serializedData)
      return AttachmentDownloadJob(
        parameters = parameters,
        messageId = data.getLong(KEY_MESSAGE_ID),
        attachmentId = AttachmentId(data.getLong(KEY_ATTACHMENT_ID)),
        forceDownload = data.getBoolean(KEY_FORCE_DOWNLOAD)
      )
    }
  }
}

private fun Parameters.Builder.maybeApplyNotInCallConstraint(forceDownload: Boolean): Parameters.Builder {
  if (forceDownload) {
    return this
  }
  return this.addConstraint(NotInCallConstraint.KEY)
}

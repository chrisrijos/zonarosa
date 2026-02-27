/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import okio.IOException
import io.zonarosa.core.models.backup.MediaRootBackupKey
import io.zonarosa.core.util.PendingIntentFlags
import io.zonarosa.core.util.Stopwatch
import io.zonarosa.core.util.isNotNullOrBlank
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.logging.logW
import io.zonarosa.libzonarosa.messagebackup.BackupForwardSecrecyToken
import io.zonarosa.libzonarosa.net.SvrBStoreResponse
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException
import io.zonarosa.protos.resumableuploads.ResumableUpload
import io.zonarosa.messenger.R
import io.zonarosa.messenger.backup.ArchiveUploadProgress
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgress
import io.zonarosa.messenger.backup.v2.ArchiveValidator
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.backup.v2.ResumableMessagesBackupUploadSpec
import io.zonarosa.messenger.backup.v2.util.ArchiveAttachmentInfo
import io.zonarosa.messenger.backup.v2.util.getAllReferencedArchiveAttachmentInfos
import io.zonarosa.messenger.database.BackupMediaSnapshotTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.BackupMessagesConstraint
import io.zonarosa.messenger.jobs.protos.BackupMessagesJobData
import io.zonarosa.messenger.keyvalue.BackupValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.isDecisionPending
import io.zonarosa.messenger.logsubmit.SubmitDebugLogActivity
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.notifications.NotificationChannels
import io.zonarosa.messenger.notifications.NotificationIds
import io.zonarosa.messenger.providers.BlobProvider
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.MediaUtil
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.messages.AttachmentTransferProgress
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachment
import io.zonarosa.service.api.svr.SvrBApi
import io.zonarosa.service.internal.push.AttachmentUploadForm
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

/**
 * Job that is responsible for exporting the DB as a backup proto and
 * also uploading the resulting proto.
 */
class BackupMessagesJob private constructor(
  private var syncTime: Long,
  private var dataFile: String,
  private var resumableMessagesBackupUploadSpec: ResumableMessagesBackupUploadSpec?,
  parameters: Parameters
) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(BackupMessagesJob::class.java)
    private val FILE_REUSE_TIMEOUT = 1.hours
    private const val ATTACHMENT_SNAPSHOT_BUFFER_SIZE = 10_000
    private val TOO_LARGE_MESSAGE_CUTTOFF_DURATION = 365.days

    const val KEY = "BackupMessagesJob"

    private fun isBackupAllowed(): Boolean {
      return when {
        ZonaRosaStore.registration.restoreDecisionState.isDecisionPending -> {
          Log.i(TAG, "Backup not allowed: a restore decision is pending.", true)
          false
        }

        ArchiveRestoreProgress.state.activelyRestoring() -> {
          Log.i(TAG, "Backup not allowed: a restore is in progress.", true)
          false
        }

        ZonaRosaStore.account.isLinkedDevice -> {
          Log.i(TAG, "Backup not allowed: linked device.", true)
          false
        }

        else -> true
      }
    }

    fun enqueue() {
      if (!isBackupAllowed()) {
        Log.d(TAG, "Skip enqueueing BackupMessagesJob.", true)
        return
      }

      val jobManager = AppDependencies.jobManager

      val chain = jobManager.startChain(BackupMessagesJob())

      if (ZonaRosaStore.backup.optimizeStorage && ZonaRosaStore.backup.backsUpMedia) {
        chain.then(OptimizeMediaJob())
      }

      chain.enqueue()
    }

    fun cancel() {
      AppDependencies.jobManager.find { it.factoryKey == KEY }.forEach { AppDependencies.jobManager.cancel(it.id) }
    }
  }

  private var backupErrorHandled = false

  constructor() : this(
    syncTime = 0L,
    dataFile = "",
    resumableMessagesBackupUploadSpec = null,
    parameters = Parameters.Builder()
      .addConstraint(BackupMessagesConstraint.KEY)
      .setMaxAttempts(3)
      .setMaxInstancesForFactory(1)
      .build()
  )

  override fun serialize(): ByteArray = BackupMessagesJobData(
    syncTime = syncTime,
    dataFile = dataFile,
    resumableUri = resumableMessagesBackupUploadSpec?.resumableUri ?: "",
    uploadSpec = resumableMessagesBackupUploadSpec?.attachmentUploadForm?.toUploadSpec()
  ).encode()

  override fun getFactoryKey(): String = KEY

  override fun onAdded() {
    ArchiveUploadProgress.begin()
  }

  override fun onFailure() {
    if (!isCanceled && !backupErrorHandled) {
      Log.w(TAG, "Failed to backup user messages. Marking failure state.", true)
      BackupRepository.markBackupCreationFailed(BackupValues.BackupCreationError.TRANSIENT)
    }
  }

  override fun run(): Result {
    if (!isBackupAllowed()) {
      Log.d(TAG, "Skip running BackupMessagesJob.", true)
      return Result.success()
    }

    val stopwatch = Stopwatch("BackupMessagesJob")

    val auth = when (val result = BackupRepository.getSvrBAuth()) {
      is NetworkResult.Success -> result.result
      is NetworkResult.NetworkError -> return Result.retry(defaultBackoff()).logW(TAG, "Network error when getting SVRB auth.", result.getCause(), true)
      is NetworkResult.StatusCodeError -> {
        return when (result.code) {
          429 -> Result.retry(result.retryAfter()?.inWholeMilliseconds ?: defaultBackoff()).logW(TAG, "Rate limited when getting SVRB auth.", result.getCause(), true)
          else -> Result.retry(defaultBackoff()).logW(TAG, "Status code error when getting SVRB auth.", result.getCause(), true)
        }
      }
      is NetworkResult.ApplicationError -> throw result.throwable
    }

    if (ZonaRosaStore.backup.backupSecretRestoreRequired) {
      Log.i(TAG, "[svrb-restore] First backup of re-registered account without remote restore, read remote data if available to re-init")

      val forwardSecrecyMetadata: ByteArray? = when (val result = BackupRepository.getRemoteBackupForwardSecrecyMetadata()) {
        is NetworkResult.Success -> result.result
        is NetworkResult.NetworkError -> return Result.retry(defaultBackoff()).logW(TAG, "[svrb-restore] Network error getting remote forward secrecy metadata.", result.getCause(), true)
        is NetworkResult.StatusCodeError -> {
          if (result.code == 401 || result.code == 403 || result.code == 404) {
            Log.i(TAG, "[svrb-restore] No backup data found, continuing.", true)
            null
          } else {
            return when (result.code) {
              429 -> Result.retry(result.retryAfter()?.inWholeMilliseconds ?: defaultBackoff()).logW(TAG, "[svrb-restore] Rate limited when getting remote forward secrecy metadata.", result.getCause(), true)
              else -> Result.retry(defaultBackoff()).logW(TAG, "[svrb-restore] Status code error when getting remote forward secrecy metadata.", result.getCause(), true)
            }
          }
        }
        is NetworkResult.ApplicationError -> {
          if (result.getCause() is VerificationFailedException) {
            Log.w(TAG, "[svrb-restore] zkverification failed getting backup info, continuing.", true)
            null
          } else {
            throw result.throwable
          }
        }
      }

      if (forwardSecrecyMetadata != null) {
        when (val result = ZonaRosaNetwork.svrB.restore(auth, ZonaRosaStore.backup.messageBackupKey, forwardSecrecyMetadata)) {
          is SvrBApi.RestoreResult.Success -> {
            Log.i(TAG, "[svrb-restore] Remote secrecy data restored successfully.")
            ZonaRosaStore.backup.nextBackupSecretData = result.data.nextBackupSecretData
          }

          is SvrBApi.RestoreResult.NetworkError -> {
            Log.w(TAG, "[svrb-restore] Network error during SVRB.", result.exception)
            return Result.retry(defaultBackoff())
          }

          is SvrBApi.RestoreResult.RestoreFailedError,
          SvrBApi.RestoreResult.InvalidDataError -> {
            Log.i(TAG, "[svrb-restore] Permanent SVRB error! Continuing $result")
          }

          SvrBApi.RestoreResult.DataMissingError,
          is SvrBApi.RestoreResult.SvrError -> {
            Log.i(TAG, "[svrb-restore] Failed to fetch SVRB data, continuing: $result")
          }

          is SvrBApi.RestoreResult.UnknownError -> {
            Log.e(TAG, "[svrb-restore] Unknown SVRB result! Crashing.", result.throwable)
            return Result.fatalFailure(RuntimeException(result.throwable))
          }
        }
      }

      ZonaRosaStore.backup.backupSecretRestoreRequired = false
    }

    val backupSecretData = ZonaRosaStore.backup.nextBackupSecretData ?: run {
      Log.i(TAG, "First SVRB backup! Creating new backup chain.", true)
      val secretData = ZonaRosaNetwork.svrB.createNewBackupChain(auth, ZonaRosaStore.backup.messageBackupKey)
      ZonaRosaStore.backup.nextBackupSecretData = secretData
      secretData
    }

    val svrBMetadata: SvrBStoreResponse = when (val result = ZonaRosaNetwork.svrB.store(auth, ZonaRosaStore.backup.messageBackupKey, backupSecretData)) {
      is SvrBApi.StoreResult.Success -> result.data
      is SvrBApi.StoreResult.NetworkError -> return Result.retry(result.retryAfter?.inWholeMilliseconds ?: defaultBackoff()).logW(TAG, "SVRB transient network error.", result.exception, true)
      is SvrBApi.StoreResult.SvrError -> return Result.retry(defaultBackoff()).logW(TAG, "SVRB error.", result.throwable, true)
      SvrBApi.StoreResult.InvalidDataError -> {
        Log.w(TAG, "Invalid SVRB data on the server! Clearing backup secret data and retrying.", true)
        ZonaRosaStore.backup.nextBackupSecretData = null
        return Result.retry(defaultBackoff())
      }
      is SvrBApi.StoreResult.UnknownError -> return Result.fatalFailure(RuntimeException(result.throwable))
    }

    Log.i(TAG, "Successfully stored data on SVRB.", true)
    stopwatch.split("svrb")

    val createKeyResult = ZonaRosaDatabase.attachments.createRemoteKeyForAttachmentsThatNeedArchiveUpload()
    if (createKeyResult.totalCount > 0) {
      if (createKeyResult.unexpectedKeyCreation) {
        Log.w(TAG, "Unexpected remote key creation! $createKeyResult", true)
        maybePostRemoteKeyMissingNotification()
      } else {
        Log.d(TAG, "Needed to create ${createKeyResult.totalCount} remote keys for quotes/stickers.")
      }
    }
    stopwatch.split("keygen")

    ZonaRosaDatabase.attachments.clearIncrementalMacsForAttachmentsThatNeedArchiveUpload().takeIf { it > 0 }?.let { count -> Log.w(TAG, "Needed to clear $count incrementalMacs.", true) }
    stopwatch.split("clear-incmac")

    if (isCanceled) {
      return Result.failure()
    }

    val (tempBackupFile, currentTime, messageCutoffTime) = when (val generateBackupFileResult = getOrCreateBackupFile(stopwatch, svrBMetadata.forwardSecrecyToken, svrBMetadata.metadata)) {
      is BackupFileResult.Success -> generateBackupFileResult
      BackupFileResult.Failure -> return Result.failure()
      BackupFileResult.Retry -> return Result.retry(defaultBackoff())
    }

    ArchiveUploadProgress.onMessageBackupCreated(tempBackupFile.length())
    ZonaRosaStore.backup.lastBackupProtoVersion = BackupRepository.VERSION

    this.syncTime = currentTime
    this.dataFile = tempBackupFile.path

    val backupSpec: ResumableMessagesBackupUploadSpec = resumableMessagesBackupUploadSpec ?: when (val result = BackupRepository.getResumableMessagesBackupUploadSpec(tempBackupFile.length())) {
      is NetworkResult.Success -> {
        Log.i(TAG, "Successfully generated a new upload spec.", true)

        val spec = result.result
        resumableMessagesBackupUploadSpec = spec
        spec
      }

      is NetworkResult.NetworkError -> {
        Log.i(TAG, "Network failure", result.getCause(), true)
        return Result.retry(defaultBackoff())
      }

      is NetworkResult.StatusCodeError -> {
        when (result.code) {
          413 -> {
            Log.i(TAG, "Backup file is too large! Size: ${tempBackupFile.length()} bytes. Current threshold: ${ZonaRosaStore.backup.messageCuttoffDuration}", result.getCause(), true)
            tempBackupFile.delete()
            this.dataFile = ""
            BackupRepository.markBackupCreationFailed(BackupValues.BackupCreationError.BACKUP_FILE_TOO_LARGE)
            backupErrorHandled = true

            if (ZonaRosaStore.backup.messageCuttoffDuration == null) {
              Log.i(TAG, "Setting message cuttoff duration to $TOO_LARGE_MESSAGE_CUTTOFF_DURATION", true)
              ZonaRosaStore.backup.messageCuttoffDuration = TOO_LARGE_MESSAGE_CUTTOFF_DURATION
              return Result.retry(defaultBackoff())
            } else {
              return Result.failure()
            }
          }
          429 -> {
            Log.i(TAG, "Rate limited when getting upload spec.", result.getCause(), true)
            return Result.retry(result.retryAfter()?.inWholeMilliseconds ?: defaultBackoff())
          }
          else -> {
            Log.i(TAG, "Status code failure", result.getCause(), true)
            return Result.retry(defaultBackoff())
          }
        }
      }

      is NetworkResult.ApplicationError -> throw result.throwable
    }

    val progressListener = object : ZonaRosaServiceAttachment.ProgressListener {
      override fun onAttachmentProgress(progress: AttachmentTransferProgress) {
        ArchiveUploadProgress.onMessageBackupUploadProgress(progress)
      }

      override fun shouldCancel(): Boolean = isCanceled
    }

    FileInputStream(tempBackupFile).use { fileStream ->
      val uploadResult = ZonaRosaNetwork.archive.uploadBackupFile(
        uploadForm = backupSpec.attachmentUploadForm,
        resumableUploadUrl = backupSpec.resumableUri,
        data = fileStream,
        dataLength = tempBackupFile.length(),
        progressListener = progressListener
      )

      when (uploadResult) {
        is NetworkResult.Success -> {
          Log.i(TAG, "Successfully uploaded backup file.", true)
          if (!ZonaRosaStore.backup.hasBackupBeenUploaded) {
            Log.i(TAG, "First time making a backup - scheduling a storage sync.", true)
            ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
            StorageSyncHelper.scheduleSyncForDataChange()
          }
          ZonaRosaStore.backup.hasBackupBeenUploaded = true
        }

        is NetworkResult.NetworkError -> {
          Log.i(TAG, "Network failure", uploadResult.getCause(), true)
          return if (isCanceled) {
            Result.failure()
          } else {
            Result.retry(defaultBackoff())
          }
        }

        is NetworkResult.StatusCodeError -> {
          when (uploadResult.code) {
            400 -> {
              Log.w(TAG, "400 likely means bad resumable state. Resetting the upload spec before retrying.", true)
              resumableMessagesBackupUploadSpec = null
              return Result.retry(defaultBackoff())
            }
            429 -> {
              Log.w(TAG, "Rate limited when uploading backup file.", uploadResult.getCause(), true)
              return Result.retry(uploadResult.retryAfter()?.inWholeMilliseconds ?: defaultBackoff())
            }
            else -> {
              Log.i(TAG, "Status code failure (${uploadResult.code})", uploadResult.getCause(), true)
              return Result.retry(defaultBackoff())
            }
          }
        }

        is NetworkResult.ApplicationError -> throw uploadResult.throwable
      }
    }
    stopwatch.split("upload")

    ZonaRosaStore.backup.nextBackupSecretData = svrBMetadata.nextBackupSecretData

    ZonaRosaStore.backup.lastBackupProtoSize = tempBackupFile.length()
    if (!tempBackupFile.delete()) {
      Log.e(TAG, "Failed to delete temp backup file", true)
    }

    ZonaRosaStore.backup.lastBackupTime = System.currentTimeMillis()
    stopwatch.split("save-meta")
    stopwatch.stop(TAG)

    if (isCanceled) {
      return Result.failure()
    }

    if (ZonaRosaStore.backup.backsUpMedia && ZonaRosaDatabase.attachments.doAnyAttachmentsNeedArchiveUpload()) {
      Log.i(TAG, "Enqueuing attachment backfill job.", true)
      AppDependencies.jobManager.add(ArchiveAttachmentBackfillJob())
    } else {
      Log.i(TAG, "No attachments need to be uploaded, we can finish. Tier: ${ZonaRosaStore.backup.backupTier}", true)
      ArchiveUploadProgress.onMessageBackupFinishedEarly()
    }

    if (ZonaRosaStore.backup.backsUpMedia && ZonaRosaDatabase.attachments.doAnyThumbnailsNeedArchiveUpload()) {
      Log.i(TAG, "Enqueuing thumbnail backfill job.", true)
      AppDependencies.jobManager.add(ArchiveThumbnailBackfillJob())
    } else {
      Log.i(TAG, "No thumbnails need to be uploaded: ${ZonaRosaStore.backup.backupTier}", true)
    }

    ZonaRosaStore.backup.messageCuttoffDuration = null
    ZonaRosaStore.backup.lastUsedMessageCutoffTime = messageCutoffTime
    if (messageCutoffTime == 0L) {
      BackupRepository.clearBackupFailure()
    }
    ZonaRosaDatabase.backupMediaSnapshots.commitPendingRows()

    if (ZonaRosaStore.backup.backsUpMedia) {
      AppDependencies.jobManager.add(ArchiveCommitAttachmentDeletesJob())
      AppDependencies.jobManager.add(ArchiveAttachmentReconciliationJob())
    }

    return Result.success()
  }

  private fun getOrCreateBackupFile(
    stopwatch: Stopwatch,
    forwardSecrecyToken: BackupForwardSecrecyToken,
    forwardSecrecyMetadata: ByteArray
  ): BackupFileResult {
    if (System.currentTimeMillis() > syncTime && syncTime > 0L && dataFile.isNotNullOrBlank()) {
      val file = File(dataFile)
      val elapsed = (System.currentTimeMillis() - syncTime).milliseconds

      if (file.exists() && file.canRead() && elapsed < FILE_REUSE_TIMEOUT) {
        Log.d(TAG, "File exists and is new enough to utilize.", true)
        return BackupFileResult.Success(file, syncTime, messageInclusionCutoffTime = ZonaRosaStore.backup.lastUsedMessageCutoffTime)
      }
    }

    BlobProvider.getInstance().clearTemporaryBackupsDirectory(AppDependencies.application)

    val tempBackupFile = BlobProvider.getInstance().forTemporaryBackup(AppDependencies.application)

    val outputStream = FileOutputStream(tempBackupFile)
    val backupKey = ZonaRosaStore.backup.messageBackupKey
    val mediaRootBackupKey = ZonaRosaStore.backup.mediaRootBackupKey
    val currentTime = System.currentTimeMillis()

    val attachmentInfoBuffer: MutableSet<ArchiveAttachmentInfo> = mutableSetOf()
    val messageInclusionCutoffTime = ZonaRosaStore.backup.messageCuttoffDuration?.let { currentTime - it.inWholeMilliseconds } ?: 0

    try {
      BackupRepository.exportForZonaRosaBackup(
        outputStream = outputStream,
        messageBackupKey = backupKey,
        forwardSecrecyMetadata = forwardSecrecyMetadata,
        forwardSecrecyToken = forwardSecrecyToken,
        progressEmitter = ArchiveUploadProgress.ArchiveBackupProgressListener,
        append = { tempBackupFile.appendBytes(it) },
        cancellationZonaRosa = { this.isCanceled },
        currentTime = currentTime,
        messageInclusionCutoffTime = messageInclusionCutoffTime
      ) { frame ->
        attachmentInfoBuffer += frame.getAllReferencedArchiveAttachmentInfos()
        if (attachmentInfoBuffer.size > ATTACHMENT_SNAPSHOT_BUFFER_SIZE) {
          ZonaRosaDatabase.backupMediaSnapshots.writePendingMediaEntries(attachmentInfoBuffer.toFullSizeMediaEntries(mediaRootBackupKey))
          ZonaRosaDatabase.backupMediaSnapshots.writePendingMediaEntries(attachmentInfoBuffer.toThumbnailMediaEntries(mediaRootBackupKey))
          attachmentInfoBuffer.clear()
        }
      }
    } catch (e: IOException) {
      if (e.message?.contains("ENOSPC") == true) {
        Log.w(TAG, "Not enough space to make a backup!", e, true)
        tempBackupFile.delete()
        this.dataFile = ""
        BackupRepository.markBackupCreationFailed(BackupValues.BackupCreationError.NOT_ENOUGH_DISK_SPACE)
        return BackupFileResult.Failure
      }
    }

    if (attachmentInfoBuffer.isNotEmpty()) {
      ZonaRosaDatabase.backupMediaSnapshots.writePendingMediaEntries(attachmentInfoBuffer.toFullSizeMediaEntries(mediaRootBackupKey))
      ZonaRosaDatabase.backupMediaSnapshots.writePendingMediaEntries(attachmentInfoBuffer.toThumbnailMediaEntries(mediaRootBackupKey))
      attachmentInfoBuffer.clear()
    }

    if (isCanceled) {
      return BackupFileResult.Failure
    }

    stopwatch.split("export")

    when (val result = ArchiveValidator.validateZonaRosaBackup(tempBackupFile, backupKey, forwardSecrecyToken)) {
      ArchiveValidator.ValidationResult.Success -> {
        Log.d(TAG, "Successfully passed validation.", true)
      }

      is ArchiveValidator.ValidationResult.ReadError -> {
        Log.w(TAG, "Failed to read the file during validation!", result.exception, true)
        return BackupFileResult.Retry
      }

      is ArchiveValidator.ValidationResult.MessageValidationError -> {
        Log.w(TAG, "The backup file fails validation! Message: ${result.exception.message}, Details: ${result.messageDetails}", true)
        tempBackupFile.delete()
        this.dataFile = ""
        BackupRepository.markBackupCreationFailed(BackupValues.BackupCreationError.VALIDATION)
        backupErrorHandled = true
        return BackupFileResult.Failure
      }

      is ArchiveValidator.ValidationResult.RecipientDuplicateE164Error -> {
        Log.w(TAG, "The backup file fails validation with a duplicate recipient! Message: ${result.exception.message}, Details: ${result.details}", true)
        tempBackupFile.delete()
        this.dataFile = ""
        AppDependencies.jobManager.add(E164FormattingJob())
        BackupRepository.markBackupCreationFailed(BackupValues.BackupCreationError.VALIDATION)
        backupErrorHandled = true
        return BackupFileResult.Failure
      }
    }
    stopwatch.split("validate")

    if (isCanceled) {
      return BackupFileResult.Failure
    }

    return BackupFileResult.Success(tempBackupFile, currentTime, messageInclusionCutoffTime)
  }

  private fun AttachmentUploadForm.toUploadSpec(): ResumableUpload {
    return ResumableUpload(
      cdnNumber = cdn,
      cdnKey = key,
      location = signedUploadLocation,
      headers = headers.map { (key, value) -> ResumableUpload.Header(key, value) }
    )
  }

  private fun maybePostRemoteKeyMissingNotification() {
    if (!RemoteConfig.internalUser || !ZonaRosaStore.backup.backsUpMedia) {
      return
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
      return
    }

    val notification: Notification = NotificationCompat.Builder(context, NotificationChannels.getInstance().FAILURES)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle("[Internal-only] Unexpected remote key missing!")
      .setContentText("Tap to send a debug log")
      .setContentIntent(PendingIntent.getActivity(context, 0, Intent(context, SubmitDebugLogActivity::class.java), PendingIntentFlags.mutable()))
      .build()

    NotificationManagerCompat.from(context).notify(NotificationIds.INTERNAL_ERROR, notification)
  }

  private fun Set<ArchiveAttachmentInfo>.toFullSizeMediaEntries(mediaRootBackupKey: MediaRootBackupKey): Set<BackupMediaSnapshotTable.MediaEntry> {
    return this
      .map {
        BackupMediaSnapshotTable.MediaEntry(
          mediaId = it.fullSizeMediaName.toMediaId(mediaRootBackupKey).encode(),
          cdn = it.cdn,
          plaintextHash = it.plaintextHash.toByteArray(),
          remoteKey = it.remoteKey.toByteArray(),
          isThumbnail = false
        )
      }
      .toSet()
  }

  /**
   * Note: we have to remove permanently failed thumbnails here because there's no way we can know from the backup frame whether or not the thumbnail
   * failed permanently independently of the attachment itself. If the attachment itself fails permanently, it's not put in the backup, so we're covered
   * for full-size stuff.
   */
  private fun Set<ArchiveAttachmentInfo>.toThumbnailMediaEntries(mediaRootBackupKey: MediaRootBackupKey): Set<BackupMediaSnapshotTable.MediaEntry> {
    return this
      .asSequence()
      .filter { MediaUtil.isImageOrVideoType(it.contentType) }
      .filterNot { it.forQuote }
      .filterNot { it.isWallpaper }
      .map {
        BackupMediaSnapshotTable.MediaEntry(
          mediaId = it.thumbnailMediaName.toMediaId(mediaRootBackupKey).encode(),
          cdn = it.cdn,
          plaintextHash = it.plaintextHash.toByteArray(),
          remoteKey = it.remoteKey.toByteArray(),
          isThumbnail = true
        )
      }
      .toSet()
      .let { ZonaRosaDatabase.attachments.filterPermanentlyFailedThumbnails(it) }
  }

  class Factory : Job.Factory<BackupMessagesJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): BackupMessagesJob {
      val jobData = if (serializedData != null) {
        BackupMessagesJobData.ADAPTER.decode(serializedData)
      } else {
        BackupMessagesJobData()
      }

      return BackupMessagesJob(
        syncTime = jobData.syncTime,
        dataFile = jobData.dataFile,
        resumableMessagesBackupUploadSpec = uploadSpecFromJobData(jobData),
        parameters = parameters
      )
    }

    private fun uploadSpecFromJobData(backupMessagesJobData: BackupMessagesJobData): ResumableMessagesBackupUploadSpec? {
      if (backupMessagesJobData.resumableUri.isBlank() || backupMessagesJobData.uploadSpec == null) {
        return null
      }

      return ResumableMessagesBackupUploadSpec(
        resumableUri = backupMessagesJobData.resumableUri,
        attachmentUploadForm = AttachmentUploadForm(
          cdn = backupMessagesJobData.uploadSpec.cdnNumber,
          key = backupMessagesJobData.uploadSpec.cdnKey,
          headers = backupMessagesJobData.uploadSpec.headers.associate { it.key to it.value_ },
          signedUploadLocation = backupMessagesJobData.uploadSpec.location
        )
      )
    }
  }

  private sealed interface BackupFileResult {
    data class Success(
      val tempBackupFile: File,
      val currentTime: Long,
      val messageInclusionCutoffTime: Long
    ) : BackupFileResult

    data object Failure : BackupFileResult
    data object Retry : BackupFileResult
  }
}

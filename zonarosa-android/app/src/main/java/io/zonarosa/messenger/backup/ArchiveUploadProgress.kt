/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.bytes
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.throttleLatest
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.ArchiveCommitAttachmentDeletesJob
import io.zonarosa.messenger.jobs.ArchiveThumbnailUploadJob
import io.zonarosa.messenger.jobs.BackupMessagesJob
import io.zonarosa.messenger.jobs.UploadAttachmentToArchiveJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.protos.ArchiveUploadProgressState
import io.zonarosa.messenger.util.ZonaRosaLocalMetrics
import io.zonarosa.service.api.messages.AttachmentTransferProgress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Tracks the progress of uploading your message archive and provides an observable stream of results.
 */
object ArchiveUploadProgress {

  private val TAG = Log.tag(ArchiveUploadProgress::class)

  private val _progress: MutableSharedFlow<Unit> = MutableSharedFlow(replay = 1)

  private var uploadProgress: ArchiveUploadProgressState = ZonaRosaStore.backup.archiveUploadState ?: ArchiveUploadProgressState(
    state = ArchiveUploadProgressState.State.None
  )

  private val attachmentProgress: MutableMap<AttachmentId, AttachmentProgressDetails> = ConcurrentHashMap()

  private var debugAttachmentStartTime: Long = 0
  private val debugTotalAttachments: AtomicInteger = AtomicInteger(0)
  private val debugTotalBytes: AtomicLong = AtomicLong(0)

  /**
   * Observe this to get updates on the current upload progress.
   */
  val progress: Flow<ArchiveUploadProgressState> = _progress
    .throttleLatest(500.milliseconds) {
      uploadProgress.state == ArchiveUploadProgressState.State.None ||
        (uploadProgress.state == ArchiveUploadProgressState.State.UploadBackupFile && uploadProgress.backupFileUploadedBytes == 0L) ||
        (uploadProgress.state == ArchiveUploadProgressState.State.UploadMedia && uploadProgress.mediaUploadedBytes == 0L)
    }
    .map {
      if (uploadProgress.state != ArchiveUploadProgressState.State.UploadMedia) {
        return@map uploadProgress
      }

      if (!ZonaRosaStore.backup.backsUpMedia) {
        Log.i(TAG, "Doesn't upload media. Done!")
        ZonaRosaStore.backup.finishedInitialBackup = true
        return@map uploadProgress.copy(
          state = ArchiveUploadProgressState.State.None,
          backupPhase = ArchiveUploadProgressState.BackupPhase.BackupPhaseNone
        )
      }

      val pendingMediaUploadBytes = ZonaRosaDatabase.attachments.getPendingArchiveUploadBytes() - attachmentProgress.values.sumOf { it.bytesUploaded }
      if (pendingMediaUploadBytes <= 0) {
        Log.i(TAG, "No more pending bytes. Done!")
        Log.d(TAG, "Upload finished! " + buildDebugStats(debugAttachmentStartTime, debugTotalAttachments.get(), debugTotalBytes.get()))
        if (uploadProgress.mediaTotalBytes > 0) {
          Log.i(TAG, "We uploaded media as part of the backup. We should enqueue another backup now to ensure that CDN info is properly written.")
          BackupMessagesJob.enqueue()
        }
        ZonaRosaStore.backup.finishedInitialBackup = true
        return@map uploadProgress.copy(
          state = ArchiveUploadProgressState.State.None,
          backupPhase = ArchiveUploadProgressState.BackupPhase.BackupPhaseNone,
          mediaUploadedBytes = uploadProgress.mediaTotalBytes
        )
      }

      // It's possible that new attachments may be pending upload after we start a backup.
      // If we wanted the most accurate progress possible, we could maintain a new database flag that indicates whether an attachment has been flagged as part
      // of the current upload batch. However, this gets us pretty close while keeping things simple and not having to juggle extra flags, with the caveat that
      // the progress bar may occasionally be including media that is not actually referenced in the active backup file.
      val totalMediaUploadBytes = max(uploadProgress.mediaTotalBytes, pendingMediaUploadBytes)

      uploadProgress.copy(
        state = ArchiveUploadProgressState.State.UploadMedia,
        mediaUploadedBytes = totalMediaUploadBytes - pendingMediaUploadBytes,
        mediaTotalBytes = totalMediaUploadBytes
      )
    }
    .onEach { updated ->
      updateState(notify = false) { updated }
    }
    .onStart { emit(uploadProgress) }
    .flowOn(Dispatchers.IO)

  val inProgress
    get() = uploadProgress.state != ArchiveUploadProgressState.State.None && uploadProgress.state != ArchiveUploadProgressState.State.UserCanceled

  fun begin() {
    if (!ZonaRosaStore.backup.finishedInitialBackup) {
      ZonaRosaStore.backup.uploadBannerVisible = true
    }

    updateState(overrideCancel = true) {
      ArchiveUploadProgressState(
        state = ArchiveUploadProgressState.State.Export
      )
    }
  }

  fun triggerUpdate() {
    _progress.tryEmit(Unit)
  }

  fun cancel() {
    ZonaRosaStore.backup.uploadBannerVisible = false

    updateState {
      ArchiveUploadProgressState(
        state = ArchiveUploadProgressState.State.UserCanceled
      )
    }

    BackupMessagesJob.cancel()

    AppDependencies.jobManager.cancelAllInQueue(ArchiveCommitAttachmentDeletesJob.ARCHIVE_ATTACHMENT_QUEUE)
    AppDependencies.jobManager.cancelAllInQueues(UploadAttachmentToArchiveJob.QUEUES)
    AppDependencies.jobManager.cancelAllInQueue(ArchiveThumbnailUploadJob.KEY)
  }

  @WorkerThread
  suspend fun cancelAndBlock() {
    Log.d(TAG, "Canceling upload.")
    cancel()

    withContext(Dispatchers.IO) {
      Log.d(TAG, "Flushing job manager queue...")
      AppDependencies.jobManager.flush()

      val queues = UploadAttachmentToArchiveJob.QUEUES + ArchiveThumbnailUploadJob.QUEUES + ArchiveCommitAttachmentDeletesJob.ARCHIVE_ATTACHMENT_QUEUE
      Log.d(TAG, "Waiting for cancelations to occur...")
      while (!AppDependencies.jobManager.areQueuesEmpty(queues)) {
        delay(1.seconds)
      }
    }
  }

  fun onMessageBackupCreated(backupFileSize: Long) {
    updateState {
      it.copy(
        state = ArchiveUploadProgressState.State.UploadBackupFile,
        backupFileTotalBytes = backupFileSize,
        backupFileUploadedBytes = 0
      )
    }
  }

  fun onMessageBackupUploadProgress(progress: AttachmentTransferProgress) {
    updateState {
      it.copy(
        state = ArchiveUploadProgressState.State.UploadBackupFile,
        backupFileUploadedBytes = progress.transmitted.inWholeBytes,
        backupFileTotalBytes = progress.total.inWholeBytes
      )
    }
  }

  fun onAttachmentSectionStarted(totalAttachmentBytes: Long) {
    debugAttachmentStartTime = System.currentTimeMillis()
    attachmentProgress.clear()
    updateState {
      ArchiveUploadProgressState(
        state = ArchiveUploadProgressState.State.UploadMedia,
        mediaUploadedBytes = 0,
        mediaTotalBytes = totalAttachmentBytes
      )
    }
  }

  fun onAttachmentStarted(attachmentId: AttachmentId, sizeBytes: Long) {
    ZonaRosaLocalMetrics.ArchiveAttachmentUpload.start(attachmentId)
    attachmentProgress[attachmentId] = AttachmentProgressDetails(startTimeMs = System.currentTimeMillis(), totalBytes = sizeBytes)
    _progress.tryEmit(Unit)
  }

  fun onAttachmentProgress(attachmentId: AttachmentId, bytesUploaded: Long) {
    attachmentProgress.getOrPut(attachmentId) { AttachmentProgressDetails() }.bytesUploaded = bytesUploaded
    _progress.tryEmit(Unit)
  }

  fun onAttachmentFinished(attachmentId: AttachmentId) {
    ZonaRosaLocalMetrics.ArchiveAttachmentUpload.end(attachmentId)
    debugTotalAttachments.incrementAndGet()

    attachmentProgress[attachmentId]?.let {
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "Attachment uploaded: $it")
      }
      debugTotalBytes.addAndGet(it.totalBytes)
    }
    attachmentProgress.remove(attachmentId)
    _progress.tryEmit(Unit)
  }

  fun onMessageBackupFinishedEarly() {
    resetState()
    ZonaRosaStore.backup.finishedInitialBackup = true
  }

  fun onMainBackupFileUploadFailure() {
    resetState()
  }

  private fun resetState() {
    val shouldRevertToUploadMedia = ZonaRosaStore.backup.backsUpMedia && !AppDependencies.jobManager.areQueuesEmpty(UploadAttachmentToArchiveJob.QUEUES)
    if (shouldRevertToUploadMedia) {
      onAttachmentSectionStarted(ZonaRosaDatabase.attachments.getPendingArchiveUploadBytes())
    } else {
      updateState {
        it.copy(
          state = ArchiveUploadProgressState.State.None,
          backupPhase = ArchiveUploadProgressState.BackupPhase.BackupPhaseNone
        )
      }
    }
  }

  private fun updateState(
    notify: Boolean = true,
    overrideCancel: Boolean = false,
    transform: (ArchiveUploadProgressState) -> ArchiveUploadProgressState
  ) {
    val newState = transform(uploadProgress).let { state ->
      val oldArchiveState = uploadProgress.state
      if (oldArchiveState == ArchiveUploadProgressState.State.UserCanceled && !overrideCancel) {
        state.copy(state = ArchiveUploadProgressState.State.UserCanceled)
      } else {
        state
      }
    }

    if (uploadProgress == newState) {
      return
    }

    uploadProgress = newState
    ZonaRosaStore.backup.archiveUploadState = newState

    if (notify) {
      _progress.tryEmit(Unit)
    }
  }

  object ArchiveBackupProgressListener : BackupRepository.ExportProgressListener {
    override fun onAccount() {
      updatePhase(ArchiveUploadProgressState.BackupPhase.Account)
    }

    override fun onRecipient() {
      updatePhase(ArchiveUploadProgressState.BackupPhase.Recipient)
    }

    override fun onThread() {
      updatePhase(ArchiveUploadProgressState.BackupPhase.Thread)
    }

    override fun onCall() {
      updatePhase(ArchiveUploadProgressState.BackupPhase.Call)
    }

    override fun onSticker() {
      updatePhase(ArchiveUploadProgressState.BackupPhase.Sticker)
    }

    override fun onNotificationProfile() {
      updatePhase(ArchiveUploadProgressState.BackupPhase.NotificationProfile)
    }

    override fun onChatFolder() {
      updatePhase(ArchiveUploadProgressState.BackupPhase.ChatFolder)
    }

    override fun onMessage(currentProgress: Long, approximateCount: Long) {
      updatePhase(ArchiveUploadProgressState.BackupPhase.Message, currentProgress, approximateCount)
    }

    override fun onAttachment(currentProgress: Long, totalCount: Long) {
      updatePhase(ArchiveUploadProgressState.BackupPhase.BackupPhaseNone)
    }

    private fun updatePhase(
      phase: ArchiveUploadProgressState.BackupPhase,
      exportedFrames: Long = 0L,
      totalFrames: Long = 0L
    ) {
      updateState {
        ArchiveUploadProgressState(
          state = ArchiveUploadProgressState.State.Export,
          backupPhase = phase,
          frameExportCount = exportedFrames,
          frameTotalCount = totalFrames
        )
      }
    }
  }

  private fun buildDebugStats(startTimeMs: Long, totalAttachments: Int, totalBytes: Long): String {
    if (startTimeMs <= 0 || totalAttachments <= 0 || totalBytes <= 0) {
      return "Insufficient data to print debug stats."
    }

    val seconds: Double = (System.currentTimeMillis() - startTimeMs).milliseconds.toDouble(DurationUnit.SECONDS)
    val bytesPerSecond: Long = (totalBytes / seconds).toLong()

    return "TotalAttachments=$totalAttachments, TotalBytes=$totalBytes (${totalBytes.bytes.toUnitString()}), Rate=$bytesPerSecond bytes/sec (${bytesPerSecond.bytes.toUnitString()}/sec)"
  }

  private class AttachmentProgressDetails(
    val startTimeMs: Long = 0,
    val totalBytes: Long = 0,
    var bytesUploaded: Long = 0
  ) {
    override fun toString(): String {
      if (startTimeMs == 0L || totalBytes == 0L) {
        return "N/A"
      }

      val seconds: Double = (System.currentTimeMillis() - startTimeMs).milliseconds.toDouble(DurationUnit.SECONDS)
      val bytesPerSecond: Long = (totalBytes / seconds).toLong()

      return "Duration=${System.currentTimeMillis() - startTimeMs}ms, TotalBytes=$totalBytes (${totalBytes.bytes.toUnitString()}), Rate=$bytesPerSecond bytes/sec (${bytesPerSecond.bytes.toUnitString()}/sec)"
    }
  }
}

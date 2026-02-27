/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.attachments

import android.Manifest
import android.widget.CheckBox
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.reactivex.rxjava3.core.Completable
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.withContext
import io.zonarosa.core.ui.permissions.Permissions
import io.zonarosa.core.ui.util.StorageUtil
import io.zonarosa.core.ui.view.AlertDialogResult
import io.zonarosa.core.ui.view.awaitResult
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.orNull
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.ProgressCardDialogFragment
import io.zonarosa.messenger.components.ProgressCardDialogFragmentArgs
import io.zonarosa.messenger.database.MediaTable
import io.zonarosa.messenger.database.model.MmsMessageRecord
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.SaveAttachmentUtil
import io.zonarosa.messenger.util.SaveAttachmentUtil.SaveAttachment
import io.zonarosa.messenger.util.SaveAttachmentUtil.SaveAttachmentsResult
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Executes all of the steps needed to save message attachments to the device storage, including:
 * - Showing the save to storage warning/confirmation dialog.
 * - Requesting WRITE_EXTERNAL_STORAGE permission.
 * - Showing/dismissing media save progress.
 */
class AttachmentSaver(private val host: Host) {

  constructor(fragment: Fragment) : this(FragmentHost(fragment))

  companion object {
    private val TAG = Log.tag(AttachmentSaver::class)
    private const val PROGRESS_DIALOG_TAG = "AttachmentSaver_progress_dialog"
  }

  suspend fun saveAttachments(record: MmsMessageRecord) {
    val attachments = record.slideDeck.slides
      .filter { it.uri != null && (it.hasImage() || it.hasVideo() || it.hasAudio() || it.hasDocument()) }
      .map { SaveAttachment(it.uri!!, it.contentType, record.dateSent, it.fileName.orNull()) }
      .toSet()
    saveAttachments(attachments)
  }

  fun saveAttachmentsRx(attachments: Set<SaveAttachment>): Completable = rxCompletable { saveAttachments(attachments) }

  suspend fun saveAttachments(records: Collection<MediaTable.MediaRecord>) {
    val attachments = records.mapNotNull { record ->
      val uri = record.attachment?.uri
      val contentType = record.contentType
      if (uri != null && contentType != null) {
        SaveAttachment(uri, contentType, record.date, record.attachment.fileName)
      } else {
        null
      }
    }.toSet()
    saveAttachments(attachments)
  }

  fun saveAttachmentsRx(records: Collection<MediaTable.MediaRecord>): Completable = rxCompletable { saveAttachments(records) }
  suspend fun saveAttachments(attachments: Set<SaveAttachment>) {
    if (checkIsSaveWarningAccepted(attachmentCount = attachments.size) == SaveToStorageWarningResult.ACCEPTED) {
      if (checkCanWriteToMediaStore() == RequestPermissionResult.GRANTED) {
        Log.d(TAG, "Saving ${attachments.size} attachments to device storage.")
        saveToStorage(attachments)
      } else {
        Log.d(TAG, "Cancel saving ${attachments.size} attachments: media store permission denied.")
        host.showSaveResult(SaveAttachmentsResult.WriteStoragePermissionDenied)
      }
    } else {
      Log.d(TAG, "Cancel saving ${attachments.size} attachments: save to storage warning denied.")
    }
  }

  private suspend fun checkIsSaveWarningAccepted(attachmentCount: Int): SaveToStorageWarningResult {
    if (ZonaRosaStore.uiHints.hasDismissedSaveStorageWarning()) {
      return SaveToStorageWarningResult.ACCEPTED
    }
    return host.showSaveToStorageWarning(attachmentCount)
  }

  private suspend fun checkCanWriteToMediaStore(): RequestPermissionResult {
    if (StorageUtil.canWriteToMediaStore()) {
      return RequestPermissionResult.GRANTED
    }
    return host.requestWriteExternalStoragePermission()
  }

  private suspend fun saveToStorage(attachments: Set<SaveAttachment>): SaveAttachmentsResult {
    host.showSaveProgress(attachmentCount = attachments.size)
    return try {
      val result = SaveAttachmentUtil.saveAttachments(attachments)
      withContext(ZonaRosaDispatchers.Main) {
        host.showSaveResult(result)
      }
      result
    } finally {
      withContext(ZonaRosaDispatchers.Main) {
        host.dismissSaveProgress()
      }
    }
  }

  interface Host {
    suspend fun showSaveToStorageWarning(attachmentCount: Int): SaveToStorageWarningResult
    suspend fun requestWriteExternalStoragePermission(): RequestPermissionResult
    fun showSaveProgress(attachmentCount: Int)
    fun showSaveResult(result: SaveAttachmentsResult)
    fun dismissSaveProgress()
  }

  data class FragmentHost(private val fragment: Fragment) : Host {

    override fun showSaveResult(result: SaveAttachmentsResult) {
      Toast.makeText(fragment.requireContext(), result.getMessage(fragment.requireContext()), Toast.LENGTH_LONG).show()
    }

    override suspend fun showSaveToStorageWarning(attachmentCount: Int): SaveToStorageWarningResult = withContext(ZonaRosaDispatchers.Main) {
      val dialog = MaterialAlertDialogBuilder(fragment.requireContext())
        .setView(R.layout.dialog_save_attachment)
        .setTitle(R.string.AttachmentSaver__save_to_phone)
        .setCancelable(true)
        .setMessage(fragment.resources.getQuantityString(R.plurals.AttachmentSaver__this_media_will_be_saved, attachmentCount, attachmentCount))
        .create()

      val result = dialog.awaitResult(
        positiveButtonTextId = R.string.save,
        negativeButtonTextId = android.R.string.cancel
      )

      if (result == AlertDialogResult.POSITIVE) {
        val dontShowAgainCheckbox = dialog.findViewById<CheckBox>(R.id.checkbox)!!
        if (dontShowAgainCheckbox.isChecked) {
          ZonaRosaStore.uiHints.markDismissedSaveStorageWarning()
        }
        return@withContext SaveToStorageWarningResult.ACCEPTED
      }
      return@withContext SaveToStorageWarningResult.DENIED
    }

    override suspend fun requestWriteExternalStoragePermission(): RequestPermissionResult = withContext(ZonaRosaDispatchers.Main) {
      suspendCoroutine { continuation ->
        Permissions.with(fragment)
          .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
          .ifNecessary()
          .withPermanentDenialDialog(fragment.getString(R.string.AttachmentSaver__zonarosa_needs_the_storage_permission_in_order_to_write_to_external_storage_but_it_has_been_permanently_denied))
          .onAnyDenied {
            Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission request denied.")
            continuation.resume(RequestPermissionResult.DENIED)
          }
          .onAllGranted {
            Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission request granted.")
            continuation.resume(RequestPermissionResult.GRANTED)
          }
          .execute()
      }
    }

    override fun showSaveProgress(attachmentCount: Int) {
      val progressMessage = fragment.resources.getQuantityString(R.plurals.ConversationFragment_saving_n_attachments_to_sd_card, attachmentCount, attachmentCount)

      val dialog = ProgressCardDialogFragment.create().apply {
        arguments = ProgressCardDialogFragmentArgs.Builder(progressMessage).build().toBundle()
      }

      dialog.show(fragment.parentFragmentManager, PROGRESS_DIALOG_TAG)
    }

    override fun dismissSaveProgress() {
      val dialog = fragment.parentFragmentManager.findFragmentByTag(PROGRESS_DIALOG_TAG)
      (dialog as ProgressCardDialogFragment).dismissAllowingStateLoss()
    }
  }

  enum class SaveToStorageWarningResult {
    ACCEPTED,
    DENIED
  }

  enum class RequestPermissionResult {
    GRANTED,
    DENIED
  }
}

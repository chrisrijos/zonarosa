/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.internal.dataseeding

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.attachments.Attachment
import io.zonarosa.messenger.attachments.UriAttachment
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.ThreadRecord
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.messenger.sms.MessageSender
import io.zonarosa.messenger.util.MediaUtil

class DataSeedingPlaygroundViewModel(application: Application) : AndroidViewModel(application) {

  companion object {
    private val TAG = Log.tag(DataSeedingPlaygroundViewModel::class.java)
    private const val MAX_RECENT_THREADS = 10
  }

  private val _state = MutableStateFlow(DataSeedingPlaygroundState())
  val state: StateFlow<DataSeedingPlaygroundState> = _state.asStateFlow()

  fun loadThreads() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val threads = mutableListOf<ThreadRecord>()
        val cursor: Cursor = ZonaRosaDatabase.threads.getRecentConversationList(
          limit = MAX_RECENT_THREADS,
          includeInactiveGroups = false,
          hideV1Groups = true
        )

        cursor.use {
          val reader = ZonaRosaDatabase.threads.readerFor(it)
          var threadRecord = reader.getNext()
          while (threadRecord != null) {
            threads.add(threadRecord)
            threadRecord = reader.getNext()
          }
        }

        _state.value = _state.value.copy(threads = threads)
      } catch (e: Exception) {
        Log.w(TAG, "Failed to load threads", e)
      }
    }
  }

  fun selectFolder(uri: Uri) {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        val context = getApplication<Application>()
        val documentFile = DocumentFile.fromTreeUri(context, uri)

        if (documentFile != null && documentFile.isDirectory) {
          val mediaFiles = findMediaFiles(documentFile)

          _state.value = _state.value.copy(
            selectedFolderPath = documentFile.uri.toString(),
            mediaFiles = mediaFiles
          )
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to select folder", e)
      }
    }
  }

  fun toggleThreadSelection(threadId: Long, isSelected: Boolean) {
    val selectedThreads = _state.value.selectedThreads.toMutableSet()
    if (isSelected) {
      selectedThreads.add(threadId)
    } else {
      selectedThreads.remove(threadId)
    }

    _state.value = _state.value.copy(selectedThreads = selectedThreads)
  }

  @OptIn(DelicateCoroutinesApi::class)
  fun seedData(onComplete: () -> Unit, onError: (String) -> Unit) {
    GlobalScope.launch(Dispatchers.IO) {
      try {
        val context = getApplication<Application>()
        val currentState = _state.value

        if (currentState.mediaFiles.isEmpty()) {
          withContext(Dispatchers.Main) {
            onError("No media files selected")
          }
          return@launch
        }

        if (currentState.selectedThreads.isEmpty()) {
          withContext(Dispatchers.Main) {
            onError("No threads selected")
          }
          return@launch
        }

        val mediaFiles = currentState.mediaFiles
        val threadIds = currentState.selectedThreads.toList()
        var currentThreadIndex = 0

        for (mediaFile in mediaFiles) {
          val threadId = threadIds[currentThreadIndex % threadIds.size]
          sendMediaToThread(context, mediaFile, threadId)
          currentThreadIndex++
        }

        withContext(Dispatchers.Main) {
          onComplete()
        }
      } catch (e: Exception) {
        Log.w(TAG, "Failed to seed data", e)
        withContext(Dispatchers.Main) {
          onError(e.message ?: "Unknown error")
        }
      }
    }
  }

  private fun findMediaFiles(directory: DocumentFile): List<String> {
    val mediaFiles = mutableListOf<String>()

    directory.listFiles().forEach { file ->
      if (file.isFile && file.type != null) {
        val mimeType = file.type!!
        if (MediaUtil.isImageType(mimeType) || MediaUtil.isVideoType(mimeType)) {
          mediaFiles.add(file.name ?: "unknown")
        }
      }
    }

    return mediaFiles
  }

  private suspend fun sendMediaToThread(context: Context, mediaFileName: String, threadId: Long) {
    try {
      // Find the actual file URI
      val currentState = _state.value
      val documentFile = DocumentFile.fromTreeUri(context, Uri.parse(currentState.selectedFolderPath))

      if (documentFile != null) {
        val mediaFile = documentFile.listFiles().find { it.name == mediaFileName }

        if (mediaFile != null && mediaFile.uri != null) {
          val recipient = ZonaRosaDatabase.threads.getRecipientForThreadId(threadId)

          if (recipient != null) {
            val mimeType = mediaFile.type ?: MediaUtil.getCorrectedMimeType(mediaFileName)
            val attachment: Attachment = UriAttachment(
              uri = mediaFile.uri,
              contentType = mimeType,
              transferState = AttachmentTable.TRANSFER_PROGRESS_STARTED,
              size = mediaFile.length(),
              fileName = mediaFileName,
              voiceNote = false,
              borderless = false,
              videoGif = false,
              quote = false,
              quoteTargetContentType = null,
              caption = null,
              stickerLocator = null,
              blurHash = null,
              audioHash = null,
              transformProperties = null
            )

            val message = OutgoingMessage(
              threadRecipient = recipient,
              body = "",
              attachments = listOf(attachment),
              sentTimeMillis = System.currentTimeMillis(),
              isSecure = true
            )

            MessageSender.send(
              context,
              message,
              threadId,
              MessageSender.SendType.ZONAROSA,
              null,
              null
            )
          }
        }
      }
    } catch (e: Exception) {
      Log.w(TAG, "Failed to send media to thread $threadId", e)
    }
  }
}

data class DataSeedingPlaygroundState(
  val threads: List<ThreadRecord> = emptyList(),
  val selectedThreads: Set<Long> = emptySet(),
  val mediaFiles: List<String> = emptyList(),
  val selectedFolderPath: String = ""
)

/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger

import okio.IOException
import io.zonarosa.spinner.Plugin
import io.zonarosa.spinner.PluginResult
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.database.AttachmentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase

class AttachmentPlugin : Plugin {
  companion object {
    const val PATH = "/attachment"
  }

  override val name: String = "Attachment"
  override val path: String = PATH

  override fun get(parameters: Map<String, List<String>>): PluginResult {
    var errorContent = ""

    parameters["attachment_id"]?.firstOrNull()?.let { id ->
      val attachmentId = id.toLongOrNull()?.let { AttachmentId(it) }
      if (attachmentId != null) {
        try {
          val attachment = ZonaRosaDatabase.attachments.getAttachment(attachmentId)
          if (attachment != null) {
            val inputStream = if (attachment.transferState == AttachmentTable.TRANSFER_PROGRESS_DONE) {
              ZonaRosaDatabase.attachments.getAttachmentStream(attachmentId, 0)
            } else {
              ZonaRosaDatabase.attachments.getAttachmentThumbnailStream(attachmentId, 0)
            }
            return PluginResult.RawFileResult(attachment.size, inputStream, attachment.contentType ?: "application/octet-stream")
          } else {
            throw IOException("Missing attachment, not found for: $attachmentId")
          }
        } catch (e: IOException) {
          errorContent = "${e.javaClass}: ${e.message}"
        }
      }
    }

    val formContent = """
      <form action="$PATH" method="GET">
          <label for="number">Enter an attachment_id:</label>
          <input type="number" id="attachment_id" name="attachment_id" required>
          <button type="submit">Submit</button>
      </form>
    """.trimIndent()

    return PluginResult.RawHtmlResult("$formContent<br>$errorContent")
  }
}

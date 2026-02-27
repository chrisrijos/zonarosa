package io.zonarosa.messenger.mms

import android.content.ContentUris
import android.net.Uri
import io.zonarosa.messenger.attachments.AttachmentId

/**
 * Parses the given [Uri] into either an [AttachmentId] or a [Long]
 */
class PartUriParser(private val uri: Uri) {
  val partId: AttachmentId
    get() = AttachmentId(id)

  private val id: Long
    get() = ContentUris.parseId(uri)
}

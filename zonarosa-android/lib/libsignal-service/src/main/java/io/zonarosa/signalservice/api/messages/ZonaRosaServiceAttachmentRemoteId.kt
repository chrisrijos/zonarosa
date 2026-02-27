package io.zonarosa.service.api.messages

import io.zonarosa.service.api.InvalidMessageStructureException
import io.zonarosa.service.internal.push.AttachmentPointer

/**
 * Represents a zonarosa service attachment identifier. This can be either a CDN key or a long, but
 * not both at once. Attachments V2 used a long as an attachment identifier. This lacks sufficient
 * entropy to reduce the likelihood of any two uploads going to the same location within a 30-day
 * window. Attachments V4 (backwards compatible with V3) uses an opaque string as an attachment
 * identifier which provides more flexibility in the amount of entropy present.
 */
sealed interface ZonaRosaServiceAttachmentRemoteId {

  object S3 : ZonaRosaServiceAttachmentRemoteId {
    override fun toString() = ""
  }

  data class V2(val cdnId: Long) : ZonaRosaServiceAttachmentRemoteId {
    override fun toString() = cdnId.toString()
  }

  data class V4(val cdnKey: String) : ZonaRosaServiceAttachmentRemoteId {
    override fun toString() = cdnKey
  }

  data class Backup(val mediaCdnPath: String, val mediaId: String) : ZonaRosaServiceAttachmentRemoteId {
    override fun toString() = mediaId
  }

  companion object {

    @JvmStatic
    @Throws(InvalidMessageStructureException::class)
    fun from(attachmentPointer: AttachmentPointer): ZonaRosaServiceAttachmentRemoteId {
      return if (attachmentPointer.cdnKey != null) {
        V4(attachmentPointer.cdnKey)
      } else if (attachmentPointer.cdnId != null && attachmentPointer.cdnId > 0) {
        V2(attachmentPointer.cdnId)
      } else {
        throw InvalidMessageStructureException("AttachmentPointer CDN location not set")
      }
    }

    /**
     * Guesses that strings which contain values parseable to `long` should use an id-based
     * CDN path. Otherwise, use key-based CDN path.
     */
    @JvmStatic
    fun from(string: String): ZonaRosaServiceAttachmentRemoteId {
      return string.toLongOrNull()?.let { V2(it) } ?: V4(string)
    }
  }
}

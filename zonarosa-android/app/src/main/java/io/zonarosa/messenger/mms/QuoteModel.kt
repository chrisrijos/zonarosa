package io.zonarosa.messenger.mms

import io.zonarosa.messenger.attachments.Attachment
import io.zonarosa.messenger.database.model.Mention
import io.zonarosa.messenger.database.model.databaseprotos.BodyRangeList
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage
import io.zonarosa.service.internal.push.DataMessage

class QuoteModel(
  val id: Long,
  val author: RecipientId,
  val text: String,
  val isOriginalMissing: Boolean,
  val attachment: Attachment?,
  mentions: List<Mention>?,
  val type: Type,
  val bodyRanges: BodyRangeList?
) {
  val mentions: List<Mention>

  init {
    this.mentions = mentions ?: emptyList()
  }

  enum class Type(val code: Int, val dataMessageType: ZonaRosaServiceDataMessage.Quote.Type) {

    NORMAL(0, ZonaRosaServiceDataMessage.Quote.Type.NORMAL),
    GIFT_BADGE(1, ZonaRosaServiceDataMessage.Quote.Type.GIFT_BADGE),
    POLL(2, ZonaRosaServiceDataMessage.Quote.Type.POLL);

    companion object {
      @JvmStatic
      fun fromCode(code: Int): Type {
        for (value in entries) {
          if (value.code == code) {
            return value
          }
        }
        throw IllegalArgumentException("Invalid code: $code")
      }

      @JvmStatic
      fun fromDataMessageType(dataMessageType: ZonaRosaServiceDataMessage.Quote.Type): Type {
        for (value in entries) {
          if (value.dataMessageType === dataMessageType) {
            return value
          }
        }
        return NORMAL
      }

      fun fromProto(type: DataMessage.Quote.Type?): Type {
        return when (type) {
          DataMessage.Quote.Type.NORMAL -> NORMAL
          DataMessage.Quote.Type.GIFT_BADGE -> GIFT_BADGE
          DataMessage.Quote.Type.POLL -> POLL
          null -> NORMAL
        }
      }
    }
  }
}

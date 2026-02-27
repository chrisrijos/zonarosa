package io.zonarosa.messenger.conversation

import android.content.Context
import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.parcelize.Parcelize
import io.zonarosa.messenger.R

/**
 * The kinds of messages you can send, e.g. a plain ZonaRosa message, an SMS message, etc.
 */
@Parcelize
sealed class MessageSendType(
  @StringRes
  val titleRes: Int,
  @StringRes
  val composeHintRes: Int,
  @DrawableRes
  val buttonDrawableRes: Int,
  val transportType: TransportType,
  val maxBodyByteSize: Int
) : Parcelable {

  @get:JvmName("usesZonaRosaTransport")
  val usesZonaRosaTransport
    get() = transportType == TransportType.ZONAROSA

  open fun getTitle(context: Context): String {
    return context.getString(titleRes)
  }

  /**
   * A type representing a basic ZonaRosa message.
   */
  @Parcelize
  object ZonaRosaMessageSendType : MessageSendType(
    titleRes = R.string.ConversationActivity_send_message_content_description,
    composeHintRes = R.string.conversation_activity__type_message_push,
    buttonDrawableRes = R.drawable.ic_send_lock_24,
    transportType = TransportType.ZONAROSA,
    maxBodyByteSize = 2048
  )

  enum class TransportType {
    ZONAROSA,
    SMS
  }
}

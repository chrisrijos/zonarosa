package io.zonarosa.messenger.mediasend.v2

import android.net.Uri
import io.zonarosa.core.models.media.Media
import io.zonarosa.core.models.media.TransformProperties
import io.zonarosa.core.util.orNull
import java.util.Optional

object MediaBuilder {
  fun buildMedia(
    uri: Uri,
    mimeType: String = "",
    date: Long = 0L,
    width: Int = 0,
    height: Int = 0,
    size: Long = 0L,
    duration: Long = 0L,
    borderless: Boolean = false,
    videoGif: Boolean = false,
    bucketId: Optional<String> = Optional.empty(),
    caption: Optional<String> = Optional.empty(),
    transformProperties: Optional<TransformProperties> = Optional.empty(),
    fileName: Optional<String> = Optional.empty()
  ) = Media(uri, mimeType, date, width, height, size, duration, borderless, videoGif, bucketId.orNull(), caption.orNull(), transformProperties.orNull(), fileName.orNull())
}

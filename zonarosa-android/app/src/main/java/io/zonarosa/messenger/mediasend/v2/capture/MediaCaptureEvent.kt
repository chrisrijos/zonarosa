package io.zonarosa.messenger.mediasend.v2.capture

import io.zonarosa.core.models.media.Media
import io.zonarosa.messenger.recipients.Recipient

sealed interface MediaCaptureEvent {
  data class MediaCaptureRendered(val media: Media) : MediaCaptureEvent
  data class UsernameScannedFromQrCode(val recipient: Recipient, val username: String) : MediaCaptureEvent
  data object DeviceLinkScannedFromQrCode : MediaCaptureEvent
  data object MediaCaptureRenderFailed : MediaCaptureEvent
  data class ReregistrationScannedFromQrCode(val data: String) : MediaCaptureEvent
}

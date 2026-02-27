package io.zonarosa.messenger.components.settings.app.subscription.donate

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import io.zonarosa.messenger.database.InAppPaymentTable

@Parcelize
class InAppPaymentProcessorActionResult(
  val action: InAppPaymentProcessorAction,
  val inAppPaymentId: InAppPaymentTable.InAppPaymentId?,
  val status: Status
) : Parcelable {
  enum class Status {
    SUCCESS,
    FAILURE
  }
}

package io.zonarosa.messenger.badges.gifts

import android.content.Context
import io.zonarosa.core.util.Base64
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialPresentation
import io.zonarosa.messenger.R
import io.zonarosa.messenger.database.model.databaseprotos.GiftBadge
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.messenger.recipients.Recipient
import java.lang.Integer.min
import java.util.concurrent.TimeUnit

/**
 * Helper object for Gift badges
 */
object Gifts {

  /**
   * Request Code for getting token from Google Pay
   */
  const val GOOGLE_PAY_REQUEST_CODE = 3000

  /**
   * Creates an OutgoingSecureMediaMessage which contains the given gift badge.
   */
  fun createOutgoingGiftMessage(
    recipient: Recipient,
    giftBadge: GiftBadge,
    sentTimestamp: Long,
    expiresIn: Long
  ): OutgoingMessage {
    return OutgoingMessage(
      threadRecipient = recipient,
      body = Base64.encodeWithPadding(giftBadge.encode()),
      isSecure = true,
      sentTimeMillis = sentTimestamp,
      expiresIn = expiresIn,
      giftBadge = giftBadge
    )
  }

  /**
   * @return the expiration time from the redemption token, in UNIX epoch seconds.
   */
  private fun GiftBadge.getExpiry(): Long {
    return try {
      ReceiptCredentialPresentation(redemptionToken.toByteArray()).receiptExpirationTime
    } catch (e: InvalidInputException) {
      return 0L
    }
  }

  fun GiftBadge.formatExpiry(context: Context): String {
    val expiry = getExpiry()
    val timeRemaining = TimeUnit.SECONDS.toMillis(expiry) - System.currentTimeMillis()
    if (timeRemaining <= 0) {
      return context.getString(R.string.Gifts__expired)
    }

    val days = TimeUnit.MILLISECONDS.toDays(timeRemaining).toInt()
    if (days > 0) {
      return context.resources.getQuantityString(R.plurals.Gifts__d_days_remaining, days, days)
    }

    val hours = TimeUnit.MILLISECONDS.toHours(timeRemaining).toInt()
    if (hours > 0) {
      return context.resources.getQuantityString(R.plurals.Gifts__d_hours_remaining, hours, hours)
    }

    val minutes = min(1, TimeUnit.MILLISECONDS.toMinutes(timeRemaining).toInt())
    return context.resources.getQuantityString(R.plurals.Gifts__d_minutes_remaining, minutes, minutes)
  }
}

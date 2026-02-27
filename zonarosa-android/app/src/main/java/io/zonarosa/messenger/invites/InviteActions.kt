package io.zonarosa.messenger.invites

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.annotation.MainThread
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.CommunicationActions

/**
 * Handles 'invite to zonarosa' actions.
 */
object InviteActions {
  /**
   * Called to send a message to a user to invite them to ZonaRosa.
   * The invite can be sent in one of three ways:
   *
   * 1. If ZonaRosa is the user's default SMS app, we can simply append the message to the composer.
   * 2. If the user has an sms address, we generate a SENDTO intent and request it to be launched.
   * 3. Otherwise, we generate a share intent to allow the user to select how to send the invite.
   */
  @MainThread
  fun inviteUserToZonaRosa(
    context: Context,
    launchIntent: (Intent) -> Unit
  ) {
    val inviteText = context.getString(
      R.string.ConversationActivity_lets_switch_to_zonarosa,
      context.getString(R.string.install_url)
    )
    val intent = CommunicationActions.createIntentToShareTextViaShareSheet(inviteText)

    if (intent.resolveActivity(context.packageManager) != null) {
      launchIntent(Intent.createChooser(intent, context.getString(R.string.InviteActivity_invite_to_zonarosa)))
    } else {
      Toast.makeText(context, R.string.InviteActivity_no_app_to_share_to, Toast.LENGTH_LONG).show()
    }
  }
}

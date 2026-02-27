package io.zonarosa.messenger.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.ServiceUtil

/**
 * Respond to a PanicKit trigger Intent by locking the app.  PanicKit provides a
 * common framework for creating "panic button" apps that can trigger actions
 * in "panic responder" apps.  In this case, the response is to lock the app,
 * if it has been configured to do so via the ZonaRosa lock preference.
 */
class PanicResponderListener : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    val passwordEnabled = !ZonaRosaStore.settings.passphraseDisabled
    val keyguardSecure = ServiceUtil.getKeyguardManager(context).isKeyguardSecure
    val intentAction = intent.action
    if ((passwordEnabled || keyguardSecure) && "info.guardianproject.panic.action.TRIGGER" == intentAction) {
      val lockIntent = Intent(context, KeyCachingService::class.java)
      lockIntent.action = KeyCachingService.CLEAR_KEY_ACTION
      context.startService(lockIntent)
    }
  }
}

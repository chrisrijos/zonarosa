package io.zonarosa.messenger.push;

import android.content.Context;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.crypto.SecurityEvent;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;

public class SecurityEventListener implements ZonaRosaServiceMessageSender.EventListener {

  private static final String TAG = Log.tag(SecurityEventListener.class);

  private final Context context;

  public SecurityEventListener(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public void onSecurityEvent(ZonaRosaServiceAddress textSecureAddress) {
    SecurityEvent.broadcastSecurityUpdateEvent(context);
  }
}

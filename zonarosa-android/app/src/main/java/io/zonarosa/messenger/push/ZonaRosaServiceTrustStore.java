package io.zonarosa.messenger.push;

import android.content.Context;

import io.zonarosa.messenger.R;
import io.zonarosa.service.api.push.TrustStore;

import java.io.InputStream;

public class ZonaRosaServiceTrustStore implements TrustStore {

  private final Context context;

  public ZonaRosaServiceTrustStore(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public InputStream getKeyStoreInputStream() {
    return context.getResources().openRawResource(R.raw.zonarosa);
  }

  @Override
  public String getKeyStorePassword() {
    return "zonarosa";
  }
}

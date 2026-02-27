package io.zonarosa.messenger.push;


import android.content.Context;

import io.zonarosa.messenger.R;
import io.zonarosa.service.api.push.TrustStore;

import java.io.InputStream;

public class DomainFrontingTrustStore implements TrustStore {

  private final Context context;

  public DomainFrontingTrustStore(Context context) {
    this.context = context.getApplicationContext();
  }

  @Override
  public InputStream getKeyStoreInputStream() {
    return context.getResources().openRawResource(R.raw.censorship_fronting);
  }

  @Override
  public String getKeyStorePassword() {
    return "zonarosa";
  }

}

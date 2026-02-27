package io.zonarosa.messenger.deeplinks;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import io.zonarosa.messenger.MainActivity;
import io.zonarosa.messenger.PassphraseRequiredActivity;

public class DeepLinkEntryActivity extends PassphraseRequiredActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState, boolean ready) {
    Intent intent = MainActivity.clearTop(this);
    Uri    data   = getIntent().getData();
    intent.setData(data);
    startActivity(intent);
  }
}

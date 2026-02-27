package io.zonarosa.messenger.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.crypto.KeyStoreHelper;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

/**
 * Allows the getting and setting of the backup passphrase, which is stored encrypted on API >= 23.
 */
public final class BackupPassphrase {

  private BackupPassphrase() {
  }

  private static final String TAG = Log.tag(BackupPassphrase.class);

  public static @Nullable String get(@NonNull Context context) {
    String passphrase          = ZonaRosaPreferences.getBackupPassphrase(context);
    String encryptedPassphrase = ZonaRosaPreferences.getEncryptedBackupPassphrase(context);

    if (passphrase == null && encryptedPassphrase == null) {
      return null;
    }

    if (encryptedPassphrase == null) {
      Log.i(TAG, "Migrating to encrypted passphrase.");
      set(context, passphrase);
      encryptedPassphrase = ZonaRosaPreferences.getEncryptedBackupPassphrase(context);
      if (encryptedPassphrase == null) throw new AssertionError("Passphrase migration failed");
    }

    KeyStoreHelper.SealedData data = KeyStoreHelper.SealedData.fromString(encryptedPassphrase);
    return stripSpaces(new String(KeyStoreHelper.unseal(data)));
  }

  public static void set(@NonNull Context context, @Nullable String passphrase) {
    if (passphrase == null) {
      ZonaRosaPreferences.setBackupPassphrase(context, null);
      ZonaRosaPreferences.setEncryptedBackupPassphrase(context, null);
    } else {
      KeyStoreHelper.SealedData encryptedPassphrase = KeyStoreHelper.seal(passphrase.getBytes());
      ZonaRosaPreferences.setEncryptedBackupPassphrase(context, encryptedPassphrase.serialize());
      ZonaRosaPreferences.setBackupPassphrase(context, null);
    }
  }

  private static String stripSpaces(@Nullable String passphrase) {
    return passphrase != null ? passphrase.replace(" ", "") : null;
  }
}

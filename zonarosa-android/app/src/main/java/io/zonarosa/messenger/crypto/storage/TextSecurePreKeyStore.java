package io.zonarosa.messenger.crypto.storage;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyStore;
import io.zonarosa.messenger.crypto.ReentrantSessionLock;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.service.api.ZonaRosaServicePreKeyStore;
import io.zonarosa.service.api.ZonaRosaSessionLock;
import io.zonarosa.core.models.ServiceId;

import java.util.List;

public class ZonaRosaPreKeyStore implements ZonaRosaServicePreKeyStore, SignedPreKeyStore {

  @SuppressWarnings("unused")
  private static final String TAG = Log.tag(ZonaRosaPreKeyStore.class);

  @NonNull
  private final ServiceId accountId;

  public ZonaRosaPreKeyStore(@NonNull ServiceId accountId) {
    this.accountId = accountId;
  }

  @Override
  public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      PreKeyRecord preKeyRecord = ZonaRosaDatabase.oneTimePreKeys().get(accountId, preKeyId);

      if (preKeyRecord == null) throw new InvalidKeyIdException("No such key: " + preKeyId);
      else                      return preKeyRecord;
    }
  }

  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      SignedPreKeyRecord signedPreKeyRecord = ZonaRosaDatabase.signedPreKeys().get(accountId, signedPreKeyId);

      if (signedPreKeyRecord == null) throw new InvalidKeyIdException("No such signed prekey: " + signedPreKeyId);
      else                            return signedPreKeyRecord;
    }
  }

  @Override
  public List<SignedPreKeyRecord> loadSignedPreKeys() {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      return ZonaRosaDatabase.signedPreKeys().getAll(accountId);
    }
  }

  @Override
  public void storePreKey(int preKeyId, PreKeyRecord record) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.oneTimePreKeys().insert(accountId, preKeyId, record);
    }
  }

  @Override
  public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
    try (ZonaRosaSessionLock.Lock unused = ReentrantSessionLock.INSTANCE.acquire()) {
      ZonaRosaDatabase.signedPreKeys().insert(accountId, signedPreKeyId, record);
    }
  }

  @Override
  public boolean containsPreKey(int preKeyId) {
    return ZonaRosaDatabase.oneTimePreKeys().get(accountId, preKeyId) != null;
  }

  @Override
  public boolean containsSignedPreKey(int signedPreKeyId) {
    return ZonaRosaDatabase.signedPreKeys().get(accountId, signedPreKeyId) != null;
  }

  @Override
  public void removePreKey(int preKeyId) {
    ZonaRosaDatabase.oneTimePreKeys().delete(accountId, preKeyId);
  }

  @Override
  public void removeSignedPreKey(int signedPreKeyId) {
    ZonaRosaDatabase.signedPreKeys().delete(accountId, signedPreKeyId);
  }

  @Override
  public void markAllOneTimeEcPreKeysStaleIfNecessary(long staleTime) {
    ZonaRosaDatabase.oneTimePreKeys().markAllStaleIfNecessary(accountId, staleTime);
  }

  @Override
  public void deleteAllStaleOneTimeEcPreKeys(long threshold, int minCount) {
    ZonaRosaDatabase.oneTimePreKeys().deleteAllStaleBefore(accountId, threshold, minCount);
  }
}

package io.zonarosa.service.api.storage;

import org.junit.Test;
import io.zonarosa.core.models.storageservice.StorageItemKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.service.internal.util.Util;

import static org.junit.Assert.assertArrayEquals;

public class ZonaRosaStorageCipherTest {

  @Test
  public void symmetry() throws InvalidKeyException {
    StorageItemKey key  = new StorageItemKey(Util.getSecretBytes(32));
    byte[]         data = Util.getSecretBytes(1337);

    byte[] ciphertext = ZonaRosaStorageCipher.encrypt(key, data);
    byte[] plaintext  = ZonaRosaStorageCipher.decrypt(key, ciphertext);

    assertArrayEquals(data, plaintext);
  }

  @Test(expected = InvalidKeyException.class)
  public void badKeyOnDecrypt() throws InvalidKeyException {
    StorageItemKey key  = new StorageItemKey(Util.getSecretBytes(32));
    byte[]         data = Util.getSecretBytes(1337);

    byte[] badKey = key.serialize().clone();
    badKey[0] += 1;

    byte[] ciphertext = ZonaRosaStorageCipher.encrypt(key, data);
    byte[] plaintext  = ZonaRosaStorageCipher.decrypt(new StorageItemKey(badKey), ciphertext);
  }
}

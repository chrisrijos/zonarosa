package io.zonarosa.service.api.crypto;

import io.zonarosa.libzonarosa.protocol.DuplicateMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.groups.GroupCipher;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;
import io.zonarosa.service.api.ZonaRosaSessionLock;

import java.util.UUID;

/**
 * A thread-safe wrapper around {@link GroupCipher}.
 */
public class ZonaRosaGroupCipher {

  private final ZonaRosaSessionLock lock;
  private final GroupCipher       cipher;

  public ZonaRosaGroupCipher(ZonaRosaSessionLock lock, GroupCipher cipher) {
    this.lock   = lock;
    this.cipher = cipher;
  }

  public CiphertextMessage encrypt(UUID distributionId, byte[] paddedPlaintext) throws NoSessionException {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.encrypt(distributionId, paddedPlaintext);
    }
  }

  public byte[] decrypt(byte[] senderKeyMessageBytes)
      throws LegacyMessageException, DuplicateMessageException, InvalidMessageException, NoSessionException
  {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.decrypt(senderKeyMessageBytes);
    }
  }
}

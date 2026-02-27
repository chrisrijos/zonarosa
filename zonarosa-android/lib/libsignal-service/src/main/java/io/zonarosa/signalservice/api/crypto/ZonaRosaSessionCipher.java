package io.zonarosa.service.api.crypto;

import io.zonarosa.libzonarosa.protocol.DuplicateMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidKeyIdException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidVersionException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.SessionCipher;
import io.zonarosa.libzonarosa.protocol.UntrustedIdentityException;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;
import io.zonarosa.libzonarosa.protocol.message.PreKeyZonaRosaMessage;
import io.zonarosa.libzonarosa.protocol.message.ZonaRosaMessage;
import io.zonarosa.service.api.ZonaRosaSessionLock;

/**
 * A thread-safe wrapper around {@link SessionCipher}.
 */
public class ZonaRosaSessionCipher {

  private final ZonaRosaSessionLock lock;
  private final SessionCipher     cipher;

  public ZonaRosaSessionCipher(ZonaRosaSessionLock lock, SessionCipher cipher) {
    this.lock   = lock;
    this.cipher = cipher;
  }

  public CiphertextMessage encrypt(byte[] paddedMessage) throws io.zonarosa.libzonarosa.protocol.UntrustedIdentityException, NoSessionException {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.encrypt(paddedMessage);
    }
  }

  public byte[] decrypt(PreKeyZonaRosaMessage ciphertext) throws DuplicateMessageException, LegacyMessageException, InvalidMessageException, InvalidKeyIdException, InvalidKeyException, io.zonarosa.libzonarosa.protocol.UntrustedIdentityException {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.decrypt(ciphertext);
    }
  }

  public byte[] decrypt(ZonaRosaMessage ciphertext) throws InvalidMessageException, InvalidVersionException, DuplicateMessageException, LegacyMessageException, NoSessionException, UntrustedIdentityException {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.decrypt(ciphertext);
    }
  }

  public int getRemoteRegistrationId() {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.getRemoteRegistrationId();
    }
  }

  public int getSessionVersion() {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.getSessionVersion();
    }
  }
}

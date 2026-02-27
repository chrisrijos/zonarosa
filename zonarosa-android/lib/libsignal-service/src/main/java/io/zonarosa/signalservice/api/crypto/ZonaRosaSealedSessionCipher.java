package io.zonarosa.service.api.crypto;

import io.zonarosa.libzonarosa.metadata.InvalidMetadataMessageException;
import io.zonarosa.libzonarosa.metadata.InvalidMetadataVersionException;
import io.zonarosa.libzonarosa.metadata.ProtocolDuplicateMessageException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidKeyException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidKeyIdException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidMessageException;
import io.zonarosa.libzonarosa.metadata.ProtocolInvalidVersionException;
import io.zonarosa.libzonarosa.metadata.ProtocolLegacyMessageException;
import io.zonarosa.libzonarosa.metadata.ProtocolNoSessionException;
import io.zonarosa.libzonarosa.metadata.ProtocolUntrustedIdentityException;
import io.zonarosa.libzonarosa.metadata.SealedSessionCipher;
import io.zonarosa.libzonarosa.metadata.SelfSendException;
import io.zonarosa.libzonarosa.metadata.certificate.CertificateValidator;
import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.InvalidRegistrationIdException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.UntrustedIdentityException;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;
import io.zonarosa.service.api.ZonaRosaSessionLock;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A thread-safe wrapper around {@link SealedSessionCipher}.
 */
public class ZonaRosaSealedSessionCipher {

  private final ZonaRosaSessionLock   lock;
  private final SealedSessionCipher cipher;

  public ZonaRosaSealedSessionCipher(ZonaRosaSessionLock lock, SealedSessionCipher cipher) {
    this.lock   = lock;
    this.cipher = cipher;
  }

  public byte[] encrypt(ZonaRosaProtocolAddress destinationAddress, UnidentifiedSenderMessageContent content)
      throws InvalidKeyException, UntrustedIdentityException
  {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.encrypt(destinationAddress, content);
    }
  }

  public byte[] multiRecipientEncrypt(List<ZonaRosaProtocolAddress> recipients, Map<ZonaRosaProtocolAddress, SessionRecord> sessionMap, UnidentifiedSenderMessageContent content)
      throws InvalidKeyException, UntrustedIdentityException, NoSessionException, InvalidRegistrationIdException
  {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      List<SessionRecord> recipientSessions = recipients.stream().map(sessionMap::get).collect(Collectors.toList());

      if (recipientSessions.contains(null)) {
        throw new NoSessionException("No session for some recipients");
      }

      return cipher.multiRecipientEncrypt(recipients, recipientSessions, content);
    }
  }

  public SealedSessionCipher.DecryptionResult decrypt(CertificateValidator validator, byte[] ciphertext, long timestamp) throws InvalidMetadataMessageException, InvalidMetadataVersionException, ProtocolInvalidMessageException, ProtocolInvalidKeyException, ProtocolNoSessionException, ProtocolLegacyMessageException, ProtocolInvalidVersionException, ProtocolDuplicateMessageException, ProtocolInvalidKeyIdException, ProtocolUntrustedIdentityException, SelfSendException {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.decrypt(validator, ciphertext, timestamp);
    }
  }

  public int getSessionVersion(ZonaRosaProtocolAddress remoteAddress) {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.getSessionVersion(remoteAddress);
    }
  }

  public int getRemoteRegistrationId(ZonaRosaProtocolAddress remoteAddress) {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return cipher.getRemoteRegistrationId(remoteAddress);
    }
  }
}

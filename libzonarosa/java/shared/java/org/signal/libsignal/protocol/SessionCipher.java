//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;
import io.zonarosa.libzonarosa.protocol.message.PreKeyZonaRosaMessage;
import io.zonarosa.libzonarosa.protocol.message.ZonaRosaMessage;
import io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore;
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyStore;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.PreKeyStore;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.SessionStore;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyStore;

/**
 * The main entry point for ZonaRosa Protocol encrypt/decrypt operations.
 *
 * <p>Once a session has been established with {@link SessionBuilder}, this class can be used for
 * all encrypt/decrypt operations within that session.
 *
 * <p>This class is not thread-safe.
 *
 * @author Moxie Marlinspike
 */
public class SessionCipher {

  private final SessionStore sessionStore;
  private final IdentityKeyStore identityKeyStore;
  private final PreKeyStore preKeyStore;
  private final SignedPreKeyStore signedPreKeyStore;
  private final KyberPreKeyStore kyberPreKeyStore;
  private final ZonaRosaProtocolAddress remoteAddress;

  /**
   * Construct a SessionCipher for encrypt/decrypt operations on a session. In order to use
   * SessionCipher, a session must have already been created and stored using {@link
   * SessionBuilder}.
   *
   * @param sessionStore The {@link SessionStore} that contains a session for this recipient.
   * @param remoteAddress The remote address that messages will be encrypted to or decrypted from.
   */
  public SessionCipher(
      SessionStore sessionStore,
      PreKeyStore preKeyStore,
      SignedPreKeyStore signedPreKeyStore,
      KyberPreKeyStore kyberPreKeyStore,
      IdentityKeyStore identityKeyStore,
      ZonaRosaProtocolAddress remoteAddress) {
    this.sessionStore = sessionStore;
    this.preKeyStore = preKeyStore;
    this.identityKeyStore = identityKeyStore;
    this.remoteAddress = remoteAddress;
    this.signedPreKeyStore = signedPreKeyStore;
    this.kyberPreKeyStore = kyberPreKeyStore;
    ;
  }

  public SessionCipher(ZonaRosaProtocolStore store, ZonaRosaProtocolAddress remoteAddress) {
    this(store, store, store, store, store, remoteAddress);
  }

  /**
   * Encrypt a message.
   *
   * @param paddedMessage The plaintext message bytes, optionally padded to a constant multiple.
   * @return A ciphertext message encrypted to the recipient+device tuple.
   * @throws NoSessionException if there is no established session for this contact, or if an
   *     unacknowledged session has expired
   * @throws UntrustedIdentityException when the {@link IdentityKey} of the sender is out of date.
   */
  public CiphertextMessage encrypt(byte[] paddedMessage)
      throws NoSessionException, UntrustedIdentityException {
    return encrypt(paddedMessage, Instant.now());
  }

  /**
   * Encrypt a message.
   *
   * <p>You should only use this overload if you need to test session expiration explicitly.
   *
   * @param paddedMessage The plaintext message bytes, optionally padded to a constant multiple.
   * @return A ciphertext message encrypted to the recipient+device tuple.
   * @throws NoSessionException if there is no established session for this contact, or if an
   *     unacknowledged session has expired
   * @throws UntrustedIdentityException when the {@link IdentityKey} of the sender is out of date.
   */
  public CiphertextMessage encrypt(byte[] paddedMessage, Instant now)
      throws NoSessionException, UntrustedIdentityException {
    try (NativeHandleGuard remoteAddress = new NativeHandleGuard(this.remoteAddress)) {
      return filterExceptions(
          NoSessionException.class,
          UntrustedIdentityException.class,
          () ->
              Native.SessionCipher_EncryptMessage(
                  paddedMessage,
                  remoteAddress.nativeHandle(),
                  sessionStore,
                  identityKeyStore,
                  now.toEpochMilli()));
    }
  }

  /**
   * Decrypt a message.
   *
   * @param ciphertext The {@link PreKeyZonaRosaMessage} to decrypt.
   * @return The plaintext.
   * @throws InvalidMessageException if the input is not valid ciphertext.
   * @throws DuplicateMessageException if the input is a message that has already been received.
   * @throws InvalidKeyIdException when there is no local {@link
   *     io.zonarosa.libzonarosa.protocol.state.PreKeyRecord} that corresponds to the PreKey ID in the
   *     message.
   * @throws InvalidKeyException when the message is formatted incorrectly.
   * @throws UntrustedIdentityException when the {@link IdentityKey} of the sender is untrusted.
   */
  public byte[] decrypt(PreKeyZonaRosaMessage ciphertext)
      throws DuplicateMessageException,
          InvalidMessageException,
          InvalidKeyIdException,
          InvalidKeyException,
          UntrustedIdentityException {
    try (NativeHandleGuard ciphertextGuard = new NativeHandleGuard(ciphertext);
        NativeHandleGuard remoteAddressGuard = new NativeHandleGuard(this.remoteAddress); ) {
      return filterExceptions(
          DuplicateMessageException.class,
          InvalidMessageException.class,
          InvalidKeyIdException.class,
          InvalidKeyException.class,
          UntrustedIdentityException.class,
          () ->
              Native.SessionCipher_DecryptPreKeyZonaRosaMessage(
                  ciphertextGuard.nativeHandle(),
                  remoteAddressGuard.nativeHandle(),
                  sessionStore,
                  identityKeyStore,
                  new io.zonarosa.libzonarosa.protocol.state.internal.PreKeyStore() {
                    public NativeHandleGuard.Owner loadPreKey(int id) throws Exception {
                      return preKeyStore.loadPreKey(id);
                    }

                    public void storePreKey(int id, long rawPreKey) throws Exception {
                      preKeyStore.storePreKey(id, new PreKeyRecord(rawPreKey));
                    }

                    public void removePreKey(int id) throws Exception {
                      preKeyStore.removePreKey(id);
                    }
                  },
                  new io.zonarosa.libzonarosa.protocol.state.internal.SignedPreKeyStore() {
                    public NativeHandleGuard.Owner loadSignedPreKey(int id) throws Exception {
                      return signedPreKeyStore.loadSignedPreKey(id);
                    }

                    public void storeSignedPreKey(int id, long rawPreKey) throws Exception {
                      signedPreKeyStore.storeSignedPreKey(id, new SignedPreKeyRecord(rawPreKey));
                    }
                  },
                  new io.zonarosa.libzonarosa.protocol.state.internal.KyberPreKeyStore() {
                    public NativeHandleGuard.Owner loadKyberPreKey(int id) throws Exception {
                      return kyberPreKeyStore.loadKyberPreKey(id);
                    }

                    public void storeKyberPreKey(int id, long rawPreKey) throws Exception {
                      kyberPreKeyStore.storeKyberPreKey(id, new KyberPreKeyRecord(rawPreKey));
                    }

                    public void markKyberPreKeyUsed(int id, int ecPrekeyId, long rawBaseKey)
                        throws Exception {
                      kyberPreKeyStore.markKyberPreKeyUsed(
                          id, ecPrekeyId, new ECPublicKey(rawBaseKey));
                    }
                  }));
    }
  }

  /**
   * Decrypt a message.
   *
   * @param ciphertext The {@link ZonaRosaMessage} to decrypt.
   * @return The plaintext.
   * @throws InvalidMessageException if the input is not valid ciphertext.
   * @throws InvalidVersionException if the message version does not match the session version.
   * @throws DuplicateMessageException if the input is a message that has already been received.
   * @throws NoSessionException if there is no established session for this contact.
   */
  public byte[] decrypt(ZonaRosaMessage ciphertext)
      throws InvalidMessageException,
          InvalidVersionException,
          DuplicateMessageException,
          NoSessionException,
          UntrustedIdentityException {
    try (NativeHandleGuard ciphertextGuard = new NativeHandleGuard(ciphertext);
        NativeHandleGuard remoteAddressGuard = new NativeHandleGuard(this.remoteAddress); ) {
      return filterExceptions(
          InvalidMessageException.class,
          InvalidVersionException.class,
          DuplicateMessageException.class,
          NoSessionException.class,
          UntrustedIdentityException.class,
          () ->
              Native.SessionCipher_DecryptZonaRosaMessage(
                  ciphertextGuard.nativeHandle(),
                  remoteAddressGuard.nativeHandle(),
                  sessionStore,
                  identityKeyStore));
    }
  }

  public int getRemoteRegistrationId() {
    if (!sessionStore.containsSession(remoteAddress)) {
      throw new IllegalStateException(String.format("No session for (%s)!", remoteAddress));
    }

    SessionRecord record = sessionStore.loadSession(remoteAddress);
    return record.getRemoteRegistrationId();
  }

  public int getSessionVersion() {
    if (!sessionStore.containsSession(remoteAddress)) {
      throw new IllegalStateException(String.format("No session for (%s)!", remoteAddress));
    }

    SessionRecord record = sessionStore.loadSession(remoteAddress);
    return record.getSessionVersion();
  }
}

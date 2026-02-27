//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore;
import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle;
import io.zonarosa.libzonarosa.protocol.state.PreKeyStore;
import io.zonarosa.libzonarosa.protocol.state.SessionStore;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyStore;

/**
 * SessionBuilder is responsible for setting up encrypted sessions. Once a session has been
 * established, {@link io.zonarosa.libzonarosa.protocol.SessionCipher} can be used to encrypt/decrypt
 * messages in that session.
 *
 * <p>Sessions are built from one of two different possible vectors:
 *
 * <ol>
 *   <li>A {@link io.zonarosa.libzonarosa.protocol.state.PreKeyBundle} retrieved from a server.
 *   <li>A {@link io.zonarosa.libzonarosa.protocol.message.PreKeyZonaRosaMessage} received from a client.
 * </ol>
 *
 * Only the first, however, is handled by SessionBuilder.
 *
 * <p>Sessions are constructed per recipientId + deviceId tuple. Remote logical users are identified
 * by their recipientId, and each logical recipientId can have multiple physical devices.
 *
 * <p>This class is not thread-safe.
 *
 * @author Moxie Marlinspike
 */
public class SessionBuilder {
  private static final String TAG = SessionBuilder.class.getSimpleName();

  private final SessionStore sessionStore;
  private final PreKeyStore preKeyStore;
  private final SignedPreKeyStore signedPreKeyStore;
  private final IdentityKeyStore identityKeyStore;
  private final ZonaRosaProtocolAddress remoteAddress;

  /**
   * Constructs a SessionBuilder.
   *
   * @param sessionStore The {@link io.zonarosa.libzonarosa.protocol.state.SessionStore} to store the
   *     constructed session in.
   * @param preKeyStore The {@link io.zonarosa.libzonarosa.protocol.state.PreKeyStore} where the
   *     client's local {@link io.zonarosa.libzonarosa.protocol.state.PreKeyRecord}s are stored.
   * @param identityKeyStore The {@link io.zonarosa.libzonarosa.protocol.state.IdentityKeyStore}
   *     containing the client's identity key information.
   * @param remoteAddress The address of the remote user to build a session with.
   */
  public SessionBuilder(
      SessionStore sessionStore,
      PreKeyStore preKeyStore,
      SignedPreKeyStore signedPreKeyStore,
      IdentityKeyStore identityKeyStore,
      ZonaRosaProtocolAddress remoteAddress) {
    this.sessionStore = sessionStore;
    this.preKeyStore = preKeyStore;
    this.signedPreKeyStore = signedPreKeyStore;
    this.identityKeyStore = identityKeyStore;
    this.remoteAddress = remoteAddress;
  }

  /**
   * Constructs a SessionBuilder
   *
   * @param store The {@link ZonaRosaProtocolStore} to store all state information in.
   * @param remoteAddress The address of the remote user to build a session with.
   */
  public SessionBuilder(ZonaRosaProtocolStore store, ZonaRosaProtocolAddress remoteAddress) {
    this(store, store, store, store, remoteAddress);
  }

  /**
   * Build a new session from a {@link io.zonarosa.libzonarosa.protocol.state.PreKeyBundle} retrieved
   * from a server.
   *
   * @param preKey A PreKey for the destination recipient, retrieved from a server.
   * @throws InvalidKeyException when the {@link io.zonarosa.libzonarosa.protocol.state.PreKeyBundle}
   *     is badly formatted.
   * @throws io.zonarosa.libzonarosa.protocol.UntrustedIdentityException when the sender's {@link
   *     IdentityKey} is not trusted.
   */
  public void process(PreKeyBundle preKey) throws InvalidKeyException, UntrustedIdentityException {
    process(preKey, Instant.now());
  }

  /**
   * Build a new session from a {@link io.zonarosa.libzonarosa.protocol.state.PreKeyBundle} retrieved
   * from a server.
   *
   * <p>You should only use this overload if you need to test session expiration explicitly.
   *
   * @param preKey A PreKey for the destination recipient, retrieved from a server.
   * @param now The current time, used later to check if the session is stale.
   * @throws InvalidKeyException when the {@link io.zonarosa.libzonarosa.protocol.state.PreKeyBundle}
   *     is badly formatted.
   * @throws io.zonarosa.libzonarosa.protocol.UntrustedIdentityException when the sender's {@link
   *     IdentityKey} is not trusted.
   */
  public void process(PreKeyBundle preKey, Instant now)
      throws InvalidKeyException, UntrustedIdentityException {
    try (NativeHandleGuard preKeyGuard = new NativeHandleGuard(preKey);
        NativeHandleGuard remoteAddressGuard = new NativeHandleGuard(this.remoteAddress)) {
      filterExceptions(
          InvalidKeyException.class,
          UntrustedIdentityException.class,
          () ->
              Native.SessionBuilder_ProcessPreKeyBundle(
                  preKeyGuard.nativeHandle(),
                  remoteAddressGuard.nativeHandle(),
                  sessionStore,
                  identityKeyStore,
                  now.toEpochMilli()));
    }
  }
}

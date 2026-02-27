package io.zonarosa.service.api.crypto;

import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.SessionBuilder;
import io.zonarosa.libzonarosa.protocol.UntrustedIdentityException;
import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle;
import io.zonarosa.service.api.ZonaRosaSessionLock;

/**
 * A thread-safe wrapper around {@link SessionBuilder}.
 */
public class ZonaRosaSessionBuilder {

  private final ZonaRosaSessionLock lock;
  private final SessionBuilder    builder;

  public ZonaRosaSessionBuilder(ZonaRosaSessionLock lock, SessionBuilder builder) {
    this.lock    = lock;
    this.builder = builder;
  }

  public void process(PreKeyBundle preKey) throws InvalidKeyException, UntrustedIdentityException {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      builder.process(preKey);
    }
  }
}

package io.zonarosa.service.api.crypto;

import io.zonarosa.libzonarosa.protocol.SessionBuilder;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.groups.GroupSessionBuilder;
import io.zonarosa.libzonarosa.protocol.message.SenderKeyDistributionMessage;
import io.zonarosa.service.api.ZonaRosaSessionLock;

import java.util.UUID;

/**
 * A thread-safe wrapper around {@link SessionBuilder}.
 */
public class ZonaRosaGroupSessionBuilder {

  private final ZonaRosaSessionLock   lock;
  private final GroupSessionBuilder builder;

  public ZonaRosaGroupSessionBuilder(ZonaRosaSessionLock lock, GroupSessionBuilder builder) {
    this.lock    = lock;
    this.builder = builder;
  }

  public void process(ZonaRosaProtocolAddress sender, SenderKeyDistributionMessage senderKeyDistributionMessage) {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      builder.process(sender, senderKeyDistributionMessage);
    }
  }

  public SenderKeyDistributionMessage create(ZonaRosaProtocolAddress sender, UUID distributionId) {
    try (ZonaRosaSessionLock.Lock unused = lock.acquire()) {
      return builder.create(sender, distributionId);
    }
  }
}

package io.zonarosa.server.limits;

import java.util.UUID;

public class NoopMessageDeliveryLoopMonitor implements MessageDeliveryLoopMonitor {

  public NoopMessageDeliveryLoopMonitor() {
  }

  public void recordDeliveryAttempt(final UUID accountIdentifier, final byte deviceId, final UUID messageGuid, final String userAgent, final String context) {
  }

}

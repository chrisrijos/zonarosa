/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api.messages.multidevice;

import io.zonarosa.core.models.ServiceId;

public class ReadMessage {

  private final ServiceId.ACI sender;
  private final long          timestamp;

  public ReadMessage(ServiceId.ACI sender, long timestamp) {
    this.sender    = sender;
    this.timestamp = timestamp;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public ServiceId.ACI getSenderAci() {
    return sender;
  }

}

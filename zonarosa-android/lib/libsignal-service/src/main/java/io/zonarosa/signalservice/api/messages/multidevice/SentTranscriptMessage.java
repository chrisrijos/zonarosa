/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.api.messages.multidevice;


import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceEditMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessage;
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessageRecipient;
import io.zonarosa.core.models.ServiceId;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SentTranscriptMessage {

  private final Optional<ZonaRosaServiceAddress>          destination;
  private final long                                    timestamp;
  private final long                                    expirationStartTimestamp;
  private final Optional<ZonaRosaServiceDataMessage>      message;
  private final Map<ServiceId, Boolean>                 unidentifiedStatusBySid;
  private final Set<ServiceId>                          recipients;
  private final boolean                                 isRecipientUpdate;
  private final Optional<ZonaRosaServiceStoryMessage>     storyMessage;
  private final Set<ZonaRosaServiceStoryMessageRecipient> storyMessageRecipients;
  private final Optional<ZonaRosaServiceEditMessage>      editMessage;

  public SentTranscriptMessage(Optional<ZonaRosaServiceAddress> destination,
                               long timestamp,
                               Optional<ZonaRosaServiceDataMessage> message,
                               long expirationStartTimestamp,
                               Map<ServiceId, Boolean> unidentifiedStatus,
                               boolean isRecipientUpdate,
                               Optional<ZonaRosaServiceStoryMessage> storyMessage,
                               Set<ZonaRosaServiceStoryMessageRecipient> storyMessageRecipients,
                               Optional<ZonaRosaServiceEditMessage> editMessage)
  {
    this.destination              = destination;
    this.timestamp                = timestamp;
    this.message                  = message;
    this.expirationStartTimestamp = expirationStartTimestamp;
    this.unidentifiedStatusBySid  = new HashMap<>(unidentifiedStatus);
    this.recipients               = unidentifiedStatus.keySet();
    this.isRecipientUpdate        = isRecipientUpdate;
    this.storyMessage             = storyMessage;
    this.storyMessageRecipients   = storyMessageRecipients;
    this.editMessage              = editMessage;
  }

  public Optional<ZonaRosaServiceAddress> getDestination() {
    return destination;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getExpirationStartTimestamp() {
    return expirationStartTimestamp;
  }

  public Optional<ZonaRosaServiceDataMessage> getDataMessage() {
    return message;
  }

  public Optional<ZonaRosaServiceEditMessage> getEditMessage() {
    return editMessage;
  }

  public Optional<ZonaRosaServiceStoryMessage> getStoryMessage() {
    return storyMessage;
  }

  public Set<ZonaRosaServiceStoryMessageRecipient> getStoryMessageRecipients() {
    return storyMessageRecipients;
  }

  public boolean isUnidentified(ServiceId serviceId) {
    return unidentifiedStatusBySid.getOrDefault(serviceId, false);
  }

  public Set<ServiceId> getRecipients() {
    return recipients;
  }

  public boolean isRecipientUpdate() {
    return isRecipientUpdate;
  }
}

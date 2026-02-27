package io.zonarosa.service.api.messages;


import io.zonarosa.service.api.push.ZonaRosaServiceAddress;

import java.util.Optional;

public final class ZonaRosaServiceMetadata {
  private final ZonaRosaServiceAddress sender;
  private final int                  senderDevice;
  private final long                 timestamp;
  private final long                 serverReceivedTimestamp;
  private final long                 serverDeliveredTimestamp;
  private final boolean              needsReceipt;
  private final String               serverGuid;
  private final Optional<byte[]>     groupId;
  private final String               destinationUuid;

  public ZonaRosaServiceMetadata(ZonaRosaServiceAddress sender,
                               int senderDevice,
                               long timestamp,
                               long serverReceivedTimestamp,
                               long serverDeliveredTimestamp,
                               boolean needsReceipt,
                               String serverGuid,
                               Optional<byte[]> groupId,
                               String destinationUuid)
  {
    this.sender                   = sender;
    this.senderDevice             = senderDevice;
    this.timestamp                = timestamp;
    this.serverReceivedTimestamp  = serverReceivedTimestamp;
    this.serverDeliveredTimestamp = serverDeliveredTimestamp;
    this.needsReceipt             = needsReceipt;
    this.serverGuid               = serverGuid;
    this.groupId                  = groupId;
    this.destinationUuid          = destinationUuid != null ? destinationUuid : "";
  }

  public ZonaRosaServiceAddress getSender() {
    return sender;
  }

  public int getSenderDevice() {
    return senderDevice;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public long getServerReceivedTimestamp() {
    return serverReceivedTimestamp;
  }

  public long getServerDeliveredTimestamp() {
    return serverDeliveredTimestamp;
  }

  public boolean isNeedsReceipt() {
    return needsReceipt;
  }

  public String getServerGuid() {
    return serverGuid;
  }

  public Optional<byte[]> getGroupId() {
    return groupId;
  }

  public String getDestinationUuid() {
    return destinationUuid;
  }
}

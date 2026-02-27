package io.zonarosa.service.api.messages.multidevice;


import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.service.api.push.ZonaRosaServiceAddress;

public class VerifiedMessage {

  public enum VerifiedState {
    DEFAULT, VERIFIED, UNVERIFIED
  }

  private final ZonaRosaServiceAddress destination;
  private final IdentityKey          identityKey;
  private final VerifiedState        verified;
  private final long                 timestamp;

  public VerifiedMessage(ZonaRosaServiceAddress destination, IdentityKey identityKey, VerifiedState verified, long timestamp) {
    this.destination = destination;
    this.identityKey = identityKey;
    this.verified    = verified;
    this.timestamp   = timestamp;
  }

  public ZonaRosaServiceAddress getDestination() {
    return destination;
  }

  public IdentityKey getIdentityKey() {
    return identityKey;
  }

  public VerifiedState getVerified() {
    return verified;
  }

  public long getTimestamp() {
    return timestamp;
  }
}

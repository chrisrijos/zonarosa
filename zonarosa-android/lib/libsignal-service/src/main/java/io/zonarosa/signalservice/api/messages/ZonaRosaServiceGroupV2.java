package io.zonarosa.service.api.messages;

import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey;
import io.zonarosa.service.api.util.Preconditions;
import io.zonarosa.service.internal.push.GroupContextV2;

import io.reactivex.rxjava3.annotations.NonNull;

/**
 * Group information to include in ZonaRosaServiceMessages destined to v2 groups.
 * <p>
 * This class represents a "context" that is included with ZonaRosa Service messages
 * to make them group messages.
 */
public final class ZonaRosaServiceGroupV2 {

  private final GroupMasterKey masterKey;
  private final int            revision;
  private final byte[]         signedGroupChange;

  private ZonaRosaServiceGroupV2(Builder builder) {
    this.masterKey         = builder.masterKey;
    this.revision          = builder.revision;
    this.signedGroupChange = builder.signedGroupChange != null ? builder.signedGroupChange.clone() : null;
  }

  /**
   * Creates a context model populated from a protobuf group V2 context.
   */
  public static ZonaRosaServiceGroupV2 fromProtobuf(@NonNull GroupContextV2 groupContextV2) {
    Preconditions.checkArgument(groupContextV2.masterKey != null && groupContextV2.revision != null);

    GroupMasterKey masterKey;
    try {
      masterKey = new GroupMasterKey(groupContextV2.masterKey.toByteArray());
    } catch (InvalidInputException e) {
      throw new AssertionError(e);
    }

    Builder builder = newBuilder(masterKey);

    if (groupContextV2.groupChange != null && groupContextV2.groupChange.size() > 0) {
      builder.withSignedGroupChange(groupContextV2.groupChange.toByteArray());
    }

    return builder.withRevision(groupContextV2.revision)
                  .build();
  }

  public GroupMasterKey getMasterKey() {
    return masterKey;
  }

  public int getRevision() {
    return revision;
  }

  public byte[] getSignedGroupChange() {
    return signedGroupChange;
  }

  public boolean hasSignedGroupChange() {
    return signedGroupChange != null && signedGroupChange.length > 0;
  }

  public static Builder newBuilder(GroupMasterKey masterKey) {
    return new Builder(masterKey);
  }

  public static class Builder {

    private final GroupMasterKey masterKey;
    private       int            revision;
    private       byte[]         signedGroupChange;

    private Builder(GroupMasterKey masterKey) {
      if (masterKey == null) {
        throw new IllegalArgumentException();
      }
      this.masterKey = masterKey;
    }

    public Builder withRevision(int revision) {
      this.revision = revision;
      return this;
    }

    public Builder withSignedGroupChange(byte[] signedGroupChange) {
      this.signedGroupChange = signedGroupChange;
      return this;
    }

    public ZonaRosaServiceGroupV2 build() {
      return new ZonaRosaServiceGroupV2(this);
    }
  }
}

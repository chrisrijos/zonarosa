//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.groups;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.UUID;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyStore;
import io.zonarosa.libzonarosa.protocol.message.SenderKeyDistributionMessage;

/**
 * GroupSessionBuilder is responsible for setting up group SenderKey encrypted sessions.
 *
 * <p>Once a session has been established, {@link io.zonarosa.libzonarosa.protocol.groups.GroupCipher}
 * can be used to encrypt/decrypt messages in that session.
 *
 * <p>The built sessions are unidirectional: they can be used either for sending or for receiving,
 * but not both.
 *
 * <p>Sessions are constructed per (senderName + deviceId) tuple, with sending additionally
 * parameterized on a per-group distributionId. Remote logical users are identified by their
 * senderName, and each logical user can have multiple physical devices.
 *
 * <p>This class is not thread-safe.
 *
 * @author Moxie Marlinspike
 */
public class GroupSessionBuilder {
  private final SenderKeyStore senderKeyStore;

  public GroupSessionBuilder(SenderKeyStore senderKeyStore) {
    this.senderKeyStore = senderKeyStore;
  }

  /**
   * Construct a group session for receiving messages from sender.
   *
   * @param sender The address of the device that sent the message.
   * @param senderKeyDistributionMessage A received SenderKeyDistributionMessage.
   */
  public void process(
      ZonaRosaProtocolAddress sender, SenderKeyDistributionMessage senderKeyDistributionMessage) {
    try (NativeHandleGuard senderGuard = new NativeHandleGuard(sender);
        NativeHandleGuard skdmGuard = new NativeHandleGuard(senderKeyDistributionMessage); ) {
      filterExceptions(
          () ->
              Native.GroupSessionBuilder_ProcessSenderKeyDistributionMessage(
                  senderGuard.nativeHandle(), skdmGuard.nativeHandle(), senderKeyStore));
    }
  }

  /**
   * Construct a group session for sending messages.
   *
   * @param sender The address of the current client.
   * @param distributionId An opaque identifier that uniquely identifies the group (but isn't the
   *     group ID).
   * @return A SenderKeyDistributionMessage that is individually distributed to each member of the
   *     group.
   */
  public SenderKeyDistributionMessage create(ZonaRosaProtocolAddress sender, UUID distributionId) {
    try (NativeHandleGuard senderGuard = new NativeHandleGuard(sender)) {
      return new SenderKeyDistributionMessage(
          filterExceptions(
              () ->
                  Native.GroupSessionBuilder_CreateSenderKeyDistributionMessage(
                      senderGuard.nativeHandle(), distributionId, senderKeyStore)));
    }
  }
}

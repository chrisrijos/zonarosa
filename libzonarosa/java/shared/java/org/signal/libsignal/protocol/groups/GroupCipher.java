//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.groups;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.UUID;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.internal.NativeHandleGuard;
import io.zonarosa.libzonarosa.protocol.DuplicateMessageException;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.LegacyMessageException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyStore;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;

/**
 * The main entry point for ZonaRosa Protocol group encrypt/decrypt operations.
 *
 * <p>Once a session has been established with {@link
 * io.zonarosa.libzonarosa.protocol.groups.GroupSessionBuilder} and a {@link
 * io.zonarosa.libzonarosa.protocol.message.SenderKeyDistributionMessage} has been distributed to each
 * member of the group, this class can be used for all subsequent encrypt/decrypt operations within
 * that session (ie: until group membership changes).
 *
 * <p>This class is not thread-safe.
 *
 * @author Moxie Marlinspike
 */
public class GroupCipher {

  private final SenderKeyStore senderKeyStore;
  private final ZonaRosaProtocolAddress sender;

  public GroupCipher(SenderKeyStore senderKeyStore, ZonaRosaProtocolAddress sender) {
    this.senderKeyStore = senderKeyStore;
    this.sender = sender;
  }

  /**
   * Encrypt a message.
   *
   * @param paddedPlaintext The plaintext message bytes, optionally padded.
   * @return Ciphertext.
   * @throws NoSessionException
   */
  public CiphertextMessage encrypt(UUID distributionId, byte[] paddedPlaintext)
      throws NoSessionException {
    try (NativeHandleGuard sender = new NativeHandleGuard(this.sender)) {
      return filterExceptions(
          NoSessionException.class,
          () ->
              Native.GroupCipher_EncryptMessage(
                  sender.nativeHandle(), distributionId, paddedPlaintext, this.senderKeyStore));
    }
  }

  /**
   * Decrypt a SenderKey group message.
   *
   * @param senderKeyMessageBytes The received ciphertext.
   * @return Plaintext
   * @throws LegacyMessageException
   * @throws InvalidMessageException
   * @throws DuplicateMessageException
   */
  public byte[] decrypt(byte[] senderKeyMessageBytes)
      throws LegacyMessageException,
          DuplicateMessageException,
          InvalidMessageException,
          NoSessionException {
    try (NativeHandleGuard sender = new NativeHandleGuard(this.sender)) {
      return filterExceptions(
          LegacyMessageException.class,
          DuplicateMessageException.class,
          InvalidMessageException.class,
          NoSessionException.class,
          () ->
              Native.GroupCipher_DecryptMessage(
                  sender.nativeHandle(), senderKeyMessageBytes, this.senderKeyStore));
    }
  }
}

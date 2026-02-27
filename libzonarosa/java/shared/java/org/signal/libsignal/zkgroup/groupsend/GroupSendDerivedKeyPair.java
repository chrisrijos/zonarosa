//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.zkgroup.groupsend;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.internal.ByteArray;

/**
 * The key pair used to issue and verify group send endorsements.
 *
 * <p>Group send endorsements use a different key pair depending on the endorsement's expiration
 * (but not the user ID being endorsed). The server may cache these keys to avoid the (small) cost
 * of deriving them from the root key in {@link ServerSecretParams}. The key object stores the
 * expiration so that it doesn't need to be provided again when issuing endorsements.
 *
 * @see GroupSendEndorsementsResponse#issue
 * @see GroupSendFullToken#verify
 */
public final class GroupSendDerivedKeyPair extends ByteArray {
  public GroupSendDerivedKeyPair(byte[] contents) throws InvalidInputException {
    super(contents);
    filterExceptions(
        InvalidInputException.class,
        () -> Native.GroupSendDerivedKeyPair_CheckValidContents(contents));
  }

  /**
   * Derives a new key for group send endorsements that expire at {@code expiration}.
   *
   * <p>{@code expiration} must be day-aligned as a protection against fingerprinting by the issuing
   * server.
   */
  public static GroupSendDerivedKeyPair forExpiration(
      Instant expiration, ServerSecretParams params) {
    byte[] newContents =
        params.guardedMap(
            (publicParams) ->
                Native.GroupSendDerivedKeyPair_ForExpiration(
                    expiration.getEpochSecond(), publicParams));
    return filterExceptions(() -> new GroupSendDerivedKeyPair(newContents));
  }
}

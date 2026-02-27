/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusException;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendDerivedKeyPair;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendFullToken;
import io.zonarosa.server.identity.ServiceIdentifier;

public class GroupSendTokenUtil {

  private final ServerSecretParams serverSecretParams;
  private final Clock clock;

  public GroupSendTokenUtil(final ServerSecretParams serverSecretParams, final Clock clock) {
    this.serverSecretParams = serverSecretParams;
    this.clock = clock;
  }


  public boolean checkGroupSendToken(final ByteString groupSendToken, final ServiceIdentifier serviceIdentifier) {
    return checkGroupSendToken(groupSendToken, List.of(serviceIdentifier.toLibzonarosa()));
  }

  public boolean checkGroupSendToken(final ByteString groupSendToken, final Collection<ServiceId> serviceIds) {
    try {
      final GroupSendFullToken token = new GroupSendFullToken(groupSendToken.toByteArray());
      final GroupSendDerivedKeyPair groupSendKeyPair =
          GroupSendDerivedKeyPair.forExpiration(token.getExpiration(), serverSecretParams);
      token.verify(serviceIds, clock.instant(), groupSendKeyPair);
      return true;
    } catch (final InvalidInputException e) {
      throw GrpcExceptions.fieldViolation("group_send_token", "malformed group send token");
    } catch (VerificationFailedException e) {
      return false;
    }
  }
}

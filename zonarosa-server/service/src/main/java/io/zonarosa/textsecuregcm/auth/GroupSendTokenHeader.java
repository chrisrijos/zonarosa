/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.auth;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import java.util.Base64;
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException;
import io.zonarosa.libzonarosa.zkgroup.groupsend.GroupSendFullToken;

public record GroupSendTokenHeader(GroupSendFullToken token) {

  public static GroupSendTokenHeader valueOf(String header) {
    try {
      return new GroupSendTokenHeader(new GroupSendFullToken(Base64.getDecoder().decode(header)));
    } catch (InvalidInputException | IllegalArgumentException e) {
      // Base64 throws IllegalArgumentException; GroupSendFullToken ctor throws InvalidInputException
      throw new WebApplicationException(e, Status.UNAUTHORIZED);
    }
  }

}

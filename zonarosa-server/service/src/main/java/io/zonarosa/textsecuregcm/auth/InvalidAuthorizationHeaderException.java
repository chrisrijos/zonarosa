/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.auth;


import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public class InvalidAuthorizationHeaderException extends WebApplicationException {
  public InvalidAuthorizationHeaderException(String s) {
    super(s, Status.UNAUTHORIZED);
  }

  public InvalidAuthorizationHeaderException(Exception e) {
    super(e, Status.UNAUTHORIZED);
  }
}

/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.mappers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import io.zonarosa.server.controllers.ServerRejectedException;

public class ServerRejectedExceptionMapper implements ExceptionMapper<ServerRejectedException> {

  @Override
  public Response toResponse(final ServerRejectedException exception) {
    return Response.status(508).build();
  }
}

/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.mappers;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import io.zonarosa.server.controllers.DeviceLimitExceededException;

@Provider
public class DeviceLimitExceededExceptionMapper implements ExceptionMapper<DeviceLimitExceededException> {
  @Override
  public Response toResponse(DeviceLimitExceededException exception) {
    return Response.status(411)
                   .entity(new DeviceLimitExceededDetails(exception.getCurrentDevices(),
                                                          exception.getMaxDevices()))
                   .build();
  }

  private static class DeviceLimitExceededDetails {
    @JsonProperty
    private int current;
    @JsonProperty
    private int max;

    public DeviceLimitExceededDetails(int current, int max) {
      this.current = current;
      this.max     = max;
    }
  }
}

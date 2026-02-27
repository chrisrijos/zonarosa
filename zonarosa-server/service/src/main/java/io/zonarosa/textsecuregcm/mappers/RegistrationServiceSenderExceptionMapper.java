/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.mappers;

import com.google.common.annotations.VisibleForTesting;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import io.zonarosa.server.registration.RegistrationServiceSenderException;

public class RegistrationServiceSenderExceptionMapper implements ExceptionMapper<RegistrationServiceSenderException> {

  public static int REMOTE_SERVICE_REJECTED_REQUEST_STATUS = 440;

  @Override
  public Response toResponse(final RegistrationServiceSenderException exception) {
    return Response.status(REMOTE_SERVICE_REJECTED_REQUEST_STATUS)
        .entity(new SendVerificationCodeFailureResponse(exception.getReason(), exception.isPermanent()))
        .build();
  }

  @VisibleForTesting
  public record SendVerificationCodeFailureResponse(RegistrationServiceSenderException.Reason reason,
                                                    boolean permanentFailure) {

  }
}

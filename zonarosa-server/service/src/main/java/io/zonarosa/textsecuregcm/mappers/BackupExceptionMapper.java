/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.mappers;

import io.dropwizard.jersey.errors.ErrorMessage;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import io.zonarosa.server.backup.BackupBadReceiptException;
import io.zonarosa.server.backup.BackupException;
import io.zonarosa.server.backup.BackupFailedZkAuthenticationException;
import io.zonarosa.server.backup.BackupInvalidArgumentException;
import io.zonarosa.server.backup.BackupMissingIdCommitmentException;
import io.zonarosa.server.backup.BackupNotFoundException;
import io.zonarosa.server.backup.BackupPermissionException;
import io.zonarosa.server.backup.BackupWrongCredentialTypeException;

public class BackupExceptionMapper implements ExceptionMapper<BackupException> {

  @Override
  public Response toResponse(final BackupException exception) {
    final Response.Status status = (switch (exception) {
      case BackupNotFoundException _ -> Response.Status.NOT_FOUND;
      case BackupInvalidArgumentException _, BackupBadReceiptException _ -> Response.Status.BAD_REQUEST;
      case BackupPermissionException _ -> Response.Status.FORBIDDEN;
      case BackupMissingIdCommitmentException _ -> Response.Status.CONFLICT;
      case BackupWrongCredentialTypeException _,
           BackupFailedZkAuthenticationException _ -> Response.Status.UNAUTHORIZED;
      default -> Response.Status.INTERNAL_SERVER_ERROR;
    });

    final WebApplicationException wae =
        new WebApplicationException(exception.getMessage(), exception, Response.status(status).build());

    return Response
        .fromResponse(wae.getResponse())
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new ErrorMessage(wae.getResponse().getStatus(), wae.getLocalizedMessage())).build();

  }
}

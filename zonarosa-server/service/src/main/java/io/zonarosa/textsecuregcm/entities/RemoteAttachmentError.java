/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Indicates an attachment failed to upload")
public record RemoteAttachmentError(
    @Schema(description = "The type of error encountered")
    @Valid @NotNull ErrorType error)
    implements TransferArchiveResult {

  public enum ErrorType {
    RELINK_REQUESTED,
    CONTINUE_WITHOUT_UPLOAD;
  }
}

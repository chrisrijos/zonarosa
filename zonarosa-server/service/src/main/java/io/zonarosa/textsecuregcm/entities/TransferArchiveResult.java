/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
    @JsonSubTypes.Type(value = RemoteAttachment.class, name = "success"),
    @JsonSubTypes.Type(value = RemoteAttachmentError.class, name = "error"),
})
public sealed interface TransferArchiveResult permits RemoteAttachment, RemoteAttachmentError {}

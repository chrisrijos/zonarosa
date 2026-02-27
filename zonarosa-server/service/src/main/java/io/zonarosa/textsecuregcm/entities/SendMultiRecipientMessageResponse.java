/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.util.ServiceIdentifierAdapter;

public record SendMultiRecipientMessageResponse(
    @Schema(description = "a list of the service identifiers in the request that do not correspond to registered ZonaRosa users; will only be present if a group send endorsement was supplied for the request")
    @JsonSerialize(contentUsing = ServiceIdentifierAdapter.ServiceIdentifierSerializer.class)
    @JsonDeserialize(contentUsing = ServiceIdentifierAdapter.ServiceIdentifierDeserializer.class)
    List<ServiceIdentifier> uuids404) {
}

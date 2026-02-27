/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.UUID;
import javax.annotation.Nullable;
import io.zonarosa.server.controllers.AccountController;
import io.zonarosa.server.util.ByteArrayBase64UrlAdapter;
import io.zonarosa.server.util.ExactlySize;

public record UsernameHashResponse(
    @Valid
    @JsonSerialize(using = ByteArrayBase64UrlAdapter.Serializing.class)
    @JsonDeserialize(using = ByteArrayBase64UrlAdapter.Deserializing.class)
    @ExactlySize(AccountController.USERNAME_HASH_LENGTH)
    @Schema(type = "string", description = "The hash of the confirmed username, as supplied in the request")
    byte[] usernameHash,

    @Nullable
    @Valid
    @Schema(type = "string", description = "A handle that can be included in username links to retrieve the stored encrypted username")
    UUID usernameLinkHandle
) {}

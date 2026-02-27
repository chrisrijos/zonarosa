/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;


import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import io.zonarosa.server.controllers.AccountController;
import io.zonarosa.server.util.ByteArrayBase64UrlAdapter;

public record ReserveUsernameHashRequest(
    @NotNull
    @Valid
    @Size(min=1, max=AccountController.MAXIMUM_USERNAME_HASHES_LIST_LENGTH)
    @JsonSerialize(contentUsing = ByteArrayBase64UrlAdapter.Serializing.class)
    @JsonDeserialize(contentUsing = ByteArrayBase64UrlAdapter.Deserializing.class)
    List<byte[]> usernameHashes
) {}

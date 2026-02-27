/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import javax.annotation.Nullable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.zonarosa.server.controllers.AccountController;
import io.zonarosa.server.util.ByteArrayBase64UrlAdapter;
import io.zonarosa.server.util.ExactlySize;

import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmUsernameHashRequest(
    @JsonSerialize(using = ByteArrayBase64UrlAdapter.Serializing.class)
    @JsonDeserialize(using = ByteArrayBase64UrlAdapter.Deserializing.class)
    @ExactlySize(AccountController.USERNAME_HASH_LENGTH)
    byte[] usernameHash,

    @JsonSerialize(using = ByteArrayBase64UrlAdapter.Serializing.class)
    @JsonDeserialize(using = ByteArrayBase64UrlAdapter.Deserializing.class)
    @NotNull
    byte[] zkProof,

    @Schema(type = "string", description = "The url-safe base64-encoded encrypted username to be stored for username links")
    @Nullable
    @JsonSerialize(using = ByteArrayBase64UrlAdapter.Serializing.class)
    @JsonDeserialize(using = ByteArrayBase64UrlAdapter.Deserializing.class)
    @Size(min = 1, max = AccountController.MAXIMUM_USERNAME_CIPHERTEXT_LENGTH)
    byte[] encryptedUsername
) {}

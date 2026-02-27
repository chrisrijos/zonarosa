/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.zonarosa.server.controllers.AccountController;
import io.zonarosa.server.util.ByteArrayBase64UrlAdapter;
import io.zonarosa.server.util.ExactlySize;
import java.util.UUID;

public record ReserveUsernameHashResponse(
    @JsonSerialize(using = ByteArrayBase64UrlAdapter.Serializing.class)
    @JsonDeserialize(using = ByteArrayBase64UrlAdapter.Deserializing.class)
    @ExactlySize(AccountController.USERNAME_HASH_LENGTH)
    byte[] usernameHash
) {}

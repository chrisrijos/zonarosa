/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.attachments;

import jakarta.validation.constraints.NotEmpty;
import io.zonarosa.server.configuration.secrets.SecretBytes;
import io.zonarosa.server.util.ExactlySize;

public record TusConfiguration(
  @ExactlySize(32) SecretBytes userAuthenticationTokenSharedSecret,
  @NotEmpty String uploadUri
){}

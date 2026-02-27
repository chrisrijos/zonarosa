/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import io.zonarosa.server.configuration.secrets.SecretBytes;

public record ZkConfig(@NotNull SecretBytes serverSecret,
                       @NotEmpty byte[] serverPublic) {
}

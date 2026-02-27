/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.constraints.NotBlank;
import javax.annotation.Nullable;
import java.net.URI;

public record PagedSingleUseKEMPreKeyStoreConfiguration(
    @NotBlank String bucket,
    @NotBlank String region,
    @Nullable URI endpointOverride) {
}

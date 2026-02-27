/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import javax.annotation.Nullable;
import java.net.URI;

public record CdnConfiguration(@NotNull @Valid StaticAwsCredentialsFactory credentials,
                               @NotBlank String bucket,
                               @NotBlank String region,
                               @Nullable URI endpointOverride) {
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import io.zonarosa.server.util.InetAddressRange;

public record ExternalRequestFilterConfiguration(@Valid @NotNull Set<@NotNull String> paths,
                                                 @Valid @NotNull Set<@NotNull InetAddressRange> permittedInternalRanges,
                                                 @Valid @NotNull Set<@NotNull String> grpcMethods) {
}

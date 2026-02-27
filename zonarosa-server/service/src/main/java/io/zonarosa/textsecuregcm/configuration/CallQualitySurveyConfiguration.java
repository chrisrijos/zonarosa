/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CallQualitySurveyConfiguration (@Valid @NotNull PubSubPublisherFactory pubSubPublisher) {
}

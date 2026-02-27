/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import java.time.Duration;

/**
 * Configuration for Apple DeviceCheck
 *
 * @param production               Whether this is for production or sandbox attestations
 * @param teamId                   The teamId to validate attestations against
 * @param bundleId                 The bundleId to validation attestations against
 */
public record AppleDeviceCheckConfiguration(
    boolean production,
    String teamId,
    String bundleId) {}

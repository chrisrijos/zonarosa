/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import java.time.Duration;

/**
 * Configuration for Device Check operations
 *
 * @param backupRedemptionDuration How long to grant backup access for redemptions via device check
 * @param backupRedemptionLevel    What backup level to grant redemptions via device check
 */
public record DeviceCheckConfiguration(Duration backupRedemptionDuration, long backupRedemptionLevel) {}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import io.zonarosa.server.configuration.secrets.SecretBytes;

public record LinkDeviceSecretConfiguration(SecretBytes secret) {
}

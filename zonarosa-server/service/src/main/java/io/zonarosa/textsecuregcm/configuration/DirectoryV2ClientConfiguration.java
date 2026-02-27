/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.configuration;

import io.zonarosa.server.configuration.secrets.SecretBytes;
import io.zonarosa.server.util.ExactlySize;

public record DirectoryV2ClientConfiguration(@ExactlySize(32) SecretBytes userAuthenticationTokenSharedSecret,
                                             @ExactlySize(32) SecretBytes userIdTokenSharedSecret) {
}

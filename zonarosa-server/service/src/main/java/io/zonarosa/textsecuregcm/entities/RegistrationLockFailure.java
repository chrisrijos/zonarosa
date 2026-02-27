/*
 * Copyright 2013-2020 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import javax.annotation.Nullable;
import io.zonarosa.server.auth.ExternalServiceCredentials;

@Schema(description = """
    Information about the current Registration lock and SVR credentials. With a correct PIN, the credentials can
    be used to recover the secret used to derive the registration lock password.
    """)
public record RegistrationLockFailure(
    @Schema(description = "Time remaining in milliseconds before the existing registration lock expires")
    long timeRemaining,
    @Schema(description = "Credentials that can be used with SVR2")
    @Nullable
    ExternalServiceCredentials svr2Credentials) {
}

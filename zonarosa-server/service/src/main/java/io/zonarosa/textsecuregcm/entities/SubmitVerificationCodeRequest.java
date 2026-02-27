/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import jakarta.validation.constraints.NotBlank;

public record SubmitVerificationCodeRequest(@NotBlank String code) {

}

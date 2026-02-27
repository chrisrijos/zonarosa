/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AnswerPushChallengeRequest extends AnswerChallengeRequest {

  @Schema(description = "A token provided to the client via a push payload")
  @NotBlank
  private String challenge;

  public String getChallenge() {
    return challenge;
  }
}

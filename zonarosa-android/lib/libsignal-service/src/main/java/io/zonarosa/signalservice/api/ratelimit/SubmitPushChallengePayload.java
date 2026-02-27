/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.ratelimit;

import com.fasterxml.jackson.annotation.JsonProperty;

class SubmitPushChallengePayload {

  @JsonProperty
  private String type;

  @JsonProperty
  private String challenge;

  public SubmitPushChallengePayload() {}

  public SubmitPushChallengePayload(String challenge) {
    this.type      = "rateLimitPushChallenge";
    this.challenge = challenge;
  }
}

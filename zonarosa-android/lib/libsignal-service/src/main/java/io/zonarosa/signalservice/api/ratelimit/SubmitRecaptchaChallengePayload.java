/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.ratelimit;

import com.fasterxml.jackson.annotation.JsonProperty;

class SubmitRecaptchaChallengePayload {

  @JsonProperty
  private String type;

  @JsonProperty
  private String token;

  @JsonProperty
  private String captcha;

  public SubmitRecaptchaChallengePayload() {}

  public SubmitRecaptchaChallengePayload(String challenge, String recaptchaToken) {
    this.type    = "captcha";
    this.token   = challenge;
    this.captcha = recaptchaToken;
  }
}

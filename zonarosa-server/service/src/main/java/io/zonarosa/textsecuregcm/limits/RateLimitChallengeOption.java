/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.limits;

public enum RateLimitChallengeOption {
  CAPTCHA("captcha"),
  PUSH_CHALLENGE("pushChallenge");

  private final String apiName;

  RateLimitChallengeOption(final String apiName) {
    this.apiName = apiName;
  }

  public String getApiName() {
    return apiName;
  }
}

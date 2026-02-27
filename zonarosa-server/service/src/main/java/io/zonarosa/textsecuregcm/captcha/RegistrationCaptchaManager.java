/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.captcha;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class RegistrationCaptchaManager {

  private final CaptchaChecker captchaChecker;

  public RegistrationCaptchaManager(final CaptchaChecker captchaChecker) {
    this.captchaChecker = captchaChecker;
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public Optional<AssessmentResult> assessCaptcha(final Optional<UUID> aci, final Optional<String> captcha, final String sourceHost, final String userAgent)
      throws IOException {
    return captcha.isPresent()
        ? Optional.of(captchaChecker.verify(aci, Action.REGISTRATION, captcha.get(), sourceHost, userAgent))
        : Optional.empty();
  }
}

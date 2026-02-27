/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.spam;

import jakarta.ws.rs.container.ContainerRequestContext;
import java.util.Optional;
import io.zonarosa.server.storage.Account;

public interface ChallengeConstraintChecker {

  record ChallengeConstraints(boolean pushPermitted, Optional<Float> captchaScoreThreshold) {}

  /**
   * Retrieve constraints for captcha and push challenges
   *
   * @param authenticatedAccount The authenticated account attempting to request or solve a challenge
   * @return ChallengeConstraints indicating what constraints should be applied to challenges
   */
  ChallengeConstraints challengeConstraints(ContainerRequestContext requestContext, Account authenticatedAccount);

  static ChallengeConstraintChecker noop() {
    return (account, ctx) -> new ChallengeConstraints(true, Optional.empty());
  }
}

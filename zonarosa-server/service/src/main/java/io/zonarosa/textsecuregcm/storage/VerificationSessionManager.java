/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import io.zonarosa.server.registration.VerificationSession;

public class VerificationSessionManager {

  private final VerificationSessions verificationSessions;

  public VerificationSessionManager(final VerificationSessions verificationSessions) {
    this.verificationSessions = verificationSessions;
  }

  public CompletableFuture<Void> insert(final VerificationSession verificationSession) {
    return verificationSessions.insert(verificationSession.sessionId(), verificationSession);
  }

  public CompletableFuture<Void> update(final VerificationSession verificationSession) {
    return verificationSessions.update(verificationSession.sessionId(), verificationSession);
  }

  public CompletableFuture<Optional<VerificationSession>> findForId(final String encodedSessionId) {
    return verificationSessions.findForKey(encodedSessionId);
  }

}

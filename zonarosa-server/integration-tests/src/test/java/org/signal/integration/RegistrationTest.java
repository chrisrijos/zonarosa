/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.integration;

import io.micrometer.common.util.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.zonarosa.server.entities.CreateVerificationSessionRequest;
import io.zonarosa.server.entities.SubmitVerificationCodeRequest;
import io.zonarosa.server.entities.UpdateVerificationSessionRequest;
import io.zonarosa.server.entities.VerificationCodeRequest;
import io.zonarosa.server.entities.VerificationSessionResponse;

public class RegistrationTest {

  @Test
  public void testRegistration() throws Exception {
    final UpdateVerificationSessionRequest originalRequest = new UpdateVerificationSessionRequest(
        "test", UpdateVerificationSessionRequest.PushTokenType.FCM, null, null, null, null);

    final Operations.PrescribedVerificationNumber params = Operations.prescribedVerificationNumber();
    final CreateVerificationSessionRequest input = new CreateVerificationSessionRequest(params.number(),
        originalRequest);

    final VerificationSessionResponse verificationSessionResponse = Operations
        .apiPost("/v1/verification/session", input)
        .executeExpectSuccess(VerificationSessionResponse.class);

    final String sessionId = verificationSessionResponse.id();
    Assertions.assertTrue(StringUtils.isNotBlank(sessionId));

    final String pushChallenge = Operations.peekVerificationSessionPushChallenge(sessionId);

    // supply push challenge
    final UpdateVerificationSessionRequest updatedRequest = new UpdateVerificationSessionRequest(
        "test", UpdateVerificationSessionRequest.PushTokenType.FCM, pushChallenge, null, null, null);
    final VerificationSessionResponse pushChallengeSupplied = Operations
        .apiPatch("/v1/verification/session/%s".formatted(sessionId), updatedRequest)
        .executeExpectSuccess(VerificationSessionResponse.class);

    Assertions.assertTrue(pushChallengeSupplied.allowedToRequestCode());

    // request code
    final VerificationCodeRequest verificationCodeRequest = new VerificationCodeRequest(
        VerificationCodeRequest.Transport.SMS, "android-ng");

    final VerificationSessionResponse codeRequested = Operations
        .apiPost("/v1/verification/session/%s/code".formatted(sessionId), verificationCodeRequest)
        .executeExpectSuccess(VerificationSessionResponse.class);

    // verify code
    final SubmitVerificationCodeRequest submitVerificationCodeRequest = new SubmitVerificationCodeRequest(
        params.verificationCode());
    final VerificationSessionResponse codeVerified = Operations
        .apiPut("/v1/verification/session/%s/code".formatted(sessionId), submitVerificationCodeRequest)
        .executeExpectSuccess(VerificationSessionResponse.class);
  }
}

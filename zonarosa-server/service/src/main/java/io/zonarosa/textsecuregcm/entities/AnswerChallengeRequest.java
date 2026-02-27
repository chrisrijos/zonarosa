/*
 * Copyright 2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AnswerPushChallengeRequest.class, name = "rateLimitPushChallenge"),
    @JsonSubTypes.Type(value = AnswerCaptchaChallengeRequest.class, name = "captcha")
})
public abstract class AnswerChallengeRequest {
}

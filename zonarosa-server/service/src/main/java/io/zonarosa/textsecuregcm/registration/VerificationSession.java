/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.registration;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import javax.annotation.Nullable;
import io.zonarosa.server.storage.SerializedExpireableJsonDynamoStore;
import io.zonarosa.server.telephony.CarrierData;

/**
 * Server-internal stored session object. Primarily used by
 * {@link io.zonarosa.server.controllers.VerificationController} to manage the steps required to begin
 * requesting codes from Registration Service, in order to get a verified session to be provided to
 * {@link io.zonarosa.server.controllers.RegistrationController}.
 *
 * @param sessionId               the session ID returned by Registration Service
 * @param pushChallenge           the value of a push challenge sent to a client, after it submitted a push token
 * @param carrierData             information about the phone number's carrier if available
 * @param requestedInformation    information requested that a client send to the server
 * @param submittedInformation    information that a client has submitted and that the server has verified
 * @param smsSenderOverride       if present, indicates a sender override argument that should be forwarded to the
 *                                Registration Service when requesting a code
 * @param voiceSenderOverride     if present, indicates a sender override argument that should be forwarded to the
 *                                Registration Service when requesting a code
 * @param allowedToRequestCode    whether the client is allowed to request a code. This request will be forwarded to
 *                                Registration Service
 * @param createdTimestamp        when this session was created
 * @param updatedTimestamp        when this session was updated
 * @param remoteExpirationSeconds when the remote
 *                                {@link io.zonarosa.server.entities.RegistrationServiceSession} expires
 * @see io.zonarosa.server.entities.RegistrationServiceSession
 * @see io.zonarosa.server.entities.VerificationSessionResponse
 */
public record VerificationSession(
    String sessionId,
    @Nullable String pushChallenge,
    @Nullable CarrierData carrierData,
    List<Information> requestedInformation,
    List<Information> submittedInformation,
    @Nullable String smsSenderOverride,
    @Nullable String voiceSenderOverride,
    boolean allowedToRequestCode,
    long createdTimestamp,
    long updatedTimestamp,
    long remoteExpirationSeconds) implements SerializedExpireableJsonDynamoStore.Expireable {

  @Override
  public long getExpirationEpochSeconds() {
    return Instant.ofEpochMilli(updatedTimestamp).plusSeconds(remoteExpirationSeconds).getEpochSecond();
  }

  public enum Information {
    @JsonProperty("pushChallenge")
    PUSH_CHALLENGE,
    @JsonProperty("captcha")
    CAPTCHA
  }
}

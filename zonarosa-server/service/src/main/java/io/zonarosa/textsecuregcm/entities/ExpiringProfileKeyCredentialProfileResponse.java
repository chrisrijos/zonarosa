/*
 * Copyright 2013-2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import io.zonarosa.libzonarosa.zkgroup.profiles.ExpiringProfileKeyCredentialResponse;

public class ExpiringProfileKeyCredentialProfileResponse extends CredentialProfileResponse {

  @JsonProperty
  @JsonSerialize(using = ExpiringProfileKeyCredentialResponseAdapter.Serializing.class)
  @JsonDeserialize(using = ExpiringProfileKeyCredentialResponseAdapter.Deserializing.class)
  @Nullable
  private ExpiringProfileKeyCredentialResponse credential;

  public ExpiringProfileKeyCredentialProfileResponse() {
  }

  public ExpiringProfileKeyCredentialProfileResponse(final VersionedProfileResponse versionedProfileResponse,
      @Nullable final ExpiringProfileKeyCredentialResponse credential) {

    super(versionedProfileResponse);
    this.credential = credential;
  }

  @Nullable
  public ExpiringProfileKeyCredentialResponse getCredential() {
    return credential;
  }
}

/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

public abstract class CredentialProfileResponse {

  @JsonUnwrapped
  private VersionedProfileResponse versionedProfileResponse;

  protected CredentialProfileResponse() {
  }

  protected CredentialProfileResponse(final VersionedProfileResponse versionedProfileResponse) {
    this.versionedProfileResponse = versionedProfileResponse;
  }

  public VersionedProfileResponse getVersionedProfileResponse() {
    return versionedProfileResponse;
  }
}

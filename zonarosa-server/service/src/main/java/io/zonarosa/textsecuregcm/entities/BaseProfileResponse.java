/*
 * Copyright 2013-2021 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.util.ByteArrayBase64WithPaddingAdapter;
import io.zonarosa.server.util.ServiceIdentifierAdapter;
import io.zonarosa.server.util.IdentityKeyAdapter;

import java.util.List;
import java.util.Map;

public class BaseProfileResponse {

  @JsonProperty
  @JsonSerialize(using = IdentityKeyAdapter.Serializer.class)
  @JsonDeserialize(using = IdentityKeyAdapter.Deserializer.class)
  private IdentityKey identityKey;

  @JsonProperty
  @JsonSerialize(using = ByteArrayBase64WithPaddingAdapter.Serializing.class)
  @JsonDeserialize(using = ByteArrayBase64WithPaddingAdapter.Deserializing.class)
  private byte[] unidentifiedAccess;

  @JsonProperty
  private boolean unrestrictedUnidentifiedAccess;

  @JsonProperty
  private Map<String, Boolean> capabilities;

  @JsonProperty
  private List<Badge> badges;

  @JsonProperty
  @JsonSerialize(using = ServiceIdentifierAdapter.ServiceIdentifierSerializer.class)
  @JsonDeserialize(using = ServiceIdentifierAdapter.ServiceIdentifierDeserializer.class)
  private ServiceIdentifier uuid;

  public BaseProfileResponse() {
  }

  public BaseProfileResponse(final IdentityKey identityKey,
      final byte[] unidentifiedAccess,
      final boolean unrestrictedUnidentifiedAccess,
      final Map<String, Boolean> capabilities,
      final List<Badge> badges,
      final ServiceIdentifier uuid) {

    this.identityKey = identityKey;
    this.unidentifiedAccess = unidentifiedAccess;
    this.unrestrictedUnidentifiedAccess = unrestrictedUnidentifiedAccess;
    this.capabilities = capabilities;
    this.badges = badges;
    this.uuid = uuid;
  }

  public IdentityKey getIdentityKey() {
    return identityKey;
  }

  public byte[] getUnidentifiedAccess() {
    return unidentifiedAccess;
  }

  public boolean isUnrestrictedUnidentifiedAccess() {
    return unrestrictedUnidentifiedAccess;
  }

  public Map<String, Boolean> getCapabilities() {
    return capabilities;
  }

  public List<Badge> getBadges() {
    return badges;
  }

  public ServiceIdentifier getUuid() {
    return uuid;
  }
}

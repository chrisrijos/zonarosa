/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server.entities;

import static io.zonarosa.server.util.RegistrationIdValidator.validRegistrationId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.annotations.VisibleForTesting;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import io.zonarosa.server.auth.UnidentifiedAccessUtil;
import io.zonarosa.server.storage.DeviceCapability;
import io.zonarosa.server.util.ByteArrayAdapter;
import io.zonarosa.server.util.DeviceCapabilityAdapter;
import io.zonarosa.server.util.ExactlySize;

public class AccountAttributes {

  @JsonProperty
  private boolean fetchesMessages;

  @JsonProperty
  private int registrationId;

  @JsonProperty("pniRegistrationId")
  private int phoneNumberIdentityRegistrationId;

  @JsonProperty
  @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
  @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
  @Size(max = 225)
  private byte[] name;

  @JsonProperty
  private String registrationLock;

  @JsonProperty
  @ExactlySize({0, UnidentifiedAccessUtil.UNIDENTIFIED_ACCESS_KEY_LENGTH})
  private byte[] unidentifiedAccessKey;

  @JsonProperty
  private boolean unrestrictedUnidentifiedAccess;

  @JsonProperty
  @JsonSerialize(using = DeviceCapabilityAdapter.Serializer.class)
  @JsonDeserialize(using = DeviceCapabilityAdapter.Deserializer.class)
  @Nullable
  private Set<DeviceCapability> capabilities;

  @JsonProperty
  private boolean discoverableByPhoneNumber = true;

  @JsonProperty
  @Nullable
  @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
  private byte[] recoveryPassword = null;

  public AccountAttributes() {
  }

  @VisibleForTesting
  public AccountAttributes(
      final boolean fetchesMessages,
      final int registrationId,
      final int phoneNumberIdentifierRegistrationId,
      final byte[] name,
      final String registrationLock,
      final boolean discoverableByPhoneNumber,
      final Set<DeviceCapability> capabilities) {
    this.fetchesMessages = fetchesMessages;
    this.registrationId = registrationId;
    this.phoneNumberIdentityRegistrationId = phoneNumberIdentifierRegistrationId;
    this.name = name;
    this.registrationLock = registrationLock;
    this.discoverableByPhoneNumber = discoverableByPhoneNumber;
    this.capabilities = capabilities;
  }

  public boolean getFetchesMessages() {
    return fetchesMessages;
  }

  public int getRegistrationId() {
    return registrationId;
  }

  public int getPhoneNumberIdentityRegistrationId() {
    return phoneNumberIdentityRegistrationId;
  }

  public byte[] getName() {
    return name;
  }

  public String getRegistrationLock() {
    return registrationLock;
  }

  public byte[] getUnidentifiedAccessKey() {
    return unidentifiedAccessKey;
  }

  public boolean isUnrestrictedUnidentifiedAccess() {
    return unrestrictedUnidentifiedAccess;
  }

  @Nullable
  public Set<DeviceCapability> getCapabilities() {
    return capabilities;
  }

  public boolean isDiscoverableByPhoneNumber() {
    return discoverableByPhoneNumber;
  }

  public Optional<byte[]> recoveryPassword() {
    return Optional.ofNullable(recoveryPassword);
  }

  @VisibleForTesting
  public AccountAttributes withUnidentifiedAccessKey(final byte[] unidentifiedAccessKey) {
    this.unidentifiedAccessKey = unidentifiedAccessKey;
    return this;
  }

  @VisibleForTesting
  public AccountAttributes withRecoveryPassword(final byte[] recoveryPassword) {
    this.recoveryPassword = recoveryPassword;
    return this;
  }

  @AssertTrue
  @Schema(hidden = true)
  public boolean isEachRegistrationIdValid() {
    return validRegistrationId(registrationId) && validRegistrationId(phoneNumberIdentityRegistrationId);
  }
}

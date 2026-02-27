/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Optional;
import jakarta.validation.constraints.PositiveOrZero;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.util.ByteArrayAdapter;
import io.zonarosa.server.util.ByteArrayBase64UrlAdapter;
import io.zonarosa.server.util.ExactlySize;
import io.zonarosa.server.util.ServiceIdentifierAdapter;

public record KeyTransparencyMonitorRequest(

    @Valid
    @NotNull
    AciMonitor aci,

    @Valid
    @NotNull
    Optional<@Valid E164Monitor> e164,

    @Valid
    @NotNull
    Optional<@Valid UsernameHashMonitor> usernameHash,

    @Schema(description = "The tree head size to prove consistency against.")
    @Positive long lastNonDistinguishedTreeHeadSize,

    @Schema(description = "The distinguished tree head size to prove consistency against.")
    @Positive long lastDistinguishedTreeHeadSize
) {

  public record AciMonitor(
      @NotNull
      @JsonSerialize(using = ServiceIdentifierAdapter.ServiceIdentifierSerializer.class)
      @JsonDeserialize(using = ServiceIdentifierAdapter.AciServiceIdentifierDeserializer.class)
      @Schema(description = "The aci identifier to monitor")
      AciServiceIdentifier value,

      @Schema(description = "A log tree position maintained by the client for the aci.")
      @PositiveOrZero
      long entryPosition,

      @Schema(description = "The commitment index derived from a previous search request, encoded in standard unpadded base64")
      @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
      @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
      @NotNull
      @ExactlySize(32)
      byte[] commitmentIndex
  ) {}

  public record E164Monitor(
      @Schema(description = "The e164-formatted phone number to monitor")
      @NotBlank
      String value,

      @Schema(description = "A log tree position maintained by the client for the e164.")
      @PositiveOrZero
      long entryPosition,

      @Schema(description = "The commitment index derived from a previous search request, encoded in standard unpadded base64")
      @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
      @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
      @NotNull
      @ExactlySize(32)
      byte[] commitmentIndex
  ) {}

  public record UsernameHashMonitor(

      @Schema(description = "The username hash to monitor, encoded in url-safe unpadded base64.")
      @JsonSerialize(using = ByteArrayBase64UrlAdapter.Serializing.class)
      @JsonDeserialize(using = ByteArrayBase64UrlAdapter.Deserializing.class)
      @NotNull
      @NotEmpty
      byte[] value,

      @Schema(description = "A log tree position maintained by the client for the username hash.")
      @PositiveOrZero
      long entryPosition,

      @Schema(description = "The commitment index derived from a previous search request, encoded in standard unpadded base64")
      @JsonSerialize(using = ByteArrayAdapter.Serializing.class)
      @JsonDeserialize(using = ByteArrayAdapter.Deserializing.class)
      @NotNull
      @ExactlySize(32)
      byte[] commitmentIndex
  ) {}
}

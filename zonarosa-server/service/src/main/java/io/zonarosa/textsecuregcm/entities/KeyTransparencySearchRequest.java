/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Optional;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.server.identity.AciServiceIdentifier;
import io.zonarosa.server.util.ByteArrayBase64UrlAdapter;
import io.zonarosa.server.util.ByteArrayBase64WithPaddingAdapter;
import io.zonarosa.server.util.E164;
import io.zonarosa.server.util.IdentityKeyAdapter;
import io.zonarosa.server.util.ServiceIdentifierAdapter;

public record KeyTransparencySearchRequest(
    @NotNull
    @JsonSerialize(using = ServiceIdentifierAdapter.ServiceIdentifierSerializer.class)
    @JsonDeserialize(using = ServiceIdentifierAdapter.AciServiceIdentifierDeserializer.class)
    @Schema(description = "The ACI to look up")
    AciServiceIdentifier aci,

    @E164
    @Schema(description = "The E164-formatted phone number to look up")
    Optional<String> e164,

    @JsonSerialize(contentUsing = ByteArrayBase64UrlAdapter.Serializing.class)
    @JsonDeserialize(contentUsing = ByteArrayBase64UrlAdapter.Deserializing.class)
    @Schema(description = "The username hash to look up, encoded in web-safe unpadded base64.")
    Optional<byte[]> usernameHash,

    @NotNull
    @JsonSerialize(using = IdentityKeyAdapter.Serializer.class)
    @JsonDeserialize(using = IdentityKeyAdapter.Deserializer.class)
    @Schema(description="The public ACI identity key associated with the provided ACI")
    IdentityKey aciIdentityKey,

    @JsonSerialize(contentUsing = ByteArrayBase64WithPaddingAdapter.Serializing.class)
    @JsonDeserialize(contentUsing = ByteArrayBase64WithPaddingAdapter.Deserializing.class)
    @Schema(description="The unidentified access key associated with the account")
    Optional<byte[]> unidentifiedAccessKey,

    @Schema(description = "The non-distinguished tree head size to prove consistency against.")
    Optional<@Positive Long> lastTreeHeadSize,

    @Schema(description = "The distinguished tree head size to prove consistency against.")
    @Positive long distinguishedTreeHeadSize
) {
    // This is the max value for a protobuf uint32 field
    private static final long MAX_UINT32 = 0xFFFFFFFFL;

    @AssertTrue
    @Schema(hidden = true)
    public boolean isUnidentifiedAccessKeyProvidedWithE164() {
      return unidentifiedAccessKey.isPresent() == e164.isPresent();
    }
}

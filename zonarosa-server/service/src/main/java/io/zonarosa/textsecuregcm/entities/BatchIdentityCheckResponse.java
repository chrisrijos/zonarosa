/*
 * Copyright 2022 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.server.identity.ServiceIdentifier;
import io.zonarosa.server.util.IdentityKeyAdapter;
import io.zonarosa.server.util.ServiceIdentifierAdapter;

public record BatchIdentityCheckResponse(@Valid List<Element> elements) {

  public record Element(@JsonInclude(JsonInclude.Include.NON_EMPTY)
                        @JsonSerialize(using = ServiceIdentifierAdapter.ServiceIdentifierSerializer.class)
                        @JsonDeserialize(using = ServiceIdentifierAdapter.ServiceIdentifierDeserializer.class)
                        @NotNull
                        ServiceIdentifier uuid,

                        @NotNull
                        @JsonSerialize(using = IdentityKeyAdapter.Serializer.class)
                        @JsonDeserialize(using = IdentityKeyAdapter.Deserializer.class)
                        IdentityKey identityKey) {
  }
}

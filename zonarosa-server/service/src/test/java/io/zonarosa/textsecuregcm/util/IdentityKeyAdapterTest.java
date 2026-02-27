/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Base64;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;

class IdentityKeyAdapterTest {

  private static final IdentityKey IDENTITY_KEY = new IdentityKey(ECKeyPair.generate().getPublicKey());

  private record IdentityKeyCarrier(@JsonSerialize(using = IdentityKeyAdapter.Serializer.class)
                                    @JsonDeserialize(using = IdentityKeyAdapter.Deserializer.class)
                                    IdentityKey identityKey) {

  };

  @ParameterizedTest
  @MethodSource
  void deserialize(final String json, @Nullable final IdentityKey expectedIdentityKey) throws JsonProcessingException {
    final IdentityKeyCarrier identityKeyCarrier = SystemMapper.jsonMapper().readValue(json, IdentityKeyCarrier.class);

    assertEquals(expectedIdentityKey, identityKeyCarrier.identityKey());
  }

  private static Stream<Arguments> deserialize() {
    final String template = """
        {
          "identityKey": %s
        }
        """;

    return Stream.of(
        Arguments.of(String.format(template, "null"), null),
        Arguments.of(String.format(template, "\"\""), null),
        Arguments.of(
            String.format(template, "\"" + Base64.getEncoder().encodeToString(IDENTITY_KEY.serialize()) + "\""),
            IDENTITY_KEY)
    );
  }
}

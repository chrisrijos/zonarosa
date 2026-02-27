/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.server.util.ECPublicKeyAdapter;

public record ECPreKey(
    @Schema(description="""
        An arbitrary ID for this key, which will be provided by peers using this key to encrypt messages so the private key can be looked up.
        Should not be zero. Should be less than 2^24.
        """)
    long keyId,

    @JsonSerialize(using = ECPublicKeyAdapter.Serializer.class)
    @JsonDeserialize(using = ECPublicKeyAdapter.Deserializer.class)
    @Schema(type="string", description="""
        The public key, serialized in libzonarosa's elliptic-curve public key format and then base64-encoded.
        """)
    ECPublicKey publicKey) implements PreKey<ECPublicKey> {

  @Override
  public byte[] serializedPublicKey() {
    return publicKey().serialize();
  }
}

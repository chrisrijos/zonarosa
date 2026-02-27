package io.zonarosa.server.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.v3.oas.annotations.media.Schema;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.server.util.ECPublicKeyAdapter;

public record SetPublicKeyRequest(
    @JsonSerialize(using = ECPublicKeyAdapter.Serializer.class)
    @JsonDeserialize(using = ECPublicKeyAdapter.Deserializer.class)
    @Schema(type="string", description="""
        The public key, serialized in libzonarosa's elliptic-curve public key format and then encoded as a standard (i.e.
        not URL-safe), padded, base64-encoded string.
        """)
    ECPublicKey publicKey) {
}

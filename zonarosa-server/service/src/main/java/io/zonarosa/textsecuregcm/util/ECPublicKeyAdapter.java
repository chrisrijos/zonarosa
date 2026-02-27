/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

public class ECPublicKeyAdapter {

  public static class Serializer extends AbstractPublicKeySerializer<ECPublicKey> {

    @Override
    protected byte[] serializePublicKey(final ECPublicKey publicKey) {
      return publicKey.serialize();
    }
  }

  public static class Deserializer extends AbstractPublicKeyDeserializer<ECPublicKey> {

    @Override
    protected ECPublicKey deserializePublicKey(final byte[] publicKeyBytes) throws InvalidKeyException {
      return new ECPublicKey(publicKeyBytes);
    }
  }
}

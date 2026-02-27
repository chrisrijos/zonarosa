/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.kem.KEMPublicKey;

public class KEMPublicKeyAdapter {

  public static class Serializer extends AbstractPublicKeySerializer<KEMPublicKey> {

    @Override
    protected byte[] serializePublicKey(final KEMPublicKey publicKey) {
      return publicKey.serialize();
    }
  }

  public static class Deserializer extends AbstractPublicKeyDeserializer<KEMPublicKey> {

    @Override
    protected KEMPublicKey deserializePublicKey(final byte[] publicKeyBytes) throws InvalidKeyException {
      return new KEMPublicKey(publicKeyBytes);
    }
  }
}

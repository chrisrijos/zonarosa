/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

import io.zonarosa.libzonarosa.protocol.IdentityKey;

public interface SignedPreKey<K> extends PreKey<K> {

  byte[] signature();

  default boolean signatureValid(final IdentityKey identityKey) {
    try {
      return identityKey.getPublicKey().verifySignature(serializedPublicKey(), signature());
    } catch (final Exception e) {
      return false;
    }
  }
}

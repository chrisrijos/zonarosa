/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.entities;

public interface PreKey<K> {

  long keyId();

  K publicKey();

  byte[] serializedPublicKey();
}

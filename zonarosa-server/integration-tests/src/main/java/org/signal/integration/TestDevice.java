/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.integration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.lang3.tuple.Pair;
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;

public class TestDevice {

  private final byte deviceId;

  private final Map<Integer, Pair<IdentityKeyPair, SignedPreKeyRecord>> signedPreKeys = new ConcurrentHashMap<>();


  public static TestDevice create(
      final byte deviceId,
      final IdentityKeyPair aciIdentityKeyPair,
      final IdentityKeyPair pniIdentityKeyPair) {
    final TestDevice device = new TestDevice(deviceId);
    device.addSignedPreKey(aciIdentityKeyPair);
    device.addSignedPreKey(pniIdentityKeyPair);
    return device;
  }

  public TestDevice(final byte deviceId) {
    this.deviceId = deviceId;
  }

  public byte deviceId() {
    return deviceId;
  }

  public SignedPreKeyRecord latestSignedPreKey(final IdentityKeyPair identity) {
    final int id = signedPreKeys.entrySet()
        .stream()
        .filter(p -> p.getValue().getLeft().equals(identity))
        .mapToInt(Map.Entry::getKey)
        .max()
        .orElseThrow();
    return signedPreKeys.get(id).getRight();
  }

  public SignedPreKeyRecord addSignedPreKey(final IdentityKeyPair identity) {
    final int nextId = signedPreKeys.keySet().stream().mapToInt(k -> k + 1).max().orElse(0);
    final ECKeyPair keyPair = ECKeyPair.generate();
    final byte[] signature = keyPair.getPrivateKey().calculateSignature(keyPair.getPublicKey().serialize());
    final SignedPreKeyRecord signedPreKeyRecord = new SignedPreKeyRecord(nextId, System.currentTimeMillis(), keyPair, signature);
    signedPreKeys.put(nextId, Pair.of(identity, signedPreKeyRecord));
    return signedPreKeyRecord;
  }
}

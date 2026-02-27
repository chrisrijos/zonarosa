/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import org.junit.jupiter.api.Test;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static io.zonarosa.server.util.EncryptDeviceCreationTimestampUtil.ENCRYPTION_INFO;

public class EncryptDeviceCreationTimestampUtilTest {
  @Test
  void encryptDecrypt() throws InvalidMessageException {
    final long createdAt = System.currentTimeMillis();
    final ECKeyPair keyPair = ECKeyPair.generate();
    final byte deviceId = 1;
    final int registrationId = 123;

    final byte[] ciphertext = EncryptDeviceCreationTimestampUtil.encrypt(createdAt, new IdentityKey(keyPair.getPublicKey()),
        deviceId, registrationId);
    final ByteBuffer associatedData = ByteBuffer.allocate(5);
    associatedData.put(deviceId);
    associatedData.putInt(registrationId);

    final byte[] decryptedData = keyPair.getPrivateKey().open(ciphertext, ENCRYPTION_INFO, associatedData.array());

    assertEquals(createdAt, ByteBuffer.wrap(decryptedData).getLong());
  }
}

//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;

public class TestNoSignedPreKeysStore extends TestInMemoryZonaRosaProtocolStore {
  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    throw new InvalidKeyIdException("TestNoSignedPreKeysStore rejected loading " + signedPreKeyId);
  }
}

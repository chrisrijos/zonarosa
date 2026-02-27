//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;

public class TestBadSignedPreKeysStore extends TestInMemoryZonaRosaProtocolStore {
  public static class CustomException extends RuntimeException {
    CustomException(String message) {
      super(message);
    }
  }

  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    throw new CustomException("TestBadSignedPreKeysStore rejected loading " + signedPreKeyId);
  }
}

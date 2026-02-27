//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import static org.junit.Assert.*;

import java.util.UUID;
import org.junit.Test;
import io.zonarosa.libzonarosa.internal.NativeTesting;
import io.zonarosa.libzonarosa.keytrans.KeyTransparencyException;
import io.zonarosa.libzonarosa.keytrans.VerificationFailedException;
import io.zonarosa.libzonarosa.protocol.IdentityKey;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.protocol.util.Hex;

public class KeyTransparencyTest {
  static final ServiceId.Aci TEST_ACI =
      new ServiceId.Aci(UUID.fromString("90c979fd-eab4-4a08-b6da-69dedeab9b29"));
  static final IdentityKey TEST_ACI_IDENTITY_KEY;
  static final String TEST_E164 = "+18005550100";
  static final byte[] TEST_USERNAME_HASH =
      Hex.fromStringCondensedAssert(
          "dc711808c2cf66d5e6a33ce41f27d69d942d2e1ff4db22d39b42d2eff8d09746");
  static final byte[] TEST_UNIDENTIFIED_ACCESS_KEY =
      Hex.fromStringCondensedAssert("108d84b71be307bdf101e380a1d7f2a2");

  static {
    try {
      TEST_ACI_IDENTITY_KEY =
          new IdentityKey(
              Hex.fromStringCondensedAssert(
                  "05cdcbb178067f0ddfd258bb21d006e0aa9c7ab132d9fb5e8b027de07d947f9d0c"));
    } catch (InvalidKeyException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void canBridgeFatalError() {
    assertThrows(
        VerificationFailedException.class, NativeTesting::TESTING_KeyTransFatalVerificationFailure);
  }

  @Test
  public void canBridgeNonFatalError() {
    var exception =
        assertThrows(
            KeyTransparencyException.class,
            NativeTesting::TESTING_KeyTransNonFatalVerificationFailure);
    // Since VerificationFailedException is a subclass of KeyTransparencyException,
    // it would also satisfy assertThrows.
    assertNotEquals(VerificationFailedException.class, exception.getClass());
  }

  @Test
  public void canBridgeChatSendError() {
    assertThrows(TimeoutException.class, NativeTesting::TESTING_KeyTransChatSendError);
  }
}

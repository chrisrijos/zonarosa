//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import junit.framework.TestCase;

public class IdentityKeyTest extends TestCase {
  public void testSignAlternateKey() {
    IdentityKeyPair primary = IdentityKeyPair.generate();
    IdentityKeyPair secondary = IdentityKeyPair.generate();
    byte[] signature = secondary.signAlternateIdentity(primary.getPublicKey());
    assertTrue(secondary.getPublicKey().verifyAlternateIdentity(primary.getPublicKey(), signature));
  }
}

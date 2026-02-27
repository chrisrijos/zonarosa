/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.tests.util;

import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyPair;
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyType;
import io.zonarosa.libzonarosa.protocol.kem.KEMPublicKey;
import io.zonarosa.server.entities.ECPreKey;
import io.zonarosa.server.entities.ECSignedPreKey;
import io.zonarosa.server.entities.KEMSignedPreKey;

public final class KeysHelper {

  public static ECPreKey ecPreKey(final long id) {
    return new ECPreKey(id, ECKeyPair.generate().getPublicKey());
  }

  public static ECSignedPreKey signedECPreKey(long id, final ECKeyPair identityKeyPair) {
    final ECPublicKey pubKey = ECKeyPair.generate().getPublicKey();
    final byte[] sig = identityKeyPair.getPrivateKey().calculateSignature(pubKey.serialize());
    return new ECSignedPreKey(id, pubKey, sig);
  }

  public static KEMSignedPreKey signedKEMPreKey(long id, final ECKeyPair identityKeyPair) {
    final KEMPublicKey pubKey = KEMKeyPair.generate(KEMKeyType.KYBER_1024).getPublicKey();
    final byte[] sig = identityKeyPair.getPrivateKey().calculateSignature(pubKey.serialize());
    return new KEMSignedPreKey(id, pubKey, sig);
  }
}

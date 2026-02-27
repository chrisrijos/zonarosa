//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import java.util.Random;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyPair;
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyType;
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.util.Medium;

public final class PQXDHBundleFactory implements BundleFactory {
  @Override
  public PreKeyBundle createBundle(ZonaRosaProtocolStore store) throws InvalidKeyException {
    ECKeyPair preKeyPair = ECKeyPair.generate();
    ECKeyPair signedPreKeyPair = ECKeyPair.generate();
    byte[] signedPreKeySignature =
        store
            .getIdentityKeyPair()
            .getPrivateKey()
            .calculateSignature(signedPreKeyPair.getPublicKey().serialize());
    KEMKeyPair kyberPreKeyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024);
    byte[] kyberPreKeySignature =
        store
            .getIdentityKeyPair()
            .getPrivateKey()
            .calculateSignature(kyberPreKeyPair.getPublicKey().serialize());

    Random random = new Random();
    int preKeyId = random.nextInt(Medium.MAX_VALUE);
    int signedPreKeyId = random.nextInt(Medium.MAX_VALUE);
    int kyberPreKeyId = random.nextInt(Medium.MAX_VALUE);
    store.storePreKey(preKeyId, new PreKeyRecord(preKeyId, preKeyPair));
    store.storeSignedPreKey(
        signedPreKeyId,
        new SignedPreKeyRecord(
            signedPreKeyId, System.currentTimeMillis(), signedPreKeyPair, signedPreKeySignature));
    store.storeKyberPreKey(
        kyberPreKeyId,
        new KyberPreKeyRecord(
            kyberPreKeyId, System.currentTimeMillis(), kyberPreKeyPair, kyberPreKeySignature));

    return new PreKeyBundle(
        store.getLocalRegistrationId(),
        1,
        preKeyId,
        preKeyPair.getPublicKey(),
        signedPreKeyId,
        signedPreKeyPair.getPublicKey(),
        signedPreKeySignature,
        store.getIdentityKeyPair().getPublicKey(),
        kyberPreKeyId,
        kyberPreKeyPair.getPublicKey(),
        kyberPreKeySignature);
  }
}

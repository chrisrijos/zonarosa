/**
 * Copyright (C) 2014-2016 ZonaRosa Systems
 *
 * Licensed according to the LICENSE file in this repository.
 */

package io.zonarosa.service.internal.crypto;

import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.kdf.HKDF;
import io.zonarosa.registration.proto.RegistrationProvisionEnvelope;
import io.zonarosa.registration.proto.RegistrationProvisionMessage;
import io.zonarosa.service.internal.push.ProvisionEnvelope;
import io.zonarosa.service.internal.push.ProvisionMessage;
import io.zonarosa.service.internal.util.Util;

import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import okio.ByteString;

public class PrimaryProvisioningCipher {

  public static final String PROVISIONING_MESSAGE = "ZonaRosa Provisioning Message";

  private final ECPublicKey theirPublicKey;

  public PrimaryProvisioningCipher(ECPublicKey theirPublicKey) {
    this.theirPublicKey = theirPublicKey;
  }

  public byte[] encrypt(ProvisionMessage message) throws InvalidKeyException {
    ECKeyPair ourKeyPair    = ECKeyPair.generate();
    byte[]    sharedSecret  = ourKeyPair.getPrivateKey().calculateAgreement(theirPublicKey);
    byte[]    derivedSecret = HKDF.deriveSecrets(sharedSecret, PROVISIONING_MESSAGE.getBytes(), 64);
    byte[][]  parts         = Util.split(derivedSecret, 32, 32);

    byte[] version    = {0x01};
    byte[] ciphertext = getCiphertext(parts[0], message.encode());
    byte[] mac        = getMac(parts[1], Util.join(version, ciphertext));
    byte[] body       = Util.join(version, ciphertext, mac);

    return new ProvisionEnvelope.Builder()
                                .publicKey(ByteString.of(ourKeyPair.getPublicKey().serialize()))
                                .body(ByteString.of(body))
                                .build()
                                .encode();
  }

  public byte[] encrypt(RegistrationProvisionMessage message) throws InvalidKeyException {
    ECKeyPair ourKeyPair    = ECKeyPair.generate();
    byte[]    sharedSecret  = ourKeyPair.getPrivateKey().calculateAgreement(theirPublicKey);
    byte[]    derivedSecret = HKDF.deriveSecrets(sharedSecret, PROVISIONING_MESSAGE.getBytes(), 64);
    byte[][]  parts         = Util.split(derivedSecret, 32, 32);

    byte[] version    = { 0x01 };
    byte[] ciphertext = getCiphertext(parts[0], message.encode());
    byte[] mac        = getMac(parts[1], Util.join(version, ciphertext));
    byte[] body       = Util.join(version, ciphertext, mac);

    return new RegistrationProvisionEnvelope.Builder()
                                            .publicKey(ByteString.of(ourKeyPair.getPublicKey().serialize()))
                                            .body(ByteString.of(body))
                                            .build()
                                            .encode();
  }

  private byte[] getCiphertext(byte[] key, byte[] message) {
    try {
      Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
      cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));

      return Util.join(cipher.getIV(), cipher.doFinal(message));
    } catch (NoSuchAlgorithmException | NoSuchPaddingException | java.security.InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      throw new AssertionError(e);
    }
  }

  private byte[] getMac(byte[] key, byte[] message) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));

      return mac.doFinal(message);
    } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
      throw new AssertionError(e);
    }
  }
}

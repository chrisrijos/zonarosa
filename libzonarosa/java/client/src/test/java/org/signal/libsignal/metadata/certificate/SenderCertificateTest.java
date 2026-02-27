//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.metadata.certificate;

import java.util.Optional;
import java.util.UUID;
import junit.framework.TestCase;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ServiceId;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;

public class SenderCertificateTest extends TestCase {

  private final ECKeyPair trustRoot = ECKeyPair.generate();

  public void testSignature() throws InvalidCertificateException, InvalidKeyException {
    ECKeyPair key = ECKeyPair.generate();
    SenderCertificate senderCertificate =
        createCertificateFor(
            trustRoot,
            UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f"),
            "+14151111111",
            1,
            key.getPublicKey(),
            31337);

    new CertificateValidator(trustRoot.getPublicKey()).validate(senderCertificate, 31336);
  }

  public void testExpiredSignature() throws InvalidCertificateException, InvalidKeyException {
    ECKeyPair key = ECKeyPair.generate();

    SenderCertificate senderCertificate =
        createCertificateFor(
            trustRoot,
            UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f"),
            "+14151111111",
            2,
            key.getPublicKey(),
            31337);
    try {
      new CertificateValidator(trustRoot.getPublicKey()).validate(senderCertificate, 31338);
      throw new AssertionError();
    } catch (InvalidCertificateException e) {
      // good
    }
  }

  public void testBadSignature() throws InvalidCertificateException, InvalidKeyException {
    ECKeyPair key = ECKeyPair.generate();

    SenderCertificate senderCertificate =
        createCertificateFor(
            trustRoot,
            UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f"),
            "+14151111111",
            3,
            key.getPublicKey(),
            31337);

    byte[] badSignature = senderCertificate.getSerialized();

    badSignature[badSignature.length - 1] ^= 1;

    SenderCertificate badCert = new SenderCertificate(badSignature);

    try {
      new CertificateValidator(trustRoot.getPublicKey()).validate(badCert, 31336);
      throw new AssertionError();
    } catch (InvalidCertificateException e) {
      // good
    }
  }

  public void testGetSenderAci()
      throws InvalidCertificateException, InvalidKeyException, ServiceId.InvalidServiceIdException {
    ECKeyPair key = ECKeyPair.generate();
    UUID uuid = UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f");
    SenderCertificate senderCertificate =
        createCertificateFor(trustRoot, uuid, null, 4, key.getPublicKey(), 31337);
    assertEquals(Optional.empty(), senderCertificate.getSenderE164());
    assertEquals(uuid, senderCertificate.getSenderAci().getRawUUID());
  }

  private SenderCertificate createCertificateFor(
      ECKeyPair trustRoot,
      UUID uuid,
      String e164,
      int deviceId,
      ECPublicKey identityKey,
      long expires)
      throws InvalidKeyException, InvalidCertificateException {
    ECKeyPair serverKey = ECKeyPair.generate();
    ServerCertificate serverCertificate =
        new ServerCertificate(trustRoot.getPrivateKey(), 1, serverKey.getPublicKey());
    return serverCertificate.issue(
        serverKey.getPrivateKey(),
        uuid.toString(),
        Optional.ofNullable(e164),
        deviceId,
        identityKey,
        expires);
  }
}

//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import androidx.benchmark.BenchmarkState;
import androidx.benchmark.junit4.BenchmarkRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import io.zonarosa.libzonarosa.metadata.SealedSessionCipher;
import io.zonarosa.libzonarosa.metadata.certificate.InvalidCertificateException;
import io.zonarosa.libzonarosa.metadata.certificate.SenderCertificate;
import io.zonarosa.libzonarosa.metadata.certificate.ServerCertificate;
import io.zonarosa.libzonarosa.metadata.protocol.UnidentifiedSenderMessageContent;
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair;
import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.SessionBuilder;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.UntrustedIdentityException;
import io.zonarosa.libzonarosa.protocol.ecc.ECKeyPair;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.libzonarosa.protocol.groups.GroupCipher;
import io.zonarosa.libzonarosa.protocol.groups.GroupSessionBuilder;
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyPair;
import io.zonarosa.libzonarosa.protocol.kem.KEMKeyType;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;
import io.zonarosa.libzonarosa.protocol.message.SenderKeyDistributionMessage;
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.impl.InMemoryZonaRosaProtocolStore;

@RunWith(Enclosed.class)
public class SealedSender {
  public static class V1 {
    @Rule public final BenchmarkRule benchmarkRule = new BenchmarkRule();

    @Test
    public void sealedSenderV1Encrypt() throws Exception {
      InMemoryZonaRosaProtocolStore aliceStore =
          new InMemoryZonaRosaProtocolStore(IdentityKeyPair.generate(), 0xAA);
      InMemoryZonaRosaProtocolStore bobStore =
          new InMemoryZonaRosaProtocolStore(IdentityKeyPair.generate(), 0xBB);
      ZonaRosaProtocolAddress bobAddress = new ZonaRosaProtocolAddress("+14152222222", 1);

      initializeSessions(aliceStore, bobStore, bobAddress);

      ECKeyPair trustRoot = ECKeyPair.generate();
      SenderCertificate senderCertificate =
          createCertificateFor(
              trustRoot,
              UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f"),
              "+14151111111",
              1,
              aliceStore.getIdentityKeyPair().getPublicKey().getPublicKey(),
              31337);
      SealedSessionCipher aliceCipher =
          new SealedSessionCipher(
              aliceStore,
              UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f"),
              "+14151111111",
              1);

      final BenchmarkState state = benchmarkRule.getState();
      while (state.keepRunning()) {
        aliceCipher.encrypt(bobAddress, senderCertificate, "smert za smert".getBytes());
      }
    }
  }

  @RunWith(Parameterized.class)
  public static class V2 {
    @Parameterized.Parameters(name = "recipients={0}")
    public static Object[] multiRecipientSizes() {
      return new Integer[] {10, 100, 1000};
    }

    @Rule public final BenchmarkRule benchmarkRule = new BenchmarkRule();

    final InMemoryZonaRosaProtocolStore aliceStore =
        new InMemoryZonaRosaProtocolStore(IdentityKeyPair.generate(), 0xAA);
    final ZonaRosaProtocolAddress aliceAddress = new ZonaRosaProtocolAddress("+14151111111", 1);
    final List<ZonaRosaProtocolAddress> recipients;

    public V2(int recipientCount) {
      recipients = new ArrayList<>(recipientCount);
      for (int i = 0; i < recipientCount; ++i) {
        InMemoryZonaRosaProtocolStore bobStore =
            new InMemoryZonaRosaProtocolStore(IdentityKeyPair.generate(), i);
        ZonaRosaProtocolAddress bobAddress =
            new ZonaRosaProtocolAddress(UUID.randomUUID().toString(), i % 127 + 1);
        filterExceptions(() -> initializeSessions(aliceStore, bobStore, bobAddress));
        recipients.add(bobAddress);
      }
    }

    @Test
    public void sealedSenderV2Encrypt() throws Exception {
      GroupCipher aliceGroupCipher = new GroupCipher(aliceStore, aliceAddress);
      UUID distributionId = UUID.randomUUID();

      SenderKeyDistributionMessage sentAliceDistributionMessage =
          new GroupSessionBuilder(aliceStore).create(aliceAddress, distributionId);

      CiphertextMessage ciphertextFromAlice =
          aliceGroupCipher.encrypt(distributionId, "smert ze smert".getBytes());

      ECKeyPair trustRoot = ECKeyPair.generate();
      SenderCertificate senderCertificate =
          createCertificateFor(
              trustRoot,
              UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f"),
              "+14151111111",
              1,
              aliceStore.getIdentityKeyPair().getPublicKey().getPublicKey(),
              31337);
      UnidentifiedSenderMessageContent content =
          new UnidentifiedSenderMessageContent(
              ciphertextFromAlice,
              senderCertificate,
              UnidentifiedSenderMessageContent.CONTENT_HINT_DEFAULT,
              Optional.empty());
      SealedSessionCipher aliceCipher =
          new SealedSessionCipher(
              aliceStore,
              UUID.fromString("9d0652a3-dcc3-4d11-975f-74d61598733f"),
              "+14151111111",
              1);

      final BenchmarkState state = benchmarkRule.getState();
      while (state.keepRunning()) {
        aliceCipher.multiRecipientEncrypt(recipients, content);
      }
    }
  }

  // Copied from SealedSessionCipherTest.java

  private static SignedPreKeyRecord generateSignedPreKey(
      IdentityKeyPair identityKeyPair, int signedPreKeyId) throws InvalidKeyException {
    ECKeyPair keyPair = ECKeyPair.generate();
    byte[] signature =
        identityKeyPair.getPrivateKey().calculateSignature(keyPair.getPublicKey().serialize());

    return new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), keyPair, signature);
  }

  private static KyberPreKeyRecord generateKyberPreKey(
      IdentityKeyPair identityKeyPair, int kyberPreKeyId) throws InvalidKeyException {
    KEMKeyPair keyPair = KEMKeyPair.generate(KEMKeyType.KYBER_1024);
    byte[] signature =
        identityKeyPair.getPrivateKey().calculateSignature(keyPair.getPublicKey().serialize());

    return new KyberPreKeyRecord(kyberPreKeyId, System.currentTimeMillis(), keyPair, signature);
  }

  private static SenderCertificate createCertificateFor(
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

  private static void initializeSessions(
      InMemoryZonaRosaProtocolStore aliceStore,
      InMemoryZonaRosaProtocolStore bobStore,
      ZonaRosaProtocolAddress bobAddress)
      throws InvalidKeyException, UntrustedIdentityException {
    ECKeyPair bobPreKey = ECKeyPair.generate();
    IdentityKeyPair bobIdentityKey = bobStore.getIdentityKeyPair();
    SignedPreKeyRecord bobSignedPreKey = generateSignedPreKey(bobIdentityKey, 2);
    KyberPreKeyRecord bobKyberPreKey = generateKyberPreKey(bobIdentityKey, 12);

    PreKeyBundle bobBundle =
        new PreKeyBundle(
            1,
            1,
            1,
            bobPreKey.getPublicKey(),
            2,
            bobSignedPreKey.getKeyPair().getPublicKey(),
            bobSignedPreKey.getSignature(),
            bobIdentityKey.getPublicKey(),
            12,
            bobKyberPreKey.getKeyPair().getPublicKey(),
            bobKyberPreKey.getSignature());
    SessionBuilder aliceSessionBuilder = new SessionBuilder(aliceStore, bobAddress);
    aliceSessionBuilder.process(bobBundle);

    bobStore.storeSignedPreKey(2, bobSignedPreKey);
    bobStore.storeKyberPreKey(12, bobKyberPreKey);
    bobStore.storePreKey(1, new PreKeyRecord(1, bobPreKey));
  }
}

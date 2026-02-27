//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;
import static io.zonarosa.libzonarosa.protocol.SessionRecordTest.getAliceBaseKey;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import io.zonarosa.libzonarosa.protocol.message.CiphertextMessage;
import io.zonarosa.libzonarosa.protocol.message.PreKeyZonaRosaMessage;
import io.zonarosa.libzonarosa.protocol.message.ZonaRosaMessage;
import io.zonarosa.libzonarosa.protocol.state.PreKeyBundle;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;

@RunWith(Parameterized.class)
public class SimultaneousInitiateTests {

  private static final ZonaRosaProtocolAddress BOB_ADDRESS =
      filterExceptions(() -> new ZonaRosaProtocolAddress("+14151231234", 1));
  private static final ZonaRosaProtocolAddress ALICE_ADDRESS =
      filterExceptions(() -> new ZonaRosaProtocolAddress("+14159998888", 1));

  private final BundleFactory bundleFactory;
  private int expectedVersion;

  public SimultaneousInitiateTests(BundleFactory bundleFactory, int expectedVersion) {
    this.bundleFactory = bundleFactory;
    this.expectedVersion = expectedVersion;
  }

  @Parameters(name = "v{1}")
  public static Collection<Object[]> data() throws Exception {
    return Arrays.asList(new Object[][] {{new PQXDHBundleFactory(), 4}});
  }

  @Test
  public void testBasicSimultaneousInitiate()
      throws InvalidKeyException,
          UntrustedIdentityException,
          InvalidVersionException,
          InvalidMessageException,
          DuplicateMessageException,
          LegacyMessageException,
          InvalidKeyIdException,
          NoSessionException {
    ZonaRosaProtocolStore aliceStore = new TestInMemoryZonaRosaProtocolStore();
    ZonaRosaProtocolStore bobStore = new TestInMemoryZonaRosaProtocolStore();

    PreKeyBundle alicePreKeyBundle = bundleFactory.createBundle(aliceStore);
    PreKeyBundle bobPreKeyBundle = bundleFactory.createBundle(bobStore);

    SessionBuilder aliceSessionBuilder = new SessionBuilder(aliceStore, BOB_ADDRESS);
    SessionBuilder bobSessionBuilder = new SessionBuilder(bobStore, ALICE_ADDRESS);

    SessionCipher aliceSessionCipher = new SessionCipher(aliceStore, BOB_ADDRESS);
    SessionCipher bobSessionCipher = new SessionCipher(bobStore, ALICE_ADDRESS);

    aliceSessionBuilder.process(bobPreKeyBundle);
    bobSessionBuilder.process(alicePreKeyBundle);

    CiphertextMessage messageForBob = aliceSessionCipher.encrypt("hey there".getBytes());
    CiphertextMessage messageForAlice = bobSessionCipher.encrypt("sample message".getBytes());

    assertEquals(messageForBob.getType(), CiphertextMessage.PREKEY_TYPE);
    assertEquals(messageForAlice.getType(), CiphertextMessage.PREKEY_TYPE);

    assertSessionIdNotEquals(aliceStore, bobStore);
    assertSessionIdNotEquals(aliceStore, bobStore);

    byte[] alicePlaintext =
        aliceSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForAlice.serialize()));
    byte[] bobPlaintext =
        bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForBob.serialize()));

    assertTrue(new String(alicePlaintext).equals("sample message"));
    assertTrue(new String(bobPlaintext).equals("hey there"));

    assertTrue(aliceStore.loadSession(BOB_ADDRESS).getSessionVersion() == expectedVersion);
    assertTrue(bobStore.loadSession(ALICE_ADDRESS).getSessionVersion() == expectedVersion);

    assertSessionIdNotEquals(aliceStore, bobStore);

    CiphertextMessage aliceResponse = aliceSessionCipher.encrypt("second message".getBytes());

    assertEquals(aliceResponse.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] responsePlaintext =
        bobSessionCipher.decrypt(new ZonaRosaMessage(aliceResponse.serialize()));

    assertTrue(new String(responsePlaintext).equals("second message"));
    assertSessionIdEquals(aliceStore, bobStore);

    CiphertextMessage finalMessage = bobSessionCipher.encrypt("third message".getBytes());

    assertEquals(finalMessage.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] finalPlaintext = aliceSessionCipher.decrypt(new ZonaRosaMessage(finalMessage.serialize()));

    assertTrue(new String(finalPlaintext).equals("third message"));
    assertSessionIdEquals(aliceStore, bobStore);
  }

  @Test
  public void testLostSimultaneousInitiate()
      throws InvalidKeyException,
          UntrustedIdentityException,
          InvalidVersionException,
          InvalidMessageException,
          DuplicateMessageException,
          LegacyMessageException,
          InvalidKeyIdException,
          NoSessionException {
    ZonaRosaProtocolStore aliceStore = new TestInMemoryZonaRosaProtocolStore();
    ZonaRosaProtocolStore bobStore = new TestInMemoryZonaRosaProtocolStore();

    PreKeyBundle alicePreKeyBundle = bundleFactory.createBundle(aliceStore);
    PreKeyBundle bobPreKeyBundle = bundleFactory.createBundle(bobStore);

    SessionBuilder aliceSessionBuilder = new SessionBuilder(aliceStore, BOB_ADDRESS);
    SessionBuilder bobSessionBuilder = new SessionBuilder(bobStore, ALICE_ADDRESS);

    SessionCipher aliceSessionCipher = new SessionCipher(aliceStore, BOB_ADDRESS);
    SessionCipher bobSessionCipher = new SessionCipher(bobStore, ALICE_ADDRESS);

    aliceSessionBuilder.process(bobPreKeyBundle);
    bobSessionBuilder.process(alicePreKeyBundle);

    CiphertextMessage messageForBob = aliceSessionCipher.encrypt("hey there".getBytes());
    CiphertextMessage messageForAlice = bobSessionCipher.encrypt("sample message".getBytes());

    assertEquals(messageForBob.getType(), CiphertextMessage.PREKEY_TYPE);
    assertEquals(messageForAlice.getType(), CiphertextMessage.PREKEY_TYPE);

    assertSessionIdNotEquals(aliceStore, bobStore);

    byte[] bobPlaintext =
        bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForBob.serialize()));

    assertTrue(new String(bobPlaintext).equals("hey there"));
    assertEquals(bobStore.loadSession(ALICE_ADDRESS).getSessionVersion(), expectedVersion);

    CiphertextMessage aliceResponse = aliceSessionCipher.encrypt("second message".getBytes());

    assertEquals(aliceResponse.getType(), CiphertextMessage.PREKEY_TYPE);

    byte[] responsePlaintext =
        bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(aliceResponse.serialize()));

    assertTrue(new String(responsePlaintext).equals("second message"));
    assertSessionIdEquals(aliceStore, bobStore);

    CiphertextMessage finalMessage = bobSessionCipher.encrypt("third message".getBytes());

    assertEquals(finalMessage.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] finalPlaintext = aliceSessionCipher.decrypt(new ZonaRosaMessage(finalMessage.serialize()));

    assertTrue(new String(finalPlaintext).equals("third message"));
    assertSessionIdEquals(aliceStore, bobStore);
  }

  @Test
  public void testSimultaneousInitiateLostMessage()
      throws InvalidKeyException,
          UntrustedIdentityException,
          InvalidVersionException,
          InvalidMessageException,
          DuplicateMessageException,
          LegacyMessageException,
          InvalidKeyIdException,
          NoSessionException {
    ZonaRosaProtocolStore aliceStore = new TestInMemoryZonaRosaProtocolStore();
    ZonaRosaProtocolStore bobStore = new TestInMemoryZonaRosaProtocolStore();

    PreKeyBundle alicePreKeyBundle = bundleFactory.createBundle(aliceStore);
    PreKeyBundle bobPreKeyBundle = bundleFactory.createBundle(bobStore);

    SessionBuilder aliceSessionBuilder = new SessionBuilder(aliceStore, BOB_ADDRESS);
    SessionBuilder bobSessionBuilder = new SessionBuilder(bobStore, ALICE_ADDRESS);

    SessionCipher aliceSessionCipher = new SessionCipher(aliceStore, BOB_ADDRESS);
    SessionCipher bobSessionCipher = new SessionCipher(bobStore, ALICE_ADDRESS);

    aliceSessionBuilder.process(bobPreKeyBundle);
    bobSessionBuilder.process(alicePreKeyBundle);

    CiphertextMessage messageForBob = aliceSessionCipher.encrypt("hey there".getBytes());
    CiphertextMessage messageForAlice = bobSessionCipher.encrypt("sample message".getBytes());

    assertEquals(messageForBob.getType(), CiphertextMessage.PREKEY_TYPE);
    assertEquals(messageForAlice.getType(), CiphertextMessage.PREKEY_TYPE);

    assertSessionIdNotEquals(aliceStore, bobStore);

    byte[] alicePlaintext =
        aliceSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForAlice.serialize()));
    byte[] bobPlaintext =
        bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForBob.serialize()));

    assertTrue(new String(alicePlaintext).equals("sample message"));
    assertTrue(new String(bobPlaintext).equals("hey there"));

    assertEquals(aliceStore.loadSession(BOB_ADDRESS).getSessionVersion(), expectedVersion);
    assertEquals(bobStore.loadSession(ALICE_ADDRESS).getSessionVersion(), expectedVersion);

    assertSessionIdNotEquals(aliceStore, bobStore);

    CiphertextMessage aliceResponse = aliceSessionCipher.encrypt("second message".getBytes());

    assertEquals(aliceResponse.getType(), CiphertextMessage.WHISPER_TYPE);

    assertSessionIdNotEquals(aliceStore, bobStore);

    CiphertextMessage finalMessage = bobSessionCipher.encrypt("third message".getBytes());

    assertEquals(finalMessage.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] finalPlaintext = aliceSessionCipher.decrypt(new ZonaRosaMessage(finalMessage.serialize()));

    assertTrue(new String(finalPlaintext).equals("third message"));
    assertSessionIdEquals(aliceStore, bobStore);
  }

  @Test
  public void testSimultaneousInitiateRepeatedMessages()
      throws InvalidKeyException,
          UntrustedIdentityException,
          InvalidVersionException,
          InvalidMessageException,
          DuplicateMessageException,
          LegacyMessageException,
          InvalidKeyIdException,
          NoSessionException {
    ZonaRosaProtocolStore aliceStore = new TestInMemoryZonaRosaProtocolStore();
    ZonaRosaProtocolStore bobStore = new TestInMemoryZonaRosaProtocolStore();

    PreKeyBundle alicePreKeyBundle = bundleFactory.createBundle(aliceStore);
    PreKeyBundle bobPreKeyBundle = bundleFactory.createBundle(bobStore);

    SessionBuilder aliceSessionBuilder = new SessionBuilder(aliceStore, BOB_ADDRESS);
    SessionBuilder bobSessionBuilder = new SessionBuilder(bobStore, ALICE_ADDRESS);

    SessionCipher aliceSessionCipher = new SessionCipher(aliceStore, BOB_ADDRESS);
    SessionCipher bobSessionCipher = new SessionCipher(bobStore, ALICE_ADDRESS);

    aliceSessionBuilder.process(bobPreKeyBundle);
    bobSessionBuilder.process(alicePreKeyBundle);

    CiphertextMessage messageForBob = aliceSessionCipher.encrypt("hey there".getBytes());
    CiphertextMessage messageForAlice = bobSessionCipher.encrypt("sample message".getBytes());

    assertEquals(messageForBob.getType(), CiphertextMessage.PREKEY_TYPE);
    assertEquals(messageForAlice.getType(), CiphertextMessage.PREKEY_TYPE);

    assertSessionIdNotEquals(aliceStore, bobStore);

    byte[] alicePlaintext =
        aliceSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForAlice.serialize()));
    byte[] bobPlaintext =
        bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForBob.serialize()));

    assertTrue(new String(alicePlaintext).equals("sample message"));
    assertTrue(new String(bobPlaintext).equals("hey there"));

    assertEquals(aliceStore.loadSession(BOB_ADDRESS).getSessionVersion(), expectedVersion);
    assertEquals(bobStore.loadSession(ALICE_ADDRESS).getSessionVersion(), expectedVersion);

    assertSessionIdNotEquals(aliceStore, bobStore);

    for (int i = 0; i < 50; i++) {
      CiphertextMessage messageForBobRepeat = aliceSessionCipher.encrypt("hey there".getBytes());
      CiphertextMessage messageForAliceRepeat =
          bobSessionCipher.encrypt("sample message".getBytes());

      assertEquals(messageForBobRepeat.getType(), CiphertextMessage.WHISPER_TYPE);
      assertEquals(messageForAliceRepeat.getType(), CiphertextMessage.WHISPER_TYPE);

      assertSessionIdNotEquals(aliceStore, bobStore);

      byte[] alicePlaintextRepeat =
          aliceSessionCipher.decrypt(new ZonaRosaMessage(messageForAliceRepeat.serialize()));
      byte[] bobPlaintextRepeat =
          bobSessionCipher.decrypt(new ZonaRosaMessage(messageForBobRepeat.serialize()));

      assertTrue(new String(alicePlaintextRepeat).equals("sample message"));
      assertTrue(new String(bobPlaintextRepeat).equals("hey there"));

      assertSessionIdNotEquals(aliceStore, bobStore);
    }

    CiphertextMessage aliceResponse = aliceSessionCipher.encrypt("second message".getBytes());

    assertEquals(aliceResponse.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] responsePlaintext =
        bobSessionCipher.decrypt(new ZonaRosaMessage(aliceResponse.serialize()));

    assertTrue(new String(responsePlaintext).equals("second message"));
    assertSessionIdEquals(aliceStore, bobStore);

    CiphertextMessage finalMessage = bobSessionCipher.encrypt("third message".getBytes());

    assertEquals(finalMessage.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] finalPlaintext = aliceSessionCipher.decrypt(new ZonaRosaMessage(finalMessage.serialize()));

    assertTrue(new String(finalPlaintext).equals("third message"));
    assertSessionIdEquals(aliceStore, bobStore);
  }

  @Test
  public void testRepeatedSimultaneousInitiateRepeatedMessages()
      throws InvalidKeyException,
          UntrustedIdentityException,
          InvalidVersionException,
          InvalidMessageException,
          DuplicateMessageException,
          LegacyMessageException,
          InvalidKeyIdException,
          NoSessionException {
    ZonaRosaProtocolStore aliceStore = new TestInMemoryZonaRosaProtocolStore();
    ZonaRosaProtocolStore bobStore = new TestInMemoryZonaRosaProtocolStore();

    SessionBuilder aliceSessionBuilder = new SessionBuilder(aliceStore, BOB_ADDRESS);
    SessionBuilder bobSessionBuilder = new SessionBuilder(bobStore, ALICE_ADDRESS);

    SessionCipher aliceSessionCipher = new SessionCipher(aliceStore, BOB_ADDRESS);
    SessionCipher bobSessionCipher = new SessionCipher(bobStore, ALICE_ADDRESS);

    for (int i = 0; i < 15; i++) {
      PreKeyBundle alicePreKeyBundle = bundleFactory.createBundle(aliceStore);
      PreKeyBundle bobPreKeyBundle = bundleFactory.createBundle(bobStore);

      aliceSessionBuilder.process(bobPreKeyBundle);
      bobSessionBuilder.process(alicePreKeyBundle);

      CiphertextMessage messageForBob = aliceSessionCipher.encrypt("hey there".getBytes());
      CiphertextMessage messageForAlice = bobSessionCipher.encrypt("sample message".getBytes());

      assertEquals(messageForBob.getType(), CiphertextMessage.PREKEY_TYPE);
      assertEquals(messageForAlice.getType(), CiphertextMessage.PREKEY_TYPE);

      assertSessionIdNotEquals(aliceStore, bobStore);

      byte[] alicePlaintext =
          aliceSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForAlice.serialize()));
      byte[] bobPlaintext =
          bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForBob.serialize()));

      assertTrue(new String(alicePlaintext).equals("sample message"));
      assertTrue(new String(bobPlaintext).equals("hey there"));

      assertEquals(aliceStore.loadSession(BOB_ADDRESS).getSessionVersion(), expectedVersion);
      assertEquals(bobStore.loadSession(ALICE_ADDRESS).getSessionVersion(), expectedVersion);

      assertFalse(isSessionIdEqual(aliceStore, bobStore));
    }

    for (int i = 0; i < 50; i++) {
      CiphertextMessage messageForBobRepeat = aliceSessionCipher.encrypt("hey there".getBytes());
      CiphertextMessage messageForAliceRepeat =
          bobSessionCipher.encrypt("sample message".getBytes());

      assertEquals(messageForBobRepeat.getType(), CiphertextMessage.WHISPER_TYPE);
      assertEquals(messageForAliceRepeat.getType(), CiphertextMessage.WHISPER_TYPE);

      assertFalse(isSessionIdEqual(aliceStore, bobStore));

      byte[] alicePlaintextRepeat =
          aliceSessionCipher.decrypt(new ZonaRosaMessage(messageForAliceRepeat.serialize()));
      byte[] bobPlaintextRepeat =
          bobSessionCipher.decrypt(new ZonaRosaMessage(messageForBobRepeat.serialize()));

      assertTrue(new String(alicePlaintextRepeat).equals("sample message"));
      assertTrue(new String(bobPlaintextRepeat).equals("hey there"));

      assertFalse(isSessionIdEqual(aliceStore, bobStore));
    }

    CiphertextMessage aliceResponse = aliceSessionCipher.encrypt("second message".getBytes());

    assertEquals(aliceResponse.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] responsePlaintext =
        bobSessionCipher.decrypt(new ZonaRosaMessage(aliceResponse.serialize()));

    assertTrue(new String(responsePlaintext).equals("second message"));
    assertTrue(isSessionIdEqual(aliceStore, bobStore));

    CiphertextMessage finalMessage = bobSessionCipher.encrypt("third message".getBytes());

    assertEquals(finalMessage.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] finalPlaintext = aliceSessionCipher.decrypt(new ZonaRosaMessage(finalMessage.serialize()));

    assertTrue(new String(finalPlaintext).equals("third message"));
    assertTrue(isSessionIdEqual(aliceStore, bobStore));
  }

  @Test
  public void testRepeatedSimultaneousInitiateLostMessageRepeatedMessages()
      throws InvalidKeyException,
          UntrustedIdentityException,
          InvalidVersionException,
          InvalidMessageException,
          DuplicateMessageException,
          LegacyMessageException,
          InvalidKeyIdException,
          NoSessionException {
    ZonaRosaProtocolStore aliceStore = new TestInMemoryZonaRosaProtocolStore();
    ZonaRosaProtocolStore bobStore = new TestInMemoryZonaRosaProtocolStore();

    SessionBuilder aliceSessionBuilder = new SessionBuilder(aliceStore, BOB_ADDRESS);
    SessionBuilder bobSessionBuilder = new SessionBuilder(bobStore, ALICE_ADDRESS);

    SessionCipher aliceSessionCipher = new SessionCipher(aliceStore, BOB_ADDRESS);
    SessionCipher bobSessionCipher = new SessionCipher(bobStore, ALICE_ADDRESS);

    PreKeyBundle bobLostPreKeyBundle = bundleFactory.createBundle(bobStore);

    aliceSessionBuilder.process(bobLostPreKeyBundle);

    CiphertextMessage lostMessageForBob = aliceSessionCipher.encrypt("hey there".getBytes());

    for (int i = 0; i < 15; i++) {
      PreKeyBundle alicePreKeyBundle = bundleFactory.createBundle(aliceStore);
      PreKeyBundle bobPreKeyBundle = bundleFactory.createBundle(bobStore);

      aliceSessionBuilder.process(bobPreKeyBundle);
      bobSessionBuilder.process(alicePreKeyBundle);

      CiphertextMessage messageForBob = aliceSessionCipher.encrypt("hey there".getBytes());
      CiphertextMessage messageForAlice = bobSessionCipher.encrypt("sample message".getBytes());

      assertEquals(messageForBob.getType(), CiphertextMessage.PREKEY_TYPE);
      assertEquals(messageForAlice.getType(), CiphertextMessage.PREKEY_TYPE);

      assertFalse(isSessionIdEqual(aliceStore, bobStore));

      byte[] alicePlaintext =
          aliceSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForAlice.serialize()));
      byte[] bobPlaintext =
          bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(messageForBob.serialize()));

      assertTrue(new String(alicePlaintext).equals("sample message"));
      assertTrue(new String(bobPlaintext).equals("hey there"));

      assertEquals(aliceStore.loadSession(BOB_ADDRESS).getSessionVersion(), expectedVersion);
      assertEquals(bobStore.loadSession(ALICE_ADDRESS).getSessionVersion(), expectedVersion);

      assertFalse(isSessionIdEqual(aliceStore, bobStore));
    }

    for (int i = 0; i < 50; i++) {
      CiphertextMessage messageForBobRepeat = aliceSessionCipher.encrypt("hey there".getBytes());
      CiphertextMessage messageForAliceRepeat =
          bobSessionCipher.encrypt("sample message".getBytes());

      assertEquals(messageForBobRepeat.getType(), CiphertextMessage.WHISPER_TYPE);
      assertEquals(messageForAliceRepeat.getType(), CiphertextMessage.WHISPER_TYPE);

      assertFalse(isSessionIdEqual(aliceStore, bobStore));

      byte[] alicePlaintextRepeat =
          aliceSessionCipher.decrypt(new ZonaRosaMessage(messageForAliceRepeat.serialize()));
      byte[] bobPlaintextRepeat =
          bobSessionCipher.decrypt(new ZonaRosaMessage(messageForBobRepeat.serialize()));

      assertTrue(new String(alicePlaintextRepeat).equals("sample message"));
      assertTrue(new String(bobPlaintextRepeat).equals("hey there"));

      assertFalse(isSessionIdEqual(aliceStore, bobStore));
    }

    CiphertextMessage aliceResponse = aliceSessionCipher.encrypt("second message".getBytes());

    assertEquals(aliceResponse.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] responsePlaintext =
        bobSessionCipher.decrypt(new ZonaRosaMessage(aliceResponse.serialize()));

    assertTrue(new String(responsePlaintext).equals("second message"));
    assertTrue(isSessionIdEqual(aliceStore, bobStore));

    CiphertextMessage finalMessage = bobSessionCipher.encrypt("third message".getBytes());

    assertEquals(finalMessage.getType(), CiphertextMessage.WHISPER_TYPE);

    byte[] finalPlaintext = aliceSessionCipher.decrypt(new ZonaRosaMessage(finalMessage.serialize()));

    assertTrue(new String(finalPlaintext).equals("third message"));
    assertTrue(isSessionIdEqual(aliceStore, bobStore));

    byte[] lostMessagePlaintext =
        bobSessionCipher.decrypt(new PreKeyZonaRosaMessage(lostMessageForBob.serialize()));
    assertTrue(new String(lostMessagePlaintext).equals("hey there"));

    assertFalse(isSessionIdEqual(aliceStore, bobStore));

    CiphertextMessage blastFromThePast = bobSessionCipher.encrypt("unexpected!".getBytes());
    byte[] blastFromThePastPlaintext =
        aliceSessionCipher.decrypt(new ZonaRosaMessage(blastFromThePast.serialize()));

    assertTrue(new String(blastFromThePastPlaintext).equals("unexpected!"));
    assertTrue(isSessionIdEqual(aliceStore, bobStore));
  }

  private void assertSessionIdEquals(ZonaRosaProtocolStore aliceStore, ZonaRosaProtocolStore bobStore) {
    assertTrue(isSessionIdEqual(aliceStore, bobStore));
  }

  private void assertSessionIdNotEquals(
      ZonaRosaProtocolStore aliceStore, ZonaRosaProtocolStore bobStore) {
    assertFalse(isSessionIdEqual(aliceStore, bobStore));
  }

  private boolean isSessionIdEqual(ZonaRosaProtocolStore aliceStore, ZonaRosaProtocolStore bobStore) {
    return Arrays.equals(
        getAliceBaseKey(aliceStore.loadSession(BOB_ADDRESS)),
        getAliceBaseKey(bobStore.loadSession(ALICE_ADDRESS)));
  }
}

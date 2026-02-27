//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import org.junit.Test;
import io.zonarosa.libzonarosa.internal.NativeTesting;
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;
import io.zonarosa.libzonarosa.protocol.util.Hex;

public class SessionRecordTest {

  public static byte[] getAliceBaseKey(SessionRecord record) {
    return filterExceptions(
        () -> record.guardedMapChecked(NativeTesting::SessionRecord_GetAliceBaseKey));
  }

  @Test
  public void testUninitAccess() {
    SessionRecord empty_record = new SessionRecord();

    assertFalse(empty_record.hasSenderChain());

    assertEquals(empty_record.getSessionVersion(), 0);
  }

  @Test
  public void testBadPreKeyRecords() throws Exception {
    assertThrows(InvalidMessageException.class, () -> new PreKeyRecord(new byte[] {0}));
    assertThrows(InvalidMessageException.class, () -> new SignedPreKeyRecord(new byte[] {0}));
    assertThrows(InvalidMessageException.class, () -> new KyberPreKeyRecord(new byte[] {0}));

    // The keys in records are lazily parsed, which means malformed keys aren't caught right away.
    // The following payloads were generated via protoscope:
    // % protoscope -s | xxd -p
    // The fields are described in storage.proto in the libzonarosa-protocol crate.
    {
      // 1: 42
      // 2: {}
      // 3: {}
      final var record = new PreKeyRecord(Hex.fromStringCondensedAssert("082a12001a00"));
      assertThrows(InvalidKeyException.class, () -> record.getKeyPair());
    }

    {
      // 1: 42
      // 2: {}
      // 3: {}
      // 4: {}
      // 5: 0i64
      final var record =
          new SignedPreKeyRecord(
              Hex.fromStringCondensedAssert("082a12001a002200290000000000000000"));
      assertThrows(InvalidKeyException.class, () -> record.getKeyPair());
    }

    {
      // 1: 42
      // 2: {}
      // 3: {}
      // 4: {}
      // 5: 0i64
      final var record =
          new KyberPreKeyRecord(
              Hex.fromStringCondensedAssert("082a12001a002200290000000000000000"));
      assertThrows(InvalidKeyException.class, () -> record.getKeyPair());
    }
  }
}

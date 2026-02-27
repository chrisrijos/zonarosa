//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.groups.state;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import kotlin.Pair;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;

public class InMemorySenderKeyStore implements SenderKeyStore {

  private final Map<Pair<ZonaRosaProtocolAddress, UUID>, SenderKeyRecord> store = new HashMap<>();

  @Override
  public void storeSenderKey(
      ZonaRosaProtocolAddress sender, UUID distributionId, SenderKeyRecord record) {
    store.put(new Pair<>(sender, distributionId), record);
  }

  @Override
  public SenderKeyRecord loadSenderKey(ZonaRosaProtocolAddress sender, UUID distributionId) {
    try {
      SenderKeyRecord record = store.get(new Pair<>(sender, distributionId));

      if (record == null) {
        return null;
      } else {
        return new SenderKeyRecord(record.serialize());
      }
    } catch (InvalidMessageException e) {
      throw new AssertionError(e);
    }
  }
}

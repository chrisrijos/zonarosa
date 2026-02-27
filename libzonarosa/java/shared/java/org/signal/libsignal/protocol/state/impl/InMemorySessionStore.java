//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import io.zonarosa.libzonarosa.protocol.InvalidMessageException;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.SessionStore;

public class InMemorySessionStore implements SessionStore {

  private Map<ZonaRosaProtocolAddress, byte[]> sessions = new HashMap<>();

  public InMemorySessionStore() {}

  @Override
  public synchronized SessionRecord loadSession(ZonaRosaProtocolAddress remoteAddress) {
    try {
      if (containsSession(remoteAddress)) {
        return new SessionRecord(sessions.get(remoteAddress));
      } else {
        return null;
      }
    } catch (InvalidMessageException e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public synchronized List<SessionRecord> loadExistingSessions(
      List<ZonaRosaProtocolAddress> addresses) throws NoSessionException {
    List<SessionRecord> resultSessions = new LinkedList<>();
    for (ZonaRosaProtocolAddress remoteAddress : addresses) {
      byte[] serialized = sessions.get(remoteAddress);
      if (serialized == null) {
        throw new NoSessionException(remoteAddress, "no session for " + remoteAddress);
      }
      try {
        resultSessions.add(new SessionRecord(serialized));
      } catch (InvalidMessageException e) {
        throw new AssertionError(e);
      }
    }
    return resultSessions;
  }

  @Override
  public synchronized List<Integer> getSubDeviceSessions(String name) {
    List<Integer> deviceIds = new LinkedList<>();

    for (ZonaRosaProtocolAddress key : sessions.keySet()) {
      if (key.getName().equals(name) && key.getDeviceId() != 1) {
        deviceIds.add(key.getDeviceId());
      }
    }

    return deviceIds;
  }

  @Override
  public synchronized void storeSession(ZonaRosaProtocolAddress address, SessionRecord record) {
    sessions.put(address, record.serialize());
  }

  @Override
  public synchronized boolean containsSession(ZonaRosaProtocolAddress address) {
    return sessions.containsKey(address);
  }

  @Override
  public synchronized void deleteSession(ZonaRosaProtocolAddress address) {
    sessions.remove(address);
  }

  @Override
  public synchronized void deleteAllSessions(String name) {
    for (ZonaRosaProtocolAddress key : sessions.keySet()) {
      if (key.getName().equals(name)) {
        sessions.remove(key);
      }
    }
  }
}

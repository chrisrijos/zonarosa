//
// Copyright 2014-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.state;

import java.util.List;
import io.zonarosa.libzonarosa.internal.CalledFromNative;
import io.zonarosa.libzonarosa.protocol.NoSessionException;
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;

/**
 * The interface to the durable store of session state information for remote clients.
 *
 * @author Moxie Marlinspike
 */
@CalledFromNative
public interface SessionStore {

  /**
   * Returns a copy of the {@link SessionRecord} corresponding to the recipientId + deviceId tuple,
   * or a new SessionRecord if one does not currently exist.
   *
   * <p>It is important that implementations return a copy of the current durable information. The
   * returned SessionRecord may be modified, but those changes should not have an effect on the
   * durable session state (what is returned by subsequent calls to this method) without the store
   * method being called here first.
   *
   * @param address The name and device ID of the remote client.
   * @return a copy of the SessionRecord corresponding to the recipientId + deviceId tuple, or a new
   *     SessionRecord if one does not currently exist.
   */
  public SessionRecord loadSession(ZonaRosaProtocolAddress address);

  /**
   * Returns the {@link SessionRecord}s corresponding to the given addresses.
   *
   * @param addresses The name and device ID of each remote client.
   * @return the SessionRecords corresponding to each recipientId + deviceId tuple.
   * @throws NoSessionException if any address does not have an active session.
   */
  public List<SessionRecord> loadExistingSessions(List<ZonaRosaProtocolAddress> addresses)
      throws NoSessionException;

  /**
   * Returns all known devices with active sessions for a recipient
   *
   * @param name the name of the client.
   * @return all known sub-devices with active sessions.
   */
  public List<Integer> getSubDeviceSessions(String name);

  /**
   * Commit to storage the {@link SessionRecord} for a given recipientId + deviceId tuple.
   *
   * @param address the address of the remote client.
   * @param record the current SessionRecord for the remote client.
   */
  public void storeSession(ZonaRosaProtocolAddress address, SessionRecord record);

  /**
   * Determine whether there is a committed {@link SessionRecord} for a recipientId + deviceId
   * tuple.
   *
   * @param address the address of the remote client.
   * @return true if a {@link SessionRecord} exists, false otherwise.
   */
  public boolean containsSession(ZonaRosaProtocolAddress address);

  /**
   * Remove a {@link SessionRecord} for a recipientId + deviceId tuple.
   *
   * @param address the address of the remote client.
   */
  public void deleteSession(ZonaRosaProtocolAddress address);

  /**
   * Remove the {@link SessionRecord}s corresponding to all devices of a recipientId.
   *
   * @param name the name of the remote client.
   */
  public void deleteAllSessions(String name);
}

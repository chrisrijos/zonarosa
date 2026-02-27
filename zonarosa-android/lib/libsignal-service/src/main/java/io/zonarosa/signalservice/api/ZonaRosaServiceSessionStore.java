package io.zonarosa.service.api;

import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.state.SessionRecord;
import io.zonarosa.libzonarosa.protocol.state.SessionStore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * And extension of the normal protocol session store interface that has additional methods that are
 * needed in the service layer, but not the protocol layer.
 */
public interface ZonaRosaServiceSessionStore extends SessionStore {
  void archiveSession(ZonaRosaProtocolAddress address);
  Map<ZonaRosaProtocolAddress, SessionRecord> getAllAddressesWithActiveSessions(List<String> addressNames);
}

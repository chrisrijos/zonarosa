package io.zonarosa.service.api;

import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;

/**
 * And extension of the normal protocol store interface that has additional methods that are needed
 * in the service layer, but not the protocol layer.
 */
public interface ZonaRosaServiceAccountDataStore extends ZonaRosaProtocolStore,
                                                       ZonaRosaServicePreKeyStore,
                                                       ZonaRosaServiceSessionStore,
                                                       ZonaRosaServiceSenderKeyStore,
                                                       ZonaRosaServiceKyberPreKeyStore {
  /**
   * @return True if the user has linked devices, otherwise false.
   */
  boolean isMultiDevice();
}

package io.zonarosa.service.api;

import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress;
import io.zonarosa.libzonarosa.protocol.groups.state.SenderKeyStore;
import io.zonarosa.service.api.push.DistributionId;

import java.util.Collection;
import java.util.Set;

/**
 * And extension of the normal protocol sender key store interface that has additional methods that are
 * needed in the service layer, but not the protocol layer.
 */
public interface ZonaRosaServiceSenderKeyStore extends SenderKeyStore {
  /**
   * @return A set of protocol addresses that have previously been sent the sender key data for the provided distributionId.
   */
  Set<ZonaRosaProtocolAddress> getSenderKeySharedWith(DistributionId distributionId);

  /**
   * Marks the provided addresses as having been sent the sender key data for the provided distributionId.
   */
  void markSenderKeySharedWith(DistributionId distributionId, Collection<ZonaRosaProtocolAddress> addresses);

  /**
   * Marks the provided addresses as not knowing about any distributionIds.
   */
  void clearSenderKeySharedWith(Collection<ZonaRosaProtocolAddress> addresses);
}

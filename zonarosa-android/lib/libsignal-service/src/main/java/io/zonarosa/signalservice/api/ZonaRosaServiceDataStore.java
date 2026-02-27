package io.zonarosa.service.api;

import io.zonarosa.core.models.ServiceId;

/**
 * And extension of the normal protocol store interface that has additional methods that are needed
 * in the service layer, but not the protocol layer.
 */
public interface ZonaRosaServiceDataStore {

  /**
   * @return A {@link ZonaRosaServiceAccountDataStore} for the specified account.
   */
  ZonaRosaServiceAccountDataStore get(ServiceId accountIdentifier);

  /**
   * @return A {@link ZonaRosaServiceAccountDataStore} for the ACI account.
   */
  ZonaRosaServiceAccountDataStore aci();

  /**
   * @return A {@link ZonaRosaServiceAccountDataStore} for the PNI account.
   */
  ZonaRosaServiceAccountDataStore pni();

  /**
   * @return True if the user has linked devices, otherwise false.
   */
  boolean isMultiDevice();
}

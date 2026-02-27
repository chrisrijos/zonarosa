package io.zonarosa.messenger

import io.zonarosa.service.api.account.AccountAttributes

object AppCapabilities {
  /**
   * @param storageCapable Whether or not the user can use storage service. This is another way of
   * asking if the user has set a ZonaRosa PIN or not.
   */
  @JvmStatic
  fun getCapabilities(storageCapable: Boolean): AccountAttributes.Capabilities {
    return AccountAttributes.Capabilities(
      storage = storageCapable,
      versionedExpirationTimer = true,
      attachmentBackfill = true,
      spqr = true
    )
  }
}

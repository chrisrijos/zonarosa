/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.link

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Request body for setting the transfer archive for a linked device.
 */
data class SetLinkedDeviceTransferArchiveRequest(
  @JsonProperty val destinationDeviceId: Int,
  @JsonProperty val destinationDeviceRegistrationId: Int,
  @JsonProperty val transferArchive: TransferArchive
) {
  sealed class TransferArchive {
    data class CdnInfo(
      @JsonProperty val cdn: Int,
      @JsonProperty val key: String
    ) : TransferArchive()
    data class Error(
      @JsonProperty val error: TransferArchiveError
    ) : TransferArchive()
  }
}

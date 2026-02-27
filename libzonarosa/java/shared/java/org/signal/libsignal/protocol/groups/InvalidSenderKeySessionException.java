//
// Copyright 2021 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.groups;

import java.util.UUID;

public class InvalidSenderKeySessionException extends IllegalStateException {

  private final UUID distributionId;

  public InvalidSenderKeySessionException(UUID distributionId, String message) {
    super(message);
    this.distributionId = distributionId;
  }

  public UUID getDistributionId() {
    return distributionId;
  }
}

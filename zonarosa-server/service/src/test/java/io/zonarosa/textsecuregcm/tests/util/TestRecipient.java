/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.tests.util;

import io.zonarosa.server.identity.ServiceIdentifier;

public record TestRecipient(ServiceIdentifier uuid,
                            byte[] deviceIds,
                            int[] registrationIds,
                            byte[] perRecipientKeyMaterial) {

  public TestRecipient(ServiceIdentifier uuid,
                       byte deviceId,
                       int registrationId,
                       byte[] perRecipientKeyMaterial) {

    this(uuid, new byte[]{deviceId}, new int[]{registrationId}, perRecipientKeyMaterial);
  }
}

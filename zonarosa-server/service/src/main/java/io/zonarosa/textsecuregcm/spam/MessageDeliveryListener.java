/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.spam;

import io.zonarosa.server.storage.Account;

public interface MessageDeliveryListener {

  void handleMessageDelivered(Account destinationAccount,
      byte destinationDeviceId,
      boolean ephemeral,
      boolean urgent,
      boolean story,
      boolean sealedSender,
      boolean multiRecipient,
      boolean sync);
}

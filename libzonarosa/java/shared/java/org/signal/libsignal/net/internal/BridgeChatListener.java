//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net.internal;

import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * A helper interface that represents the callback methods used by the Rust side of the bridge.
 *
 * <p>The app-facing listener API is {@link io.zonarosa.libzonarosa.net.ChatConnectionListener}.
 */
@CalledFromNative
public interface BridgeChatListener {
  void receivedIncomingMessage(byte[] envelope, long serverDeliveryTimestamp, long sendAckHandle);

  void receivedQueueEmpty();

  void receivedAlerts(String[] alerts);

  // disconnectReason should always be a ChatServiceError, but it is converted to a Throwable
  //   just to be easily passed across the bridge.
  void connectionInterrupted(Throwable disconnectReason);
}

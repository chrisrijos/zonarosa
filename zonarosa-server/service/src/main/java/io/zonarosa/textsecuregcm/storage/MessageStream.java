/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import io.zonarosa.server.entities.MessageProtos;

/// A message stream publishes an ordered stream of ZonaRosa messages from a destination device's queue and provides a
/// mechanism for consumers to acknowledge receipt of delivered messages.
public interface MessageStream {

  /// Publishes a non-terminating stream of [MessageStreamEntry.Envelope] entities and at most one
  /// [MessageStreamEntry.QueueEmpty].
  ///
  /// @return a non-terminating stream of message stream entries
  Flow.Publisher<MessageStreamEntry> getMessages();

  /// Acknowledges receipt of the given message. Implementations may delete the message immediately or defer deletion for
  /// inclusion in a more efficient batch deletion.
  ///
  /// @param message the message to acknowledge
  ///
  /// @return a future that completes when the message stream has processed the acknowledgement
  CompletableFuture<Void> acknowledgeMessage(MessageProtos.Envelope message);
}

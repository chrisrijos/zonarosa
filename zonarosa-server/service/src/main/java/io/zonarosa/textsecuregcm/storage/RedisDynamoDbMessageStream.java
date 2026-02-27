/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import com.google.common.annotations.VisibleForTesting;
import io.zonarosa.server.entities.MessageProtos;
import io.zonarosa.server.push.RedisMessageAvailabilityManager;
import io.zonarosa.server.util.Util;

/// A [MessageStream] implementation that produces message from a joint DynamoDB/Redis message store.
public class RedisDynamoDbMessageStream implements MessageStream {

  private final MessagesDynamoDb messagesDynamoDb;
  private final MessagesCache messagesCache;

  private final UUID accountIdentifier;
  private final Device device;

  private final RedisDynamoDbMessagePublisher messagePublisher;

  public RedisDynamoDbMessageStream(final MessagesDynamoDb messagesDynamoDb,
      final MessagesCache messagesCache,
      final RedisMessageAvailabilityManager redisMessageAvailabilityManager,
      final UUID accountIdentifier,
      final Device device) {

    this(messagesDynamoDb, messagesCache, accountIdentifier, device, new RedisDynamoDbMessagePublisher(messagesDynamoDb,
        messagesCache,
        redisMessageAvailabilityManager,
        accountIdentifier,
        device));
  }

  @VisibleForTesting
  RedisDynamoDbMessageStream(final MessagesDynamoDb messagesDynamoDb,
      final MessagesCache messagesCache,
      final UUID accountIdentifier,
      final Device device,
      final RedisDynamoDbMessagePublisher messagePublisher) {

    this.messagesDynamoDb = messagesDynamoDb;
    this.messagesCache = messagesCache;
    this.accountIdentifier = accountIdentifier;
    this.device = device;
    this.messagePublisher = messagePublisher;
  }

  @Override
  public Flow.Publisher<MessageStreamEntry> getMessages() {
    return messagePublisher;
  }

  @Override
  public CompletableFuture<Void> acknowledgeMessage(final MessageProtos.Envelope message) {
    final UUID guid = UUID.fromString(message.getServerGuid());

    return messagesCache.remove(accountIdentifier, device.getId(), guid)
        .thenCompose(removed -> removed.map(_ -> CompletableFuture.<Void>completedFuture(null))
            .orElseGet(() ->
                messagesDynamoDb.deleteMessage(accountIdentifier, device, guid, message.getServerTimestamp())
                    .thenRun(Util.NOOP)))
        .whenComplete((_, _) -> messagePublisher.handleMessageAcknowledged());
  }
}

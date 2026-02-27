/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import io.lettuce.core.ScriptOutputType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import io.zonarosa.server.entities.MessageProtos;
import io.zonarosa.server.push.ClientEvent;
import io.zonarosa.server.push.NewMessageAvailableEvent;
import io.zonarosa.server.push.RedisMessageAvailabilityManager;
import io.zonarosa.server.redis.ClusterLuaScript;
import io.zonarosa.server.redis.FaultTolerantRedisClusterClient;
import io.zonarosa.server.util.ResilienceUtil;

/**
 * Inserts an envelope into the message queue for a destination device and publishes a "new message available" event.
 */
class MessagesCacheInsertScript {

  private final ClusterLuaScript insertScript;
  private final ScheduledExecutorService retryExecutor;

  private static final byte[] NEW_MESSAGE_EVENT_BYTES = ClientEvent.newBuilder()
      .setNewMessageAvailable(NewMessageAvailableEvent.getDefaultInstance())
      .build()
      .toByteArray();

  MessagesCacheInsertScript(FaultTolerantRedisClusterClient redisCluster,
      final ScheduledExecutorService retryExecutor) throws IOException {

    this.insertScript = ClusterLuaScript.fromResource(redisCluster, "lua/insert_item.lua", ScriptOutputType.BOOLEAN);
    this.retryExecutor = retryExecutor;
  }

  /**
   * Inserts a message into the given device's message queue and publishes a "new message available" event.
   *
   * @param destinationUuid the account identifier for the receiving account
   * @param destinationDevice the ID of the receiving device within the given account
   * @param envelope the message to insert
   * @return {@code true} if the destination device had a registered "presence"/event subscriber or {@code false}
   * otherwise
   */
  CompletionStage<Boolean> executeAsync(final UUID destinationUuid, final byte destinationDevice, final MessageProtos.Envelope envelope) {
    assert envelope.hasServerGuid();
    assert envelope.hasServerTimestamp();

    final List<byte[]> keys = List.of(
        MessagesCache.getMessageQueueKey(destinationUuid, destinationDevice), // queueKey
        MessagesCache.getMessageQueueMetadataKey(destinationUuid, destinationDevice), // queueMetadataKey
        MessagesCache.getQueueIndexKey(destinationUuid, destinationDevice), // queueTotalIndexKey
        RedisMessageAvailabilityManager.getClientEventChannel(destinationUuid, destinationDevice) // eventChannelKey
    );

    final List<byte[]> args = new ArrayList<>(Arrays.asList(
        EnvelopeUtil.compress(envelope).toByteArray(), // message
        String.valueOf(envelope.getServerTimestamp()).getBytes(StandardCharsets.UTF_8), // currentTime
        envelope.getServerGuid().getBytes(StandardCharsets.UTF_8), // guid
        NEW_MESSAGE_EVENT_BYTES // eventPayload
    ));

    return ResilienceUtil.getGeneralRedisRetry(MessagesCache.RETRY_NAME)
        .executeCompletionStage(retryExecutor, () -> insertScript.executeBinaryAsync(keys, args))
        .thenApply(result -> (boolean) result);
  }
}

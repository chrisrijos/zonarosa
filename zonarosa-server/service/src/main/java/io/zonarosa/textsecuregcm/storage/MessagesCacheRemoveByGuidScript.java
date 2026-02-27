/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import io.lettuce.core.ScriptOutputType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import io.zonarosa.server.redis.ClusterLuaScript;
import io.zonarosa.server.redis.FaultTolerantRedisClusterClient;
import io.zonarosa.server.util.ResilienceUtil;

/**
 * Removes a list of message GUIDs from the queue of a destination device.
 */
class MessagesCacheRemoveByGuidScript {

  private final ClusterLuaScript removeByGuidScript;
  private final ScheduledExecutorService retryExecutor;

  MessagesCacheRemoveByGuidScript(final FaultTolerantRedisClusterClient redisCluster,
      final ScheduledExecutorService retryExecutor) throws IOException {

    this.removeByGuidScript = ClusterLuaScript.fromResource(redisCluster, "lua/remove_item_by_guid.lua",
        ScriptOutputType.OBJECT);
    this.retryExecutor = retryExecutor;
  }

  CompletionStage<List<byte[]>> execute(final UUID destinationUuid, final byte destinationDevice,
      final List<UUID> messageGuids) {

    final List<byte[]> keys = List.of(
        MessagesCache.getMessageQueueKey(destinationUuid, destinationDevice), // queueKey
        MessagesCache.getMessageQueueMetadataKey(destinationUuid, destinationDevice), // queueMetadataKey
        MessagesCache.getQueueIndexKey(destinationUuid, destinationDevice) // queueTotalIndexKey
    );
    final List<byte[]> args = messageGuids.stream().map(guid -> guid.toString().getBytes(StandardCharsets.UTF_8))
        .toList();

    //noinspection unchecked
    return ResilienceUtil.getGeneralRedisRetry(MessagesCache.RETRY_NAME)
        .executeCompletionStage(retryExecutor, () -> removeByGuidScript.executeBinaryAsync(keys, args))
        .thenApply(result -> (List<byte[]>) result);
  }

}

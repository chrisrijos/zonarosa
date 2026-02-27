/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import io.lettuce.core.ScriptOutputType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import io.zonarosa.server.redis.ClusterLuaScript;
import io.zonarosa.server.redis.FaultTolerantRedisClusterClient;

/**
 * Returns a list of queues that may be persisted. They will be sorted from oldest to more recent, limited by the
 * {@code maxTime} argument.
 *
 * @see MessagePersister
 */
class MessagesCacheGetQueuesToPersistScript {

  private final ClusterLuaScript getQueuesToPersistScript;

  MessagesCacheGetQueuesToPersistScript(final FaultTolerantRedisClusterClient redisCluster) throws IOException {
    this.getQueuesToPersistScript = ClusterLuaScript.fromResource(redisCluster, "lua/get_queues_to_persist.lua",
        ScriptOutputType.MULTI);
  }

  List<String> execute(final int slot, final Instant maxTime, final int limit) {
    final List<String> keys = List.of(
        new String(MessagesCache.getQueueIndexKey(slot), StandardCharsets.UTF_8) // queueTotalIndexKey
    );
    final List<String> args = List.of(
        String.valueOf(maxTime.toEpochMilli()), // maxTime
        String.valueOf(limit) // limit
    );

    //noinspection unchecked
    return (List<String>) getQueuesToPersistScript.execute(keys, args);
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.zonarosa.server.entities.MessageProtos;
import io.zonarosa.server.redis.RedisClusterExtension;

class MessagesCacheRemoveQueueScriptTest {

  @RegisterExtension
  static final RedisClusterExtension REDIS_CLUSTER_EXTENSION = RedisClusterExtension.builder().build();


  @Test
  void testCacheRemoveQueueScript() throws Exception {
    final MessagesCacheInsertScript insertScript = new MessagesCacheInsertScript(
        REDIS_CLUSTER_EXTENSION.getRedisCluster(),
        mock(ScheduledExecutorService.class));

    final UUID destinationUuid = UUID.randomUUID();
    final byte deviceId = 1;
    final MessageProtos.Envelope envelope1 = MessageProtos.Envelope.newBuilder()
        .setServerTimestamp(Instant.now().getEpochSecond())
        .setServerGuid(UUID.randomUUID().toString())
        .build();

    insertScript.executeAsync(destinationUuid, deviceId, envelope1).toCompletableFuture().join();

    final MessagesCacheRemoveQueueScript removeScript = new MessagesCacheRemoveQueueScript(
        REDIS_CLUSTER_EXTENSION.getRedisCluster());

    final List<byte[]> messagesToCheckForMrmKeys = removeScript.execute(destinationUuid, deviceId,
            Collections.emptyList())
        .block(Duration.ofSeconds(1));

    assertEquals(1, messagesToCheckForMrmKeys.size());

  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.storage;

import io.lettuce.core.ScriptOutputType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;
import io.zonarosa.libzonarosa.protocol.SealedSenderMultiRecipientMessage;
import io.zonarosa.server.redis.ClusterLuaScript;
import io.zonarosa.server.redis.FaultTolerantRedisClusterClient;
import io.zonarosa.server.util.ResilienceUtil;
import io.zonarosa.server.util.Util;

/**
 * Inserts the shared multi-recipient message payload into the cache. The list of recipients and views will be set as
 * fields in the hash.
 *
 * @see SealedSenderMultiRecipientMessage#serializedRecipientView(SealedSenderMultiRecipientMessage.Recipient)
 */
class MessagesCacheInsertSharedMultiRecipientPayloadAndViewsScript {

  private final ClusterLuaScript script;
  private final ScheduledExecutorService retryExecutor;

  static final String ERROR_KEY_EXISTS = "ERR key exists";

  MessagesCacheInsertSharedMultiRecipientPayloadAndViewsScript(FaultTolerantRedisClusterClient redisCluster,
      final ScheduledExecutorService retryExecutor) throws IOException {

    this.script = ClusterLuaScript.fromResource(redisCluster, "lua/insert_shared_multirecipient_message_data.lua",
        ScriptOutputType.INTEGER);

    this.retryExecutor = retryExecutor;
  }

  CompletionStage<Void> executeAsync(final byte[] sharedMrmKey, final SealedSenderMultiRecipientMessage message) {
    final List<byte[]> keys = List.of(
        sharedMrmKey // sharedMrmKey
    );

    // Pre-allocate capacity for the most fields we expect -- 6 devices per recipient, plus the data field.
    final List<byte[]> args = new ArrayList<>(message.getRecipients().size() * 6 + 1);
    args.add(message.serialized());

    message.getRecipients().forEach((serviceId, recipient) -> {
      for (byte device : recipient.getDevices()) {
        args.add(MessagesCache.getSharedMrmViewKey(serviceId, device));
        args.add(message.serializedRecipientView(recipient));
      }
    });

    return ResilienceUtil.getGeneralRedisRetry(MessagesCache.RETRY_NAME)
        .executeCompletionStage(retryExecutor, () -> script.executeBinaryAsync(keys, args))
        .thenRun(Util.NOOP);
  }
}

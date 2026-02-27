/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.lettuce.core.resource.ClientResources;
import java.util.concurrent.atomic.AtomicBoolean;
import io.zonarosa.server.redis.FaultTolerantRedisClient;
import io.zonarosa.server.redis.RedisServerExtension;

@JsonTypeName("local")
public class LocalFaultTolerantRedisClientFactory implements FaultTolerantRedisClientFactory {

  private static final RedisServerExtension REDIS_SERVER_EXTENSION = RedisServerExtension.builder().build();

  private final AtomicBoolean shutdownHookConfigured = new AtomicBoolean();

  private LocalFaultTolerantRedisClientFactory() {
    try {
      REDIS_SERVER_EXTENSION.beforeAll(null);
      REDIS_SERVER_EXTENSION.beforeEach(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public FaultTolerantRedisClient build(final String name, final ClientResources clientResources) {

    if (shutdownHookConfigured.compareAndSet(false, true)) {
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        try {
          REDIS_SERVER_EXTENSION.close();
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      }));
    }

    final RedisConfiguration config = new RedisConfiguration();
    config.setUri(RedisServerExtension.getRedisURI().toString());

    return new FaultTolerantRedisClient(name, config, clientResources.mutate());
  }
}

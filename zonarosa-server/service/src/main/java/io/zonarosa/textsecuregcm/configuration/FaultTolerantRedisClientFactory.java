/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.configuration;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.jackson.Discoverable;
import io.lettuce.core.resource.ClientResources;
import io.zonarosa.server.redis.FaultTolerantRedisClient;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = RedisConfiguration.class)
public interface FaultTolerantRedisClientFactory extends Discoverable {

  FaultTolerantRedisClient build(String name, ClientResources clientResources);
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.limits;

import static java.util.Objects.requireNonNull;

import io.lettuce.core.ScriptOutputType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import io.zonarosa.server.configuration.dynamic.DynamicConfiguration;
import io.zonarosa.server.redis.ClusterLuaScript;
import io.zonarosa.server.redis.FaultTolerantRedisClusterClient;
import io.zonarosa.server.storage.DynamicConfigurationManager;

public abstract class BaseRateLimiters<T extends RateLimiterDescriptor> {

  private final Map<T, RateLimiter> rateLimiterByDescriptor;

  protected BaseRateLimiters(
      final T[] values,
      final DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager,
      final ClusterLuaScript validateScript,
      final FaultTolerantRedisClusterClient cacheCluster,
      final ScheduledExecutorService retryExecutor,
      final Clock clock) {
    this.rateLimiterByDescriptor = Arrays.stream(values)
        .map(descriptor -> Pair.of(
            descriptor,
            createForDescriptor(descriptor, dynamicConfigurationManager, validateScript, cacheCluster, retryExecutor, clock)))
        .collect(Collectors.toUnmodifiableMap(Pair::getKey, Pair::getValue));
  }

  public RateLimiter forDescriptor(final T handle) {
    return requireNonNull(rateLimiterByDescriptor.get(handle));
  }

  public static ClusterLuaScript defaultScript(final FaultTolerantRedisClusterClient cacheCluster) {
    try {
      return ClusterLuaScript.fromResource(
          cacheCluster, "lua/validate_rate_limit.lua", ScriptOutputType.INTEGER);
    } catch (final IOException e) {
      throw new UncheckedIOException("Failed to load rate limit validation script", e);
    }
  }

  private static RateLimiter createForDescriptor(
      final RateLimiterDescriptor descriptor,
      final DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager,
      final ClusterLuaScript validateScript,
      final FaultTolerantRedisClusterClient cacheCluster,
      final ScheduledExecutorService retryExecutor,
      final Clock clock) {
    final Supplier<RateLimiterConfig> configResolver =
        () -> dynamicConfigurationManager.getConfiguration().getLimits().getOrDefault(descriptor.id(), descriptor.defaultConfig());
    return new LeakyBucketRateLimiter(descriptor.id(), configResolver, validateScript, cacheCluster, retryExecutor, clock);
  }
}

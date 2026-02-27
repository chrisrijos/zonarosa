/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.redis;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;

public class FaultTolerantPubSubConnection<K, V> extends AbstractFaultTolerantPubSubConnection<K, V, StatefulRedisPubSubConnection<K, V>> {

  protected FaultTolerantPubSubConnection(final String name,
      final StatefulRedisPubSubConnection<K, V> pubSubConnection) {

    super(name, pubSubConnection);
  }
}

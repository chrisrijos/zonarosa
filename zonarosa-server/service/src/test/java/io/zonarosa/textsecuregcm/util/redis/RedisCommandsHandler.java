/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util.redis;

import java.util.List;

@FunctionalInterface
public interface RedisCommandsHandler {

  Object redisCommand(String command, List<Object> args);
}

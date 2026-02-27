/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.grpc;

import io.zonarosa.server.limits.RateLimiter;
import reactor.core.publisher.Mono;

class RateLimitUtil {

  static Mono<Void> rateLimitByRemoteAddress(final RateLimiter rateLimiter) {
    return rateLimiter.validateReactive(RequestAttributesUtil.getRemoteAddress().getHostAddress());
  }
}

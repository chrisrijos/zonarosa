//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net

import io.zonarosa.libzonarosa.internal.CalledFromNative
import java.util.EnumSet

/**
 * Thrown when a request should be retried after waiting.
 *
 * <p>When the websocket transport is in use, this corresponds to a {@code HTTP 428} response to
 * requests to a number of endpoints.
 */
public class RateLimitChallengeException : ChatServiceException {
  public val token: String
  public val options: Set<ChallengeOption>

  @CalledFromNative
  public constructor(message: String, token: String, options: Array<ChallengeOption>) : super(message) {
    this.token = token
    this.options = EnumSet.copyOf(options.asList())
  }
}

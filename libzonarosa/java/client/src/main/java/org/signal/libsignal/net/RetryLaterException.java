//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import java.io.IOException;
import java.time.Duration;
import io.zonarosa.libzonarosa.internal.CalledFromNative;

/**
 * Thrown when a request should be retried after waiting.
 *
 * <p>When the websocket transport is in use, this corresponds to a {@code HTTP 429} response to
 * requests to a number of endpoints. It can also be produced as the result of a websocket close
 * frame from an enclave service with close code {@code 4008}.
 */
public class RetryLaterException extends IOException {
  /** The amount of time to wait before retrying. */
  public final Duration duration;

  @CalledFromNative
  private RetryLaterException(long retryAfterSeconds) {
    this(Duration.ofSeconds(retryAfterSeconds));
  }

  public RetryLaterException(Duration duration) {
    super("Retry after " + duration.getSeconds() + " seconds");
    this.duration = duration;
  }
}

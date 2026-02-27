//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

import java.time.Duration;

public abstract class DurationExt {
  public static final int timeoutMillis(Duration timeout) {
    int millis;
    try {
      millis = Math.toIntExact(timeout.toMillis());
    } catch (ArithmeticException e) {
      millis = Integer.MAX_VALUE;
    }
    return millis;
  }
}

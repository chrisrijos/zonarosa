//
// Copyright 2024 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.svr;

public final class RestoreFailedException extends SvrException {
  private int triesRemaining;

  public RestoreFailedException(String message, int triesRemaining) {
    super(message);
    this.triesRemaining = triesRemaining;
  }

  public int getTriesRemaining() {
    return this.triesRemaining;
  }
}

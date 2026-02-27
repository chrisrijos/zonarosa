//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

public abstract class KeyTransparency {
  /**
   * Mode of the monitor operation.
   *
   * <p>If the newer version of account data is found in the key transparency log, self-monitor will
   * terminate with an error, but monitor for other account will fall back to a full search and
   * update the locally stored data.
   */
  public enum MonitorMode {
    SELF,
    OTHER
  }
}

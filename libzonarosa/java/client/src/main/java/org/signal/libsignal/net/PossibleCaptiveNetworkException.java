//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net;

/**
 * Indicates that a server presented a TLS certificate that might have come from a captive network.
 */
public class PossibleCaptiveNetworkException extends NetworkException {
  public PossibleCaptiveNetworkException(String message) {
    super(message);
  }
}

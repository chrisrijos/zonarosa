//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.cds2;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.util.Map;
import io.zonarosa.libzonarosa.attest.AttestationDataException;
import io.zonarosa.libzonarosa.internal.Native;

public final class Cds2Metrics {

  private Cds2Metrics() {}

  /**
   * Parse a cds2 attestation response (ClientHandshakeStart) and return supplemental information
   * extracted from the response's evidence and endorsements.
   *
   * @param attestationMessage A ClientHandshakeStart message
   * @throws AttestationDataException if the attestationMessage cannot be parsed
   */
  public static Map<String, Long> extract(final byte[] attestationMessage)
      throws AttestationDataException {
    @SuppressWarnings("unchecked")
    final var result =
        (Map<String, Long>)
            filterExceptions(
                AttestationDataException.class,
                () -> Native.Cds2Metrics_extract(attestationMessage));
    return result;
  }
}

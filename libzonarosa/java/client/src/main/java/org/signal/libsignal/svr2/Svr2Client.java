//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.svr2;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.attest.AttestationDataException;
import io.zonarosa.libzonarosa.attest.AttestationFailedException;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.sgxsession.SgxClient;

/**
 * Svr2Client provides bindings to interact with ZonaRosa's v2 Secure Value Recovery service.
 *
 * <p>See the {@link SgxClient} docs for more information.
 */
public class Svr2Client extends SgxClient {
  public Svr2Client(byte[] mrenclave, byte[] attestationMsg, Instant currentInstant)
      throws AttestationDataException, AttestationFailedException {
    super(
        filterExceptions(
            AttestationDataException.class,
            AttestationFailedException.class,
            () -> Native.Svr2Client_New(mrenclave, attestationMsg, currentInstant.toEpochMilli())));
  }
}

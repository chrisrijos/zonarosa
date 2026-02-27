//
// Copyright 2022 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.cds2;

import static io.zonarosa.libzonarosa.internal.FilterExceptions.filterExceptions;

import java.time.Instant;
import io.zonarosa.libzonarosa.attest.AttestationDataException;
import io.zonarosa.libzonarosa.attest.AttestationFailedException;
import io.zonarosa.libzonarosa.internal.Native;
import io.zonarosa.libzonarosa.sgxsession.SgxClient;

/**
 * Cds2Client provides bindings to interact with ZonaRosa's v2 Contact Discovery Service.
 *
 * <p>See the {@link SgxClient} docs for more information.
 *
 * <p>A future update to Cds2Client will implement additional parts of the contact discovery
 * protocol.
 */
public class Cds2Client extends SgxClient {
  public Cds2Client(byte[] mrenclave, byte[] attestationMsg, Instant currentInstant)
      throws AttestationDataException, AttestationFailedException {
    super(
        filterExceptions(
            AttestationDataException.class,
            AttestationFailedException.class,
            () ->
                Native.Cds2ClientState_New(
                    mrenclave, attestationMsg, currentInstant.toEpochMilli())));
  }
}

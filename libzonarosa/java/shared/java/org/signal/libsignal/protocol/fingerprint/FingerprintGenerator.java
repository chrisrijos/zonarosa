//
// Copyright 2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.fingerprint;

import io.zonarosa.libzonarosa.protocol.IdentityKey;

public interface FingerprintGenerator {
  public Fingerprint createFor(
      int version,
      byte[] localStableIdentifier,
      IdentityKey localIdentityKey,
      byte[] remoteStableIdentifier,
      IdentityKey remoteIdentityKey);
}

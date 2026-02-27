/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.server.util;

import io.zonarosa.libzonarosa.usernames.BaseUsernameException;
import io.zonarosa.libzonarosa.usernames.Username;

public class UsernameHashZkProofVerifier {
  public void verifyProof(final byte[] proof, final byte[] hash) throws BaseUsernameException {
    Username.verifyProof(proof, hash);
  }
}

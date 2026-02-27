//
// Copyright 2013-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.ecc

public data class ECKeyPair(
  val publicKey: ECPublicKey,
  val privateKey: ECPrivateKey,
) {
  public companion object {
    @JvmStatic
    public fun generate(): ECKeyPair {
      var privateKey = ECPrivateKey.generate()
      return ECKeyPair(privateKey.getPublicKey(), privateKey)
    }
  }
}

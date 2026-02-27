//
// Copyright 2013-2016 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//
package io.zonarosa.libzonarosa.protocol.kdf

import io.zonarosa.libzonarosa.internal.Native

public object HKDF {
  @JvmStatic
  public fun deriveSecrets(
    inputKeyMaterial: ByteArray,
    info: ByteArray,
    outputLength: Int,
  ): ByteArray = Native.HKDF_DeriveSecrets(outputLength, inputKeyMaterial, info, null)

  @JvmStatic
  public fun deriveSecrets(
    inputKeyMaterial: ByteArray,
    salt: ByteArray,
    info: ByteArray?,
    outputLength: Int,
  ): ByteArray = Native.HKDF_DeriveSecrets(outputLength, inputKeyMaterial, info, salt)
}

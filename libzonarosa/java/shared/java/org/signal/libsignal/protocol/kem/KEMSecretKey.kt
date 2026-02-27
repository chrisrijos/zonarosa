//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.kem

import io.zonarosa.libzonarosa.internal.Native
import io.zonarosa.libzonarosa.internal.NativeHandleGuard
import io.zonarosa.libzonarosa.protocol.InvalidKeyException

public class KEMSecretKey(
  nativeHandle: Long,
) : NativeHandleGuard.SimpleOwner(
    NativeHandleGuard.SimpleOwner.throwIfNull(nativeHandle),
  ) {
  @Throws(InvalidKeyException::class)
  public constructor(privateKey: ByteArray) : this(
    Native.KyberSecretKey_Deserialize(privateKey),
  )

  protected override fun release(nativeHandle: Long) {
    Native.KyberSecretKey_Destroy(nativeHandle)
  }

  public fun serialize(): ByteArray = guardedMapChecked(Native::KyberSecretKey_Serialize)
}

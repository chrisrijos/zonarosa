//
// Copyright 2023 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.protocol.kem

import io.zonarosa.libzonarosa.internal.Native
import io.zonarosa.libzonarosa.internal.NativeHandleGuard
import io.zonarosa.libzonarosa.protocol.InvalidKeyException
import io.zonarosa.libzonarosa.protocol.SerializablePublicKey
import java.util.Arrays

public class KEMPublicKey :
  NativeHandleGuard.SimpleOwner,
  SerializablePublicKey {
  @Deprecated("use the constructor that takes an offset and length")
  @Throws(InvalidKeyException::class)
  public constructor(serialized: ByteArray, offset: Int) :
    this(serialized, offset, length = serialized.size - offset)

  @Throws(InvalidKeyException::class)
  public constructor(serialized: ByteArray, offset: Int, length: Int) :
    super(Native.KyberPublicKey_DeserializeWithOffsetLength(serialized, offset, length))

  @Throws(InvalidKeyException::class)
  public constructor(serialized: ByteArray) : this(serialized, 0, serialized.size)

  public constructor(nativeHandle: Long) : super(NativeHandleGuard.SimpleOwner.throwIfNull(nativeHandle))

  protected override fun release(nativeHandle: Long) {
    Native.KyberPublicKey_Destroy(nativeHandle)
  }

  public fun serialize(): ByteArray = guardedMapChecked(Native::KyberPublicKey_Serialize)

  public override fun equals(other: Any?): Boolean =
    when (other) {
      null -> false
      is KEMPublicKey ->
        guardedMap { thisNativeHandle ->
          other.guardedMap { otherNativeHandle ->
            Native.KyberPublicKey_Equals(thisNativeHandle, otherNativeHandle)
          }
        }
      else -> false
    }

  public override fun hashCode(): Int = Arrays.hashCode(this.serialize())
}

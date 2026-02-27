//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

@file:Suppress("ktlint:standard:filename") // We'll have more interfaces added later.

package io.zonarosa.libzonarosa.protocol.state.internal

import io.zonarosa.libzonarosa.internal.CalledFromNative
import io.zonarosa.libzonarosa.internal.NativeHandleGuard
import io.zonarosa.libzonarosa.internal.ObjectHandle

@CalledFromNative
internal interface PreKeyStore {
  @Throws(Exception::class)
  public fun loadPreKey(id: Int): NativeHandleGuard.Owner

  @Throws(Exception::class)
  public fun storePreKey(
    id: Int,
    rawPreKey: ObjectHandle,
  )

  @Throws(Exception::class)
  public fun removePreKey(id: Int)
}

@CalledFromNative
internal interface SignedPreKeyStore {
  @Throws(Exception::class)
  public fun loadSignedPreKey(id: Int): NativeHandleGuard.Owner

  @Throws(Exception::class)
  public fun storeSignedPreKey(
    id: Int,
    rawSignedPreKey: ObjectHandle,
  )
}

@CalledFromNative
internal interface KyberPreKeyStore {
  @Throws(Exception::class)
  public fun loadKyberPreKey(id: Int): NativeHandleGuard.Owner

  @Throws(Exception::class)
  public fun storeKyberPreKey(
    id: Int,
    rawKyberPreKey: ObjectHandle,
  )

  @Throws(Exception::class)
  public fun markKyberPreKeyUsed(
    id: Int,
    ecPrekeyId: Int,
    rawBaseKey: ObjectHandle,
  )
}

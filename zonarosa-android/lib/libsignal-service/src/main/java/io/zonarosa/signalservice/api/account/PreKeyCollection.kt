/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.account

import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord

/**
 * Holder class to pass around a bunch of prekeys that we send off to the service during registration.
 * As the service does not return the submitted prekeys, we need to hold them in memory so that when
 * the service approves the keys we have a local copy to persist.
 */
data class PreKeyCollection(
  val identityKey: IdentityKey,
  val signedPreKey: SignedPreKeyRecord,
  val lastResortKyberPreKey: KyberPreKeyRecord
)

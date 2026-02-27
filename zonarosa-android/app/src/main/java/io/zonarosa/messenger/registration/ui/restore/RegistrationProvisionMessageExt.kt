/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.restore

import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ecc.ECPrivateKey
import io.zonarosa.registration.proto.RegistrationProvisionMessage
import java.security.InvalidKeyException

/**
 * Attempt to parse the ACI identity key pair from the proto message parts.
 */
val RegistrationProvisionMessage.aciIdentityKeyPair: IdentityKeyPair?
  get() {
    return try {
      IdentityKeyPair(
        IdentityKey(aciIdentityKeyPublic.toByteArray()),
        ECPrivateKey(aciIdentityKeyPrivate.toByteArray())
      )
    } catch (_: InvalidKeyException) {
      null
    }
  }

/**
 * Attempt to parse the PNI identity key pair from the proto message parts.
 */
val RegistrationProvisionMessage.pniIdentityKeyPair: IdentityKeyPair?
  get() {
    return try {
      IdentityKeyPair(
        IdentityKey(pniIdentityKeyPublic.toByteArray()),
        ECPrivateKey(pniIdentityKeyPrivate.toByteArray())
      )
    } catch (_: InvalidKeyException) {
      null
    }
  }

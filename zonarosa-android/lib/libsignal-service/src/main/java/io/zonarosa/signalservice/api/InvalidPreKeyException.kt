/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api

import io.zonarosa.libzonarosa.protocol.InvalidKeyException
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import java.io.IOException

/**
 * Wraps an [InvalidKeyException] in an [IOException] with a nicer message.
 */
class InvalidPreKeyException(
  address: ZonaRosaProtocolAddress,
  invalidKeyException: InvalidKeyException
) : IOException("Invalid prekey for $address", invalidKeyException)

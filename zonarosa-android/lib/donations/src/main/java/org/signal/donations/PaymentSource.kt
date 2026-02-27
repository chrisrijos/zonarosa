/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.donations

import org.json.JSONObject

/**
 * A PaymentSource, being something that can be used to perform a
 * transaction. See [PaymentSourceType].
 */
interface PaymentSource {
  val type: PaymentSourceType
  fun parameterize(): JSONObject = error("Unsupported by $type.")
  fun getTokenId(): String = error("Unsupported by $type.")
  fun email(): String? = error("Unsupported by $type.")
}

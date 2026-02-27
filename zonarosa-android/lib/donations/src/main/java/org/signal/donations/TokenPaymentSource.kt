/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.donations

import org.json.JSONObject

class TokenPaymentSource(
  override val type: PaymentSourceType,
  val parameters: String,
  val token: String,
  val email: String?
) : PaymentSource {

  override fun parameterize(): JSONObject = JSONObject(parameters)

  override fun getTokenId(): String = token

  override fun email(): String? = email
}

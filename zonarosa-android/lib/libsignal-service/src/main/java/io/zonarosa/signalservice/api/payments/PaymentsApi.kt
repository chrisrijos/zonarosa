/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.payments

import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.push.AuthCredentials
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage

/**
 * Provide payments specific network apis.
 */
class PaymentsApi(private val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket) {

  /**
   * GET /v1/payments/auth
   * - 200: Success
   */
  fun getAuthorization(): NetworkResult<AuthCredentials> {
    val request = WebSocketRequestMessage.get("/v1/payments/auth")
    return NetworkResult.fromWebSocketRequest(authWebSocket, request, AuthCredentials::class)
  }

  /**
   * GET /v1/payments/conversions
   * - 200: Success
   */
  fun getCurrencyConversions(): NetworkResult<CurrencyConversions> {
    val request = WebSocketRequestMessage.get("/v1/payments/conversions")
    return NetworkResult.fromWebSocketRequest(authWebSocket, request, CurrencyConversions::class)
  }
}

/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.certificate

import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.push.SenderCertificate
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage

/**
 * Endpoints to get [SenderCertificate]s.
 */
class CertificateApi(private val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket) {

  /**
   * GET /v1/certificate/delivery
   * - 200: Success
   */
  fun getSenderCertificate(): NetworkResult<ByteArray> {
    val request = WebSocketRequestMessage.get("/v1/certificate/delivery")
    return NetworkResult.fromWebSocketRequest(authWebSocket, request, SenderCertificate::class)
      .map { it.certificate }
  }

  /**
   * GET /v1/certificate/delivery?includeE164=false
   * - 200: Success
   */
  fun getSenderCertificateForPhoneNumberPrivacy(): NetworkResult<ByteArray> {
    val request = WebSocketRequestMessage.get("/v1/certificate/delivery?includeE164=false")
    return NetworkResult.fromWebSocketRequest(authWebSocket, request, SenderCertificate::class)
      .map { it.certificate }
  }
}

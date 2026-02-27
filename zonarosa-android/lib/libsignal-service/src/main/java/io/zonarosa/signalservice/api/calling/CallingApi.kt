/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.calling

import io.zonarosa.libzonarosa.zkgroup.calllinks.CreateCallLinkCredentialRequest
import io.zonarosa.libzonarosa.zkgroup.calllinks.CreateCallLinkCredentialResponse
import io.zonarosa.storageservice.protos.calls.quality.SubmitCallQualitySurveyRequest
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.messages.calls.CallingResponse
import io.zonarosa.service.api.messages.calls.TurnServerInfo
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.post
import io.zonarosa.service.internal.push.CreateCallLinkAuthRequest
import io.zonarosa.service.internal.push.CreateCallLinkAuthResponse
import io.zonarosa.service.internal.push.GetCallingRelaysResponse
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.putCustom
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage

/**
 * Provide calling specific network apis.
 */
class CallingApi(
  private val auth: ZonaRosaWebSocket.AuthenticatedWebSocket,
  private val unAuth: ZonaRosaWebSocket.UnauthenticatedWebSocket,
  private val pushServiceSocket: PushServiceSocket
) {

  /**
   * Submit call quality information (with the user's permission) to the server on an unauthenticated channel.
   *
   * PUT /v1/call_quality_survey
   * - 204: The survey response was submitted successfully
   * - 422: The survey response could not be parsed
   * - 429: Too many attempts, try after Retry-After seconds.
   */
  fun submitCallQualitySurvey(request: SubmitCallQualitySurveyRequest): NetworkResult<Unit> {
    val webSocketRequestMessage = WebSocketRequestMessage.putCustom(
      path = "/v1/call_quality_survey",
      body = request.encode(),
      headers = mapOf("Content-Type" to "application/octet-stream")
    )

    return NetworkResult.fromWebSocketRequest(unAuth, webSocketRequestMessage)
  }

  /**
   * Get 1:1 relay addresses in IpV4, Ipv6, and URL formats.
   *
   * GET /v2/calling/relays
   * - 200: Success
   * - 400: Invalid request
   * - 422: Invalid request format
   * - 429: Rate limited
   */
  fun getTurnServerInfo(): NetworkResult<List<TurnServerInfo>> {
    val request = WebSocketRequestMessage.get("/v2/calling/relays")
    return NetworkResult.fromWebSocketRequest(auth, request, GetCallingRelaysResponse::class)
      .map { it.relays ?: emptyList() }
  }

  /**
   * Generate a call link credential.
   *
   * POST /v1/call-link/create-auth
   * - 200: Success
   * - 400: Invalid request
   * - 422: Invalid request format
   * - 429: Rate limited
   */
  fun createCallLinkCredential(request: CreateCallLinkCredentialRequest): NetworkResult<CreateCallLinkCredentialResponse> {
    val request = WebSocketRequestMessage.post("/v1/call-link/create-auth", body = CreateCallLinkAuthRequest.create(request))
    return NetworkResult.fromWebSocketRequest(auth, request, CreateCallLinkAuthResponse::class)
      .map { it.createCallLinkCredentialResponse }
  }

  /**
   * Send an http request on behalf of the calling infrastructure. Only returns [NetworkResult.Success] with the
   * wrapped [CallingResponse] wrapping the error which in practice should never happen.
   *
   * @param requestId Request identifier
   * @param url Fully qualified URL to request
   * @param httpMethod Http method to use (e.g., "GET", "POST")
   * @param headers Optional list of headers to send with request
   * @param body Optional body to send with request
   * @return
   */
  fun makeCallingRequest(
    requestId: Long,
    url: String,
    httpMethod: String,
    headers: List<Pair<String, String>>?,
    body: ByteArray?
  ): NetworkResult<CallingResponse> {
    return when (val result = NetworkResult.fromFetch { pushServiceSocket.makeCallingRequest(requestId, url, httpMethod, headers, body) }) {
      is NetworkResult.Success -> result
      else -> NetworkResult.Success(CallingResponse.Error(requestId, result.getCause()))
    }
  }
}

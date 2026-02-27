/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.remoteconfig

import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.util.JsonUtil
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage
import io.zonarosa.service.internal.websocket.WebsocketResponse
import java.util.Locale

/**
 * Remote configuration is a list of namespaced keys that clients may use for consistent configuration or behavior.
 *
 * Configuration values change over time, and the list should be refreshed periodically.
 */
class RemoteConfigApi(val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, val pushServiceSocket: PushServiceSocket) {

  /**
   * Get remote config data from the server.
   *
   * GET /v2/config
   * - 200: Success
   * - 304: No changes since the last fetch
   * - 401: Requires authentication
   */
  fun getRemoteConfig(eTag: String = ""): NetworkResult<RemoteConfigResult> {
    val headers = if (eTag.isNotEmpty()) mapOf("If-None-Match" to eTag) else mapOf()
    val request = WebSocketRequestMessage.get("/v2/config", headers = headers)
    return NetworkResult.fromWebSocketRequest(zonarosaWebSocket = authWebSocket, request = request, webSocketResponseConverter = RemoteConfigResultWebSocketResponseConverter())
      .fallback(predicate = { it is NetworkResult.StatusCodeError && it.code != 304 }) {
        NetworkResult.fromFetch {
          val response = pushServiceSocket.getRemoteConfig()
          val transformed = response.config.map { it.key to (it.value.lowercase(Locale.getDefault()).toBooleanStrictOrNull() ?: it.value) }.toMap()
          RemoteConfigResult(
            config = transformed,
            serverEpochTimeMilliseconds = response.serverEpochTime
          )
        }
      }
  }

  /**
   * Custom converter for [RemoteConfigResult] as it needs the value of the timestamp header to construct the
   * complete result, not just the JSON body.
   */
  private class RemoteConfigResultWebSocketResponseConverter : NetworkResult.WebSocketResponseConverter<RemoteConfigResult> {
    override fun convert(response: WebsocketResponse): NetworkResult<RemoteConfigResult> {
      return if (response.status < 200 || response.status > 299) {
        response.toStatusCodeError()
      } else {
        val remoteConfigResponse = JsonUtil.fromJson(response.body, RemoteConfigResponse::class.java)
        val transformed = remoteConfigResponse.config.map { it.key to (it.value.lowercase(Locale.getDefault()).toBooleanStrictOrNull() ?: it.value) }.toMap()

        NetworkResult.Success(
          RemoteConfigResult(
            config = transformed,
            serverEpochTimeMilliseconds = response.getHeader(ZonaRosaWebSocket.SERVER_DELIVERED_TIMESTAMP_HEADER).toLongOrNull() ?: System.currentTimeMillis(),
            eTag = response.headers["etag"]
          )
        )
      }
    }
  }
}

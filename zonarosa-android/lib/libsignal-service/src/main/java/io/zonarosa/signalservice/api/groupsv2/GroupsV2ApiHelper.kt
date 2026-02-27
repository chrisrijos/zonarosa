/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.groupsv2

import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage
import java.io.IOException
import kotlin.time.Duration.Companion.days

/**
 * Allow [GroupsV2Api] to have a partial kotlin conversion by putting more kotlin friendly calls here.
 */
object GroupsV2ApiHelper {
  /**
   * Provides 7 days of credentials, which you should cache.
   *
   * GET /v1/certificate/auth/group?redemptionStartSeconds=[todaySeconds]&redemptionEndSeconds=`todaySecondsPlus7DaysOfSeconds`
   * - 200: Success
   */
  @JvmStatic
  @Throws(IOException::class)
  fun getCredentials(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, todaySeconds: Long): CredentialResponse {
    val todayPlus7 = todaySeconds + 7.days.inWholeSeconds
    val request = WebSocketRequestMessage.get("/v1/certificate/auth/group?redemptionStartSeconds=$todaySeconds&redemptionEndSeconds=$todayPlus7")
    return NetworkResult.fromWebSocketRequest(authWebSocket, request, CredentialResponse::class).successOrThrow()
  }
}

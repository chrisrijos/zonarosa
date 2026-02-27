/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.internal.websocket

import io.zonarosa.libzonarosa.net.ChatConnection.Response

fun Response.toWebsocketResponse(isUnidentified: Boolean): WebsocketResponse {
  return WebsocketResponse(
    this.status,
    this.body.decodeToString(),
    this.headers,
    isUnidentified
  )
}

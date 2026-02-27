/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.provisioning

import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.urlEncode
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey
import io.zonarosa.registration.proto.RegistrationProvisionMessage
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.registration.RestoreMethodBody
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.crypto.PrimaryProvisioningCipher
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.put
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage
import kotlin.time.Duration.Companion.seconds

/**
 * Linked and new device provisioning endpoints.
 */
class ProvisioningApi(private val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, private val unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket) {

  /**
   * Encrypts and sends the [registrationProvisionMessage] from the current primary (old device) to the new device over
   * the provisioning web socket identified by [deviceIdentifier].
   *
   * PUT /v1/provisioning/[deviceIdentifier]
   * - 204: Success
   * - 400: Message was too large
   * - 404: No provisioning socket for [deviceIdentifier]
   */
  fun sendReRegisterDeviceProvisioningMessage(
    deviceIdentifier: String,
    deviceKey: ECPublicKey,
    registrationProvisionMessage: RegistrationProvisionMessage
  ): NetworkResult<Unit> {
    val cipherText = PrimaryProvisioningCipher(deviceKey).encrypt(registrationProvisionMessage)

    val request = WebSocketRequestMessage.put("/v1/provisioning/${deviceIdentifier.urlEncode()}", ProvisioningMessage(Base64.encodeWithPadding(cipherText)))
    return NetworkResult.fromWebSocketRequest(authWebSocket, request)
  }

  /**
   * Wait for the [RestoreMethod] to be set on the server by the new device. This is a long polling operation.
   *
   * GET /v1/devices/restore_account/[token]?timeout=[timeout]
   * - 200: A request was received for the given token
   * - 204: No request given yet, may repeat call to continue waiting
   * - 400: Invalid [token] or [timeout]
   * - 429: Rate limited
   */
  fun waitForRestoreMethod(token: String, timeout: Int = 30): NetworkResult<RestoreMethod> {
    val request = WebSocketRequestMessage.get("/v1/devices/restore_account/${token.urlEncode()}?timeout=$timeout")

    return NetworkResult.fromWebSocket(NetworkResult.LongPollingWebSocketConverter(RestoreMethodBody::class)) {
      unauthWebSocket.request(request, timeout.seconds)
    }.map {
      it.method ?: RestoreMethod.DECLINE
    }
  }
}

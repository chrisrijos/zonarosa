/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.username

import kotlinx.coroutines.runBlocking
import io.zonarosa.core.models.ServiceId
import io.zonarosa.libzonarosa.net.LookUpUsernameLinkFailure
import io.zonarosa.libzonarosa.net.RequestResult
import io.zonarosa.libzonarosa.net.UnauthUsernamesService
import io.zonarosa.libzonarosa.net.getOrError
import io.zonarosa.libzonarosa.usernames.Username
import io.zonarosa.service.api.account.AccountApi
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.get
import java.util.UUID

/**
 * Username specific APIs related to learning service information for someone else by username.
 * For APIs to manage your own username, see [AccountApi].
 */
class UsernameApi(private val unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket) {

  /**
   * Gets the ACI for the given [username]. This is an unauthenticated request.
   *
   * A successful result with a null value means the username was not found on the server.
   * Other errors (network, decryption, etc.) are represented by the other [RequestResult] types.
   */
  fun getAciByUsername(username: Username): RequestResult<ServiceId.ACI?, Nothing> {
    return runBlocking {
      unauthWebSocket.runCatchingWithUnauthChatConnection { chatConnection ->
        UnauthUsernamesService(chatConnection).lookUpUsernameHash(username.hash)
      }.getOrError().map { it?.let { ServiceId.ACI.fromLibZonaRosa(it) } }
    }
  }

  /**
   * Gets the username for a ([serverId], [entropy]) pairing from a username link. This is an unauthenticated request.
   *
   * A successful result with a null value means no username link was found for the given server ID.
   * Other errors (network, decryption, etc.) are represented by the other [RequestResult] types.
   */
  fun getDecryptedUsernameFromLinkServerIdAndEntropy(serverId: UUID, entropy: ByteArray): RequestResult<Username?, LookUpUsernameLinkFailure> {
    return runBlocking {
      unauthWebSocket.runCatchingWithUnauthChatConnection { chatConnection ->
        UnauthUsernamesService(chatConnection).lookUpUsernameLink(serverId, entropy)
      }.getOrError()
    }
  }
}

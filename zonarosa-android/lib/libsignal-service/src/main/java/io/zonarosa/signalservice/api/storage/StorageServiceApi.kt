/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import okhttp3.Credentials
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.get
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.storage.protos.ReadOperation
import io.zonarosa.service.internal.storage.protos.StorageItems
import io.zonarosa.service.internal.storage.protos.StorageManifest
import io.zonarosa.service.internal.storage.protos.WriteOperation
import io.zonarosa.service.internal.websocket.WebSocketRequestMessage

/**
 * Class to interact with storage service endpoints.
 */
class StorageServiceApi(
  private val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket,
  private val pushServiceSocket: PushServiceSocket
) {

  /**
   * Retrieves an auth string that's needed to make other storage requests.
   *
   * GET /v1/storage/auth
   */
  fun getAuth(): NetworkResult<String> {
    val request = WebSocketRequestMessage.get("/v1/storage/auth")
    return NetworkResult.fromWebSocketRequest(authWebSocket, request, StorageAuthResponse::class)
      .map { Credentials.basic(it.username, it.password) }
  }

  /**
   * Gets the latest [StorageManifest].
   *
   * GET /v1/storage/manifest
   *
   * - 200: Success
   * - 404: No storage manifest was found
   */
  fun getStorageManifest(authToken: String): NetworkResult<StorageManifest> {
    return NetworkResult.fromFetch {
      pushServiceSocket.getStorageManifest(authToken)
    }
  }

  /**
   * Gets the latest [StorageManifest], but only if the version supplied doesn't match the remote.
   *
   * GET /v1/storage/manifest/version/{version}
   *
   * - 200: Success
   * - 204: The manifest matched the provided version, and therefore no manifest was returned
   */
  fun getStorageManifestIfDifferentVersion(authToken: String, version: Long): NetworkResult<StorageManifest> {
    return NetworkResult.fromFetch {
      pushServiceSocket.getStorageManifestIfDifferentVersion(authToken, version)
    }
  }

  /**
   * PUT /v1/storage/read
   */
  fun readStorageItems(authToken: String, operation: ReadOperation): NetworkResult<StorageItems> {
    return NetworkResult.fromFetch {
      pushServiceSocket.readStorageItems(authToken, operation)
    }
  }

  /**
   * Performs the provided [WriteOperation].
   *
   * PUT /v1/storage
   *
   * - 200: Success
   * - 409: Your [WriteOperation] version does not equal remoteVersion + 1. That means that there have been writes that you're not aware of.
   *   The body includes the current [StorageManifest] as binary data.
   */
  fun writeStorageItems(authToken: String, writeOperation: WriteOperation): NetworkResult<Unit> {
    return NetworkResult.fromFetch {
      pushServiceSocket.writeStorageItems(authToken, writeOperation)
    }
  }

  /**
   * Lets you know if storage service is reachable.
   *
   * GET /ping
   */
  fun pingStorageService(): NetworkResult<Unit> {
    return NetworkResult.fromFetch {
      pushServiceSocket.pingStorageService()
    }
  }
}

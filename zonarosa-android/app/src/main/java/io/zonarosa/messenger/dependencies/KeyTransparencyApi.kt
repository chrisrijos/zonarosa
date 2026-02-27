package io.zonarosa.messenger.dependencies

import io.zonarosa.libzonarosa.internal.mapWithCancellation
import io.zonarosa.libzonarosa.keytrans.KeyTransparencyException
import io.zonarosa.libzonarosa.keytrans.VerificationFailedException
import io.zonarosa.libzonarosa.net.AppExpiredException
import io.zonarosa.libzonarosa.net.BadRequestError
import io.zonarosa.libzonarosa.net.ChatServiceException
import io.zonarosa.libzonarosa.net.KeyTransparency
import io.zonarosa.libzonarosa.net.NetworkException
import io.zonarosa.libzonarosa.net.NetworkProtocolException
import io.zonarosa.libzonarosa.net.RequestResult
import io.zonarosa.libzonarosa.net.RetryLaterException
import io.zonarosa.libzonarosa.net.ServerSideErrorException
import io.zonarosa.libzonarosa.net.TimeoutException
import io.zonarosa.libzonarosa.net.getOrError
import io.zonarosa.libzonarosa.protocol.IdentityKey
import io.zonarosa.libzonarosa.protocol.ServiceId
import io.zonarosa.messenger.database.model.KeyTransparencyStore
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket

/**
 * Operations used when interacting with [io.zonarosa.libzonarosa.net.KeyTransparencyClient]
 */
class KeyTransparencyApi(private val unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket) {

  /**
   * Uses KT to verify recipient. This is an unauthenticated and should only be called the first time KT is being requested for this recipient.
   */
  suspend fun search(aci: ServiceId.Aci, aciIdentityKey: IdentityKey, e164: String?, unidentifiedAccessKey: ByteArray?, usernameHash: ByteArray?, keyTransparencyStore: KeyTransparencyStore): RequestResult<Unit, KeyTransparencyError> {
    return unauthWebSocket.runCatchingWithUnauthChatConnection { chatConnection ->
      chatConnection.keyTransparencyClient().search(aci, aciIdentityKey, e164, unidentifiedAccessKey, usernameHash, keyTransparencyStore)
        .mapWithCancellation(
          onSuccess = { RequestResult.Success(Unit) },
          onError = { throwable ->
            when (throwable) {
              is VerificationFailedException,
              is KeyTransparencyException,
              is AppExpiredException,
              is IllegalArgumentException -> {
                RequestResult.NonSuccess(KeyTransparencyError(throwable))
              }
              is ChatServiceException,
              is NetworkException,
              is NetworkProtocolException -> {
                RequestResult.RetryableNetworkError(throwable, null)
              }
              is RetryLaterException -> {
                RequestResult.RetryableNetworkError(throwable, throwable.duration)
              }
              else -> {
                RequestResult.ApplicationError(throwable)
              }
            }
          }
        )
    }.getOrError()
  }

  /**
   * Monitors KT to verify recipient. This is an unauthenticated and should only be called following a successful [search].
   */
  suspend fun monitor(monitorMode: KeyTransparency.MonitorMode, aci: ServiceId.Aci, aciIdentityKey: IdentityKey, e164: String?, unidentifiedAccessKey: ByteArray?, usernameHash: ByteArray?, keyTransparencyStore: KeyTransparencyStore): RequestResult<Unit, KeyTransparencyError> {
    return unauthWebSocket.runCatchingWithUnauthChatConnection { chatConnection ->
      chatConnection.keyTransparencyClient().monitor(monitorMode, aci, aciIdentityKey, e164, unidentifiedAccessKey, usernameHash, keyTransparencyStore)
        .mapWithCancellation(
          onSuccess = { RequestResult.Success(Unit) },
          onError = { throwable ->
            when (throwable) {
              is TimeoutException,
              is ServerSideErrorException,
              is NetworkException,
              is NetworkProtocolException -> {
                RequestResult.RetryableNetworkError(throwable, null)
              }
              is RetryLaterException -> {
                RequestResult.RetryableNetworkError(throwable, throwable.duration)
              }
              is VerificationFailedException,
              is KeyTransparencyException,
              is AppExpiredException,
              is IllegalArgumentException -> {
                RequestResult.NonSuccess(KeyTransparencyError(throwable))
              }
              else -> {
                RequestResult.ApplicationError(throwable)
              }
            }
          }
        )
    }.getOrError()
  }
}

data class KeyTransparencyError(val exception: Throwable) : BadRequestError

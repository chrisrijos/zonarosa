//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net

import io.zonarosa.libzonarosa.internal.CompletableFuture
import io.zonarosa.libzonarosa.internal.Native
import io.zonarosa.libzonarosa.internal.mapWithCancellation
import io.zonarosa.libzonarosa.protocol.ServiceId
import io.zonarosa.libzonarosa.usernames.Username
import java.util.UUID

public class UnauthUsernamesService(
  private val connection: UnauthenticatedChatConnection,
) {
  /**
   * Looks up a username hash on the service, like that computed by
   * [io.zonarosa.libzonarosa.usernames.Username].
   *
   * Produces the corresponding account's ACI, or `null` if the username doesn't correspond to an
   * account.
   *
   * All exceptions are mapped into [RequestResult]; unexpected ones will be treated as
   * [RequestResult.ApplicationError].
   */
  public fun lookUpUsernameHash(hash: ByteArray): CompletableFuture<RequestResult<ServiceId.Aci?, Nothing>> =
    try {
      connection
        .runWithContextAndConnectionHandles { asyncCtx, conn ->
          Native.UnauthenticatedChatConnection_look_up_username_hash(asyncCtx, conn, hash)
        }.mapWithCancellation(
          onSuccess = { uuid -> RequestResult.Success(uuid?.let(ServiceId::Aci)) },
          onError = { err -> err.toRequestResult() },
        )
    } catch (e: Throwable) {
      CompletableFuture.completedFuture(RequestResult.ApplicationError(e))
    }

  /**
   * Looks up a username link on the service by UUID.
   *
   * Returns a decrypted, validated username, or `null` if the UUID does not correspond to a
   * username link (perhaps the user rotated their link).
   *
   * All exceptions are mapped into [RequestResult]; unexpected ones will be treated as
   * [RequestResult.ApplicationError].
   */
  public fun lookUpUsernameLink(
    uuid: UUID,
    entropy: ByteArray,
  ): CompletableFuture<RequestResult<Username?, LookUpUsernameLinkFailure>> =
    try {
      connection
        .runWithContextAndConnectionHandles { asyncCtx, conn ->
          Native.UnauthenticatedChatConnection_look_up_username_link(asyncCtx, conn, uuid, entropy)
        }.mapWithCancellation(
          onSuccess = { pair ->
            if (pair == null) {
              RequestResult.Success(null)
            } else {
              RequestResult.Success(Username._withPrecomputedHash(pair.first, pair.second))
            }
          },
          onError = { err -> err.toRequestResult<LookUpUsernameLinkFailure>() },
        )
    } catch (e: Throwable) {
      CompletableFuture.completedFuture(e.toRequestResult<LookUpUsernameLinkFailure>())
    }
}

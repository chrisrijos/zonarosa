//
// Copyright 2026 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net

import io.zonarosa.libzonarosa.internal.CompletableFuture
import io.zonarosa.libzonarosa.internal.Native
import io.zonarosa.libzonarosa.internal.mapWithCancellation
import io.zonarosa.libzonarosa.protocol.ServiceId

public class UnauthProfilesService(
  private val connection: UnauthenticatedChatConnection,
) {
  /**
   * Does an account with the given ACI or PNI exist?
   *
   * All exceptions are mapped into [RequestResult]; unexpected ones will be treated as
   * [RequestResult.ApplicationError].
   */
  public fun accountExists(account: ServiceId): CompletableFuture<RequestResult<Boolean, Nothing>> =
    try {
      connection.runWithContextAndConnectionHandles { asyncCtx, conn ->
        Native
          .UnauthenticatedChatConnection_account_exists(
            asyncCtx,
            conn,
            account.toServiceIdFixedWidthBinary(),
          ).mapWithCancellation(
            onSuccess = { RequestResult.Success(it) },
            onError = { err -> err.toRequestResult() },
          )
      }
    } catch (e: Throwable) {
      CompletableFuture.completedFuture(RequestResult.ApplicationError(e))
    }
}

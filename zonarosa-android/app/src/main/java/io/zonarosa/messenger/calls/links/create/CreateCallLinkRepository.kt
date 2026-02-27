/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links.create

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.CallLinkUpdateSendJob
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.service.webrtc.links.CreateCallLinkResult
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkManager
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.service.internal.push.SyncMessage

/**
 * Repository for creating new call links. This will delegate to the [ZonaRosaCallLinkManager]
 * but will also ensure the database is updated.
 */
class CreateCallLinkRepository(
  private val callLinkManager: ZonaRosaCallLinkManager = AppDependencies.zonarosaCallManager.callLinkManager
) {
  fun ensureCallLinkCreated(credentials: CallLinkCredentials): Single<EnsureCallLinkCreatedResult> {
    val callLinkRecipientId = Single.fromCallable {
      ZonaRosaDatabase.recipients.getByCallLinkRoomId(credentials.roomId)
    }

    return callLinkRecipientId.flatMap { recipientId ->
      if (recipientId.isPresent) {
        Single.just(EnsureCallLinkCreatedResult.Success(Recipient.resolved(recipientId.get())))
      } else {
        callLinkManager.createCallLink(credentials).map {
          when (it) {
            is CreateCallLinkResult.Success -> {
              ZonaRosaDatabase.callLinks.insertCallLink(
                CallLinkTable.CallLink(
                  recipientId = RecipientId.UNKNOWN,
                  roomId = it.credentials.roomId,
                  credentials = it.credentials,
                  state = it.state,
                  deletionTimestamp = 0L
                )
              )

              AppDependencies.jobManager.add(
                CallLinkUpdateSendJob(
                  it.credentials.roomId,
                  SyncMessage.CallLinkUpdate.Type.UPDATE
                )
              )

              StorageSyncHelper.scheduleSyncForDataChange()

              EnsureCallLinkCreatedResult.Success(
                Recipient.resolved(
                  ZonaRosaDatabase.recipients.getByCallLinkRoomId(it.credentials.roomId).get()
                )
              )
            }

            is CreateCallLinkResult.Failure -> EnsureCallLinkCreatedResult.Failure(it)
          }
        }
      }
    }.subscribeOn(Schedulers.io())
  }
}

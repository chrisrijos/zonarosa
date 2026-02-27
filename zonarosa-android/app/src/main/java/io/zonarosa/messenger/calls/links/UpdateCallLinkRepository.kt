/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links

import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.ringrtc.CallLinkState
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.CallLinkUpdateSendJob
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkManager
import io.zonarosa.messenger.service.webrtc.links.UpdateCallLinkResult
import io.zonarosa.messenger.storage.StorageSyncHelper

/**
 * Repository for performing update operations on call links:
 * <ul>
 *   <li>Set name</li>
 *   <li>Set restrictions</li>
 *   <li>Revoke link</li>
 * </ul>
 *
 * All of these will delegate to the [ZonaRosaCallLinkManager] but will additionally update the database state.
 */
class UpdateCallLinkRepository(
  private val callLinkManager: ZonaRosaCallLinkManager = AppDependencies.zonarosaCallManager.callLinkManager
) {
  fun setCallName(credentials: CallLinkCredentials, name: String): Single<UpdateCallLinkResult> {
    return callLinkManager
      .updateCallLinkName(
        credentials = credentials,
        name = name
      )
      .doOnSuccess(updateState(credentials))
      .subscribeOn(Schedulers.io())
  }

  fun setCallRestrictions(credentials: CallLinkCredentials, restrictions: CallLinkState.Restrictions): Single<UpdateCallLinkResult> {
    return callLinkManager
      .updateCallLinkRestrictions(
        credentials = credentials,
        restrictions = restrictions
      )
      .doOnSuccess(updateState(credentials))
      .subscribeOn(Schedulers.io())
  }

  fun deleteCallLink(credentials: CallLinkCredentials): Single<UpdateCallLinkResult> {
    return callLinkManager
      .deleteCallLink(credentials)
      .doOnSuccess(updateState(credentials))
      .subscribeOn(Schedulers.io())
  }

  private fun updateState(credentials: CallLinkCredentials): (UpdateCallLinkResult) -> Unit {
    return { result ->
      when (result) {
        is UpdateCallLinkResult.Update -> {
          ZonaRosaDatabase.callLinks.updateCallLinkState(credentials.roomId, result.state)
          AppDependencies.jobManager.add(CallLinkUpdateSendJob(credentials.roomId))
        }
        is UpdateCallLinkResult.Delete -> {
          ZonaRosaDatabase.callLinks.markRevoked(credentials.roomId)
          AppDependencies.jobManager.add(CallLinkUpdateSendJob(credentials.roomId))
          StorageSyncHelper.scheduleSyncForDataChange()
        }
        else -> {}
      }
    }
  }
}

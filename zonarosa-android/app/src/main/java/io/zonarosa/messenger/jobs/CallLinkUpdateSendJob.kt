/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.protos.CallLinkUpdateSendJobData
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.api.push.exceptions.PushNetworkException
import io.zonarosa.service.api.push.exceptions.ServerRejectedException
import io.zonarosa.service.internal.push.SyncMessage.CallLinkUpdate
import java.util.concurrent.TimeUnit

/**
 * Sends a [CallLinkUpdate] message to linked devices.
 */
class CallLinkUpdateSendJob private constructor(
  parameters: Parameters,
  private val callLinkRoomId: CallLinkRoomId,
  private val callLinkUpdateType: CallLinkUpdate.Type
) : BaseJob(parameters) {

  companion object {
    const val KEY = "CallLinkUpdateSendJob"
    private val TAG = Log.tag(CallLinkUpdateSendJob::class.java)
  }

  constructor(
    callLinkRoomId: CallLinkRoomId,
    callLinkUpdateType: CallLinkUpdate.Type = CallLinkUpdate.Type.UPDATE
  ) : this(
    Parameters.Builder()
      .setQueue("CallLinkUpdateSendJob")
      .setLifespan(TimeUnit.DAYS.toMillis(1))
      .setMaxAttempts(Parameters.UNLIMITED)
      .addConstraint(NetworkConstraint.KEY)
      .build(),
    callLinkRoomId,
    callLinkUpdateType
  )

  override fun serialize(): ByteArray = CallLinkUpdateSendJobData.Builder()
    .callLinkRoomId(callLinkRoomId.serialize())
    .type(
      when (callLinkUpdateType) {
        CallLinkUpdate.Type.UPDATE -> CallLinkUpdateSendJobData.Type.UPDATE
      }
    )
    .build()
    .encode()

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    val callLink = ZonaRosaDatabase.callLinks.getCallLinkByRoomId(callLinkRoomId)
    if (callLink?.credentials == null) {
      Log.i(TAG, "Call link not found or missing credentials. Exiting.")
      return
    }

    val callLinkUpdate = CallLinkUpdate(
      rootKey = callLink.credentials.linkKeyBytes.toByteString(),
      adminPasskey = callLink.credentials.adminPassBytes?.toByteString(),
      type = callLinkUpdateType
    )

    AppDependencies.zonarosaServiceMessageSender
      .sendSyncMessage(ZonaRosaServiceSyncMessage.forCallLinkUpdate(callLinkUpdate))
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return when (e) {
      is ServerRejectedException -> false
      is PushNetworkException -> true
      else -> false
    }
  }

  class Factory : Job.Factory<CallLinkUpdateSendJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): CallLinkUpdateSendJob {
      val jobData = CallLinkUpdateSendJobData.ADAPTER.decode(serializedData!!)
      val type: CallLinkUpdate.Type = when (jobData.type) {
        CallLinkUpdateSendJobData.Type.UPDATE, null -> CallLinkUpdate.Type.UPDATE
      }

      return CallLinkUpdateSendJob(
        parameters,
        CallLinkRoomId.DatabaseSerializer.deserialize(jobData.callLinkRoomId),
        type
      )
    }
  }
}

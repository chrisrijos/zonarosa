/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import androidx.annotation.WorkerThread
import okio.ByteString.Companion.toByteString
import io.zonarosa.messenger.database.CallTable
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.protos.CallLogEventSendJobData
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.api.push.exceptions.PushNetworkException
import io.zonarosa.service.api.push.exceptions.ServerRejectedException
import io.zonarosa.service.internal.push.SyncMessage
import java.util.concurrent.TimeUnit

/**
 * Sends CallLogEvents to synced devices.
 */
class CallLogEventSendJob private constructor(
  parameters: Parameters,
  private val callLogEvent: SyncMessage.CallLogEvent
) : BaseJob(parameters) {

  companion object {
    const val KEY = "CallLogEventSendJob"

    @WorkerThread
    fun forClearHistory(
      call: CallTable.Call
    ) = CallLogEventSendJob(
      Parameters.Builder()
        .setQueue("CallLogEventSendJob")
        .setLifespan(TimeUnit.DAYS.toMillis(1))
        .setMaxAttempts(Parameters.UNLIMITED)
        .addConstraint(NetworkConstraint.KEY)
        .build(),
      SyncMessage.CallLogEvent(
        timestamp = call.timestamp,
        callId = call.callId,
        conversationId = Recipient.resolved(call.peer).requireCallConversationId().toByteString(),
        type = SyncMessage.CallLogEvent.Type.CLEAR
      )
    )

    @WorkerThread
    fun forMarkedAsRead(
      call: CallTable.Call
    ) = CallLogEventSendJob(
      Parameters.Builder()
        .setQueue("CallLogEventSendJob")
        .setLifespan(TimeUnit.DAYS.toMillis(1))
        .setMaxAttempts(Parameters.UNLIMITED)
        .addConstraint(NetworkConstraint.KEY)
        .build(),
      SyncMessage.CallLogEvent(
        timestamp = call.timestamp,
        callId = call.callId,
        conversationId = Recipient.resolved(call.peer).requireCallConversationId().toByteString(),
        type = SyncMessage.CallLogEvent.Type.MARKED_AS_READ
      )
    )

    @JvmStatic
    @WorkerThread
    fun forMarkedAsReadInConversation(
      call: CallTable.Call
    ) = CallLogEventSendJob(
      Parameters.Builder()
        .setQueue("CallLogEventSendJob")
        .setLifespan(TimeUnit.DAYS.toMillis(1))
        .setMaxAttempts(Parameters.UNLIMITED)
        .addConstraint(NetworkConstraint.KEY)
        .build(),
      SyncMessage.CallLogEvent(
        timestamp = call.timestamp,
        callId = call.callId,
        conversationId = Recipient.resolved(call.peer).requireCallConversationId().toByteString(),
        type = SyncMessage.CallLogEvent.Type.MARKED_AS_READ_IN_CONVERSATION
      )
    )
  }

  override fun serialize(): ByteArray = CallLogEventSendJobData.Builder()
    .callLogEvent(callLogEvent.encodeByteString())
    .build()
    .encode()

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    AppDependencies.zonarosaServiceMessageSender
      .sendSyncMessage(ZonaRosaServiceSyncMessage.forCallLogEvent(callLogEvent))
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return when (e) {
      is ServerRejectedException -> false
      is PushNetworkException -> true
      else -> false
    }
  }

  class Factory : Job.Factory<CallLogEventSendJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): CallLogEventSendJob {
      return CallLogEventSendJob(
        parameters,
        SyncMessage.CallLogEvent.ADAPTER.decode(
          CallLogEventSendJobData.ADAPTER.decode(serializedData!!).callLogEvent.toByteArray()
        )
      )
    }
  }
}

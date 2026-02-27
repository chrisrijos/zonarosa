/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.api.push.exceptions.PushNetworkException
import io.zonarosa.service.internal.push.SyncMessage.CallLinkUpdate
import kotlin.time.Duration.Companion.days

/**
 * Sends a sync message to linked devices when a new call link is created locally.
 */
// TODO [cody] not being created?
class MultiDeviceCallLinkSyncJob private constructor(
  parameters: Parameters,
  private val callLinkUpdate: CallLinkUpdate
) : BaseJob(parameters) {

  constructor(credentials: CallLinkCredentials) : this(
    Parameters.Builder()
      .setQueue("__MULTI_DEVICE_CALL_LINK_UPDATE_JOB__")
      .addConstraint(NetworkConstraint.KEY)
      .setLifespan(1.days.inWholeMilliseconds)
      .setMaxAttempts(Parameters.UNLIMITED)
      .build(),
    CallLinkUpdate(
      rootKey = credentials.linkKeyBytes.toByteString(),
      adminPasskey = credentials.adminPassBytes!!.toByteString()
    )
  )

  companion object {
    const val KEY = "MultiDeviceCallLinkSyncJob"

    private val TAG = Log.tag(MultiDeviceCallLinkSyncJob::class.java)
  }

  override fun serialize(): ByteArray {
    return callLinkUpdate.encode()
  }

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    val syncMessage = ZonaRosaServiceSyncMessage.forCallLinkUpdate(callLinkUpdate)

    try {
      AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(syncMessage)
    } catch (e: Exception) {
      Log.w(TAG, "Unable to send call link update message.", e)
      throw e
    }
  }

  override fun onShouldRetry(exception: Exception): Boolean {
    return when (exception) {
      is PushNetworkException -> true
      else -> false
    }
  }

  class Factory : Job.Factory<MultiDeviceCallLinkSyncJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): MultiDeviceCallLinkSyncJob {
      val data = CallLinkUpdate.ADAPTER.decode(serializedData!!)
      return MultiDeviceCallLinkSyncJob(parameters, data)
    }
  }
}

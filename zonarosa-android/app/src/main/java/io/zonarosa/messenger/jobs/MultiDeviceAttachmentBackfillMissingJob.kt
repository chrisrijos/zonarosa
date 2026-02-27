/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.protos.MultiDeviceAttachmentBackfillMissingJobData
import io.zonarosa.service.api.crypto.UntrustedIdentityException
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.api.push.exceptions.ServerRejectedException
import io.zonarosa.service.internal.push.AddressableMessage
import io.zonarosa.service.internal.push.ConversationIdentifier
import io.zonarosa.service.internal.push.SyncMessage
import java.io.IOException
import kotlin.time.Duration.Companion.days

/**
 * Tells linked devices that the requested message from a [SyncMessage.attachmentBackfillRequest] could not be found.
 */
class MultiDeviceAttachmentBackfillMissingJob(
  parameters: Parameters,
  private val targetMessage: AddressableMessage,
  private val targetConversation: ConversationIdentifier
) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(MultiDeviceAttachmentBackfillMissingJob::class)

    const val KEY = "MultiDeviceAttachmentBackfillMissingJob"

    fun enqueue(targetMessage: AddressableMessage, targetConversation: ConversationIdentifier) {
      AppDependencies.jobManager.add(MultiDeviceAttachmentBackfillMissingJob(targetMessage, targetConversation))
    }
  }

  constructor(targetMessage: AddressableMessage, targetConversation: ConversationIdentifier) : this(
    Parameters.Builder()
      .setLifespan(1.days.inWholeMilliseconds)
      .setMaxAttempts(Parameters.UNLIMITED)
      .addConstraint(NetworkConstraint.KEY)
      .build(),
    targetMessage,
    targetConversation
  )

  override fun getFactoryKey(): String = KEY

  override fun serialize(): ByteArray {
    return MultiDeviceAttachmentBackfillMissingJobData(
      targetMessage = targetMessage,
      targetConversation = targetConversation
    ).encode()
  }

  override fun run(): Result {
    val syncMessage = ZonaRosaServiceSyncMessage.forAttachmentBackfillResponse(
      SyncMessage.AttachmentBackfillResponse(
        targetMessage = targetMessage,
        targetConversation = targetConversation,
        error = SyncMessage.AttachmentBackfillResponse.Error.MESSAGE_NOT_FOUND
      )
    )

    return try {
      val result = AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(syncMessage)
      if (result.isSuccess) {
        Log.i(TAG, "[${targetMessage.sentTimestamp}] Successfully sent backfill missing message response.")
        Result.success()
      } else {
        Log.w(TAG, "[${targetMessage.sentTimestamp}] Non-successful result. Retrying.")
        Result.retry(defaultBackoff())
      }
    } catch (e: ServerRejectedException) {
      Log.w(TAG, e)
      Result.failure()
    } catch (e: IOException) {
      Log.w(TAG, e)
      Result.retry(defaultBackoff())
    } catch (e: UntrustedIdentityException) {
      Log.w(TAG, e)
      Result.failure()
    }
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<MultiDeviceAttachmentBackfillMissingJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): MultiDeviceAttachmentBackfillMissingJob {
      val data = MultiDeviceAttachmentBackfillMissingJobData.ADAPTER.decode(serializedData!!)
      return MultiDeviceAttachmentBackfillMissingJob(
        parameters,
        data.targetMessage!!,
        data.targetConversation!!
      )
    }
  }
}

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.JsonJobData
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.service.api.messages.ZonaRosaServiceStoryMessageRecipient
import io.zonarosa.service.api.messages.multidevice.SentTranscriptMessage
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import java.util.Optional
import java.util.concurrent.TimeUnit

/**
 * Transmits a sent sync transcript to linked devices containing the story sync manifest for the given sent timestamp.
 * The transmitted message will contain all current recipients of a given story.
 */
class MultiDeviceStorySendSyncJob private constructor(parameters: Parameters, private val sentTimestamp: Long, private val deletedMessageId: Long) : BaseJob(parameters) {

  companion object {
    const val KEY = "MultiDeviceStorySendSyncJob"

    private val TAG = Log.tag(MultiDeviceStorySendSyncJob::class.java)

    private const val DATA_SENT_TIMESTAMP = "sent.timestamp"
    private const val DATA_DELETED_MESSAGE_ID = "deleted.message.id"

    @JvmStatic
    fun create(sentTimestamp: Long, deletedMessageId: Long): MultiDeviceStorySendSyncJob {
      return MultiDeviceStorySendSyncJob(
        parameters = Parameters.Builder()
          .addConstraint(NetworkConstraint.KEY)
          .setMaxAttempts(Parameters.UNLIMITED)
          .setLifespan(TimeUnit.DAYS.toMillis(1))
          .setQueue(KEY)
          .build(),
        sentTimestamp = sentTimestamp,
        deletedMessageId = deletedMessageId
      )
    }
  }

  override fun serialize(): ByteArray? {
    return JsonJobData.Builder()
      .putLong(DATA_SENT_TIMESTAMP, sentTimestamp)
      .putLong(DATA_DELETED_MESSAGE_ID, deletedMessageId)
      .serialize()
  }

  override fun getFactoryKey(): String = KEY

  override fun onRun() {
    val updateManifest = ZonaRosaDatabase.storySends.getLocalManifest(sentTimestamp)
    val recipientsSet: Set<ZonaRosaServiceStoryMessageRecipient> = updateManifest.toRecipientsSet()
    val transcriptMessage: ZonaRosaServiceSyncMessage = ZonaRosaServiceSyncMessage.forSentTranscript(buildSentTranscript(recipientsSet))
    val sendMessageResult = AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(transcriptMessage)

    Log.i(TAG, "Sent transcript message with ${recipientsSet.size} recipients")

    if (!sendMessageResult.isSuccess) {
      throw RetryableException()
    }

    ZonaRosaDatabase.messages.deleteRemotelyDeletedStory(deletedMessageId)
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return e is RetryableException
  }

  private fun buildSentTranscript(recipientsSet: Set<ZonaRosaServiceStoryMessageRecipient>): SentTranscriptMessage {
    return SentTranscriptMessage(
      Optional.of(ZonaRosaServiceAddress(Recipient.self().requireAci())),
      sentTimestamp,
      Optional.empty(),
      0,
      emptyMap(),
      true,
      Optional.empty(),
      recipientsSet,
      Optional.empty()
    )
  }

  override fun onFailure() = Unit

  class RetryableException : Exception()

  class Factory : Job.Factory<MultiDeviceStorySendSyncJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): MultiDeviceStorySendSyncJob {
      val data = JsonJobData.deserialize(serializedData)
      return MultiDeviceStorySendSyncJob(
        parameters = parameters,
        sentTimestamp = data.getLong(DATA_SENT_TIMESTAMP),
        deletedMessageId = data.getLong(DATA_DELETED_MESSAGE_ID)
      )
    }
  }
}

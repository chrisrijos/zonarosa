package io.zonarosa.messenger.jobs

import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.logging.Log.tag
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.MessageId
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.protos.AdminDeleteJobData
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.messages.GroupSendUtil
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.RecipientUtil
import io.zonarosa.messenger.util.GroupUtil
import io.zonarosa.service.api.crypto.ContentHint
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage.Companion.newBuilder
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration.Companion.days

/**
 * Job used when an admin deletes a message in a group
 */
class AdminDeleteSendJob private constructor(
  private val messageId: Long,
  private val recipientIds: MutableList<Long>,
  private val initialRecipientCount: Int,
  parameters: Parameters
) : Job(parameters) {

  companion object {
    const val KEY: String = "AdminDeleteSendJob"

    private val TAG = tag(AdminDeleteSendJob::class.java)

    @JvmStatic
    fun create(messageId: Long): AdminDeleteSendJob? {
      val message = ZonaRosaDatabase.messages.getMessageRecord(messageId)
      val conversationRecipient = ZonaRosaDatabase.threads.getRecipientForThreadId(message.threadId)

      if (conversationRecipient == null) {
        return null
      }

      val recipientIds = conversationRecipient.participantIds.map { it.toLong() }.toMutableList()

      return AdminDeleteSendJob(
        messageId = messageId,
        recipientIds = recipientIds,
        initialRecipientCount = recipientIds.size,
        parameters = Parameters.Builder()
          .setQueue(conversationRecipient.id.toQueueKey())
          .addConstraint(NetworkConstraint.KEY)
          .setLifespan(1.days.inWholeMilliseconds)
          .setMaxAttempts(Parameters.UNLIMITED)
          .build()
      )
    }
  }

  override fun serialize(): ByteArray? {
    return AdminDeleteJobData(messageId, recipientIds, initialRecipientCount).encode()
  }

  override fun getFactoryKey(): String {
    return KEY
  }

  override fun run(): Result {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.w(TAG, "Not registered. Skipping.")
      return Result.failure()
    }

    val message = ZonaRosaDatabase.messages.getMessageRecord(messageId)
    if (!message.fromRecipient.hasServiceId) {
      Log.w(TAG, "Missing service id for the target author.")
      return Result.failure()
    }

    val recipients = recipientIds.map { Recipient.resolved(RecipientId.from(it)) }.toMutableList()
    val targetSentTimestamp = message.dateSent
    val targetAuthor = message.fromRecipient.requireServiceId()

    val conversationRecipient = ZonaRosaDatabase.threads.getRecipientForThreadId(message.threadId)
    if (conversationRecipient == null) {
      Log.w(TAG, "We have a message, but couldn't find the thread!")
      return Result.failure()
    }

    if (!conversationRecipient.isPushV2Group) {
      Log.w(TAG, "Cannot admin delete in a non V2 group.")
      return Result.failure()
    }

    val groupRecord = ZonaRosaDatabase.groups.getGroup(conversationRecipient.requireGroupId())
    if (groupRecord.isEmpty || !groupRecord.get().isAdmin(Recipient.self())) {
      Log.w(TAG, "Cannot delete because you are not an admin.")
      return Result.failure()
    }

    val eligible = RecipientUtil.getEligibleForSending(recipients.filter { it.hasServiceId })
    val skippedRecipients = recipients - eligible
    val sendResult = deliver(conversationRecipient, eligible, targetAuthor, targetSentTimestamp)

    for (completion in sendResult.completed) {
      recipientIds.remove(completion.id.toLong())
    }

    for (unregistered in sendResult.unregistered) {
      ZonaRosaDatabase.recipients.markUnregistered(unregistered)
    }

    for (recipient in skippedRecipients) {
      recipientIds.remove(recipient.id.toLong())
    }

    Log.i(TAG, "Completed now: ${sendResult.completed.size} Skipped: ${skippedRecipients.size + sendResult.skipped.size} Remaining: ${recipientIds.size}")

    if (recipientIds.isEmpty()) {
      return Result.success()
    } else {
      Log.w(TAG, "Still need to send to ${recipients.size} recipients. Retrying.")
      return Result.retry(defaultBackoff())
    }
  }

  override fun onFailure() {
    Log.w(TAG, "Failed to send admin delete to all recipients! ${initialRecipientCount - recipientIds.size} /  $initialRecipientCount")
  }

  private fun deliver(
    conversationRecipient: Recipient,
    destinations: MutableList<Recipient>,
    targetAuthor: ServiceId,
    targetSentTimestamp: Long
  ): GroupSendJobHelper.SendResult {
    val dataMessageBuilder = newBuilder()
      .withTimestamp(System.currentTimeMillis())
      .withAdminDelete(ZonaRosaServiceDataMessage.AdminDelete(targetAuthor, targetSentTimestamp))

    GroupUtil.setDataMessageGroupContext(context, dataMessageBuilder, conversationRecipient.requireGroupId().requirePush())

    val nonSelfDestinations = destinations.filterNot { it.isSelf }
    val includeSelf = destinations.size != nonSelfDestinations.size

    val dataMessage = dataMessageBuilder.build()

    val results = GroupSendUtil.sendResendableDataMessage(
      context,
      conversationRecipient.groupId.map { it.requireV2() }.getOrNull(),
      null,
      nonSelfDestinations,
      false,
      ContentHint.RESENDABLE,
      MessageId(messageId),
      dataMessage,
      true,
      false,
      null,
      null
    ).toMutableList()

    if (includeSelf) {
      results.add(AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(dataMessage))
    }

    return GroupSendJobHelper.getCompletedSends(destinations, results)
  }

  class Factory : Job.Factory<AdminDeleteSendJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AdminDeleteSendJob {
      val data = AdminDeleteJobData.ADAPTER.decode(serializedData!!)

      return AdminDeleteSendJob(
        messageId = data.messageId,
        recipientIds = data.recipientIds.toMutableList(),
        initialRecipientCount = data.initialRecipientCount,
        parameters = parameters
      )
    }
  }
}

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.MessageId
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.protos.PollVoteJobData
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.messages.GroupSendUtil
import io.zonarosa.messenger.polls.PollRecord
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.RecipientUtil
import io.zonarosa.messenger.util.GroupUtil
import io.zonarosa.service.api.crypto.ContentHint
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage
import io.zonarosa.service.api.messages.ZonaRosaServiceDataMessage.Companion.newBuilder
import kotlin.time.Duration.Companion.days

/**
 * Sends a poll vote for a given poll in a group. If the vote completely fails to send, we do our best to undo that vote.
 */
class PollVoteJob(
  private val messageId: Long,
  private val recipientIds: MutableList<Long>,
  private val initialRecipientCount: Int,
  private val voteCount: Int,
  private val isRemoval: Boolean,
  private val optionId: Long,
  parameters: Parameters
) : Job(parameters) {

  companion object {
    const val KEY: String = "PollVoteJob"
    private val TAG = Log.tag(PollVoteJob::class.java)

    fun create(messageId: Long, voteCount: Int, isRemoval: Boolean, optionId: Long): PollVoteJob? {
      val message = ZonaRosaDatabase.messages.getMessageRecordOrNull(messageId)
      if (message == null) {
        Log.w(TAG, "Unable to find corresponding message")
        return null
      }

      val conversationRecipient = ZonaRosaDatabase.threads.getRecipientForThreadId(message.threadId)
      if (conversationRecipient == null) {
        Log.w(TAG, "We have a message, but couldn't find the thread!")
        return null
      }

      val recipients = if (conversationRecipient.isGroup) {
        conversationRecipient.participantIds.filter { it != Recipient.self().id }.map { it.toLong() }
      } else {
        listOf(conversationRecipient.id.toLong())
      }

      return PollVoteJob(
        messageId = messageId,
        recipientIds = recipients.toMutableList(),
        initialRecipientCount = recipients.size,
        voteCount = voteCount,
        isRemoval = isRemoval,
        optionId = optionId,
        parameters = Parameters.Builder()
          .setQueue(conversationRecipient.id.toQueueKey())
          .addConstraint(NetworkConstraint.KEY)
          .setMaxAttempts(Parameters.UNLIMITED)
          .setLifespan(1.days.inWholeMilliseconds)
          .build()
      )
    }
  }

  override fun serialize(): ByteArray {
    return PollVoteJobData(messageId, recipientIds, initialRecipientCount, voteCount, isRemoval, optionId).encode()
  }

  override fun getFactoryKey(): String {
    return KEY
  }

  override fun run(): Result {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.w(TAG, "Not registered. Skipping.")
      return Result.failure()
    }

    val message = ZonaRosaDatabase.messages.getMessageRecordOrNull(messageId)
    if (message == null) {
      Log.w(TAG, "Unable to find corresponding message")
      return Result.failure()
    }

    val conversationRecipient = ZonaRosaDatabase.threads.getRecipientForThreadId(message.threadId)
    if (conversationRecipient == null) {
      Log.w(TAG, "We have a message, but couldn't find the thread!")
      return Result.failure()
    }

    val poll = ZonaRosaDatabase.polls.getPoll(messageId)
    if (poll == null) {
      Log.w(TAG, "Unable to find corresponding poll")
      return Result.failure()
    }

    val targetAuthor = message.fromRecipient
    if (targetAuthor == null || !targetAuthor.hasServiceId) {
      Log.w(TAG, "Unable to find target author")
      return Result.failure()
    }

    val targetSentTimestamp = message.dateSent

    val recipients = Recipient.resolvedList(recipientIds.map { RecipientId.from(it) })
    val registered = RecipientUtil.getEligibleForSending(recipients)
    val unregistered = recipients - registered.toSet()
    val completions: List<Recipient> = deliver(conversationRecipient, registered, targetAuthor, targetSentTimestamp, poll)

    recipientIds.removeAll(unregistered.map { it.id.toLong() })
    recipientIds.removeAll(completions.map { it.id.toLong() })

    Log.i(TAG, "Completed now: " + completions.size + ", Remaining: " + recipientIds.size)

    if (recipientIds.isNotEmpty()) {
      Log.w(TAG, "Still need to send to " + recipientIds.size + " recipients. Retrying.")
      return Result.retry(defaultBackoff())
    }

    return Result.success()
  }

  private fun deliver(conversationRecipient: Recipient, destinations: List<Recipient>, targetAuthor: Recipient, targetSentTimestamp: Long, poll: PollRecord): List<Recipient> {
    val votes = ZonaRosaDatabase.polls.getVotes(poll.id, poll.allowMultipleVotes, voteCount)

    val dataMessageBuilder = newBuilder()
      .withTimestamp(System.currentTimeMillis())
      .withPollVote(
        buildPollVote(
          targetAuthor = targetAuthor,
          targetSentTimestamp = targetSentTimestamp,
          optionIndexes = votes,
          voteCount = voteCount
        )
      )

    if (conversationRecipient.isPushV2Group) {
      GroupUtil.setDataMessageGroupContext(context, dataMessageBuilder, conversationRecipient.requireGroupId().requirePush())
    }

    val dataMessage = dataMessageBuilder.build()
    val nonSelfDestinations = destinations.filter { !it.isSelf }

    val results = GroupSendUtil.sendResendableDataMessage(
      context,
      conversationRecipient.groupId.map { obj: GroupId -> obj.requireV2() }.orElse(null),
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
    )

    if (conversationRecipient.isSelf) {
      results.add(AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(dataMessage))
    }

    val groupResult = GroupSendJobHelper.getCompletedSends(destinations, results)

    for (unregistered in groupResult.unregistered) {
      ZonaRosaDatabase.recipients.markUnregistered(unregistered)
    }

    if (groupResult.completed.isNotEmpty() || destinations.isEmpty()) {
      if (isRemoval) {
        ZonaRosaDatabase.polls.markPendingAsRemoved(
          pollId = poll.id,
          voterId = Recipient.self().id.toLong(),
          voteCount = voteCount,
          messageId = poll.messageId,
          optionId = optionId
        )
      } else {
        ZonaRosaDatabase.polls.markPendingAsAdded(
          pollId = poll.id,
          voterId = Recipient.self().id.toLong(),
          voteCount = voteCount,
          messageId = poll.messageId,
          optionId = optionId
        )
      }
    }

    return groupResult.completed
  }

  override fun onFailure() {
    if (recipientIds.size < initialRecipientCount) {
      Log.w(TAG, "Only sent vote to " + recipientIds.size + "/" + initialRecipientCount + " recipients. Still, it sent to someone, so it stays.")
      return
    }

    Log.w(TAG, "Failed to send to all recipients!")

    val pollId = ZonaRosaDatabase.polls.getPollId(messageId)
    if (pollId == null) {
      Log.w(TAG, "Poll no longer exists")
      return
    }

    ZonaRosaDatabase.polls.removePendingVote(pollId, optionId, voteCount, messageId)
  }

  private fun buildPollVote(
    targetAuthor: Recipient,
    targetSentTimestamp: Long,
    optionIndexes: List<Int>,
    voteCount: Int
  ): ZonaRosaServiceDataMessage.PollVote {
    return ZonaRosaServiceDataMessage.PollVote(
      targetAuthor = targetAuthor.requireServiceId(),
      targetSentTimestamp = targetSentTimestamp,
      optionIndexes = optionIndexes,
      voteCount = voteCount
    )
  }

  class Factory : Job.Factory<PollVoteJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): PollVoteJob {
      val data = PollVoteJobData.ADAPTER.decode(serializedData!!)

      return PollVoteJob(
        messageId = data.messageId,
        recipientIds = data.recipients.toMutableList(),
        initialRecipientCount = data.initialRecipientCount,
        voteCount = data.voteCount,
        isRemoval = data.isRemoval,
        optionId = data.optionId,
        parameters = parameters
      )
    }
  }
}

package io.zonarosa.messenger.jobs

import android.os.Build
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.JsonJobData
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.transport.RetryLaterException
import io.zonarosa.messenger.util.ConversationUtil
import io.zonarosa.messenger.util.ConversationUtil.Direction
import kotlin.time.Duration.Companion.seconds

/**
 * Updates the ranking of a shortcut by providing hints for when we send/receive messages to different recipients.
 */
class ConversationShortcutRankingUpdateJob private constructor(
  parameters: Parameters,
  private val recipient: Recipient,
  private val direction: Direction
) : BaseJob(parameters) {

  companion object {
    private val TAG = Log.tag(ConversationShortcutRankingUpdateJob::class.java)

    const val KEY = "ConversationShortcutRankingUpdateJob"

    private const val KEY_RECIPIENT = "recipient"
    private const val KEY_REPORTED_ZONAROSA = "reported_zonarosa"

    @JvmStatic
    fun enqueueForOutgoingIfNecessary(recipient: Recipient) {
      if (Build.VERSION.SDK_INT >= 34) {
        AppDependencies.jobManager.add(ConversationShortcutRankingUpdateJob(recipient, Direction.OUTGOING))
      }
    }

    @JvmStatic
    fun enqueueForIncomingIfNecessary(recipient: Recipient) {
      if (Build.VERSION.SDK_INT >= 34) {
        AppDependencies.jobManager.add(ConversationShortcutRankingUpdateJob(recipient, Direction.INCOMING))
      }
    }
  }

  private constructor(recipient: Recipient, direction: Direction) : this(
    Parameters.Builder()
      .setQueue("ConversationShortcutRankingUpdateJob::${recipient.id.serialize()}")
      .setMaxInstancesForQueue(1)
      .setMaxAttempts(3)
      .build(),
    recipient,
    direction
  )

  override fun serialize(): ByteArray? {
    return JsonJobData.Builder()
      .putString(KEY_RECIPIENT, recipient.id.serialize())
      .putInt(KEY_REPORTED_ZONAROSA, direction.serialize())
      .serialize()
  }

  override fun getFactoryKey() = KEY

  override fun onRun() {
    if (ZonaRosaStore.settings.screenLockEnabled) {
      Log.i(TAG, "Screen lock enabled. Clearing shortcuts.")
      ConversationUtil.clearAllShortcuts(context)
      return
    }

    val success: Boolean = ConversationUtil.pushShortcutForRecipientSync(context, recipient, direction)

    if (!success) {
      Log.w(TAG, "Failed to update shortcut for ${recipient.id}. Possibly retrying.")
      throw RetryLaterException()
    }
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return e is RetryLaterException
  }

  override fun getNextRunAttemptBackoff(pastAttemptCount: Int, exception: Exception): Long {
    return 30.seconds.inWholeMilliseconds
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<ConversationShortcutRankingUpdateJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): ConversationShortcutRankingUpdateJob {
      val data = JsonJobData.deserialize(serializedData)
      val recipient: Recipient = Recipient.resolved(RecipientId.from(data.getString(KEY_RECIPIENT)))
      val direction: Direction = Direction.deserialize(data.getInt(KEY_REPORTED_ZONAROSA))

      return ConversationShortcutRankingUpdateJob(parameters, recipient, direction)
    }
  }
}

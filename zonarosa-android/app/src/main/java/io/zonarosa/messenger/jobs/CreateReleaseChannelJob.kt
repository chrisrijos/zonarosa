package io.zonarosa.messenger.jobs

import androidx.core.content.ContextCompat
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.avatar.Avatar
import io.zonarosa.messenger.avatar.AvatarRenderer
import io.zonarosa.messenger.avatar.Avatars
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.profiles.AvatarHelper
import io.zonarosa.messenger.profiles.ProfileName
import io.zonarosa.messenger.providers.BlobProvider
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.transport.RetryLaterException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Creates the Release Channel (ZonaRosa) recipient.
 */
class CreateReleaseChannelJob private constructor(parameters: Parameters) : BaseJob(parameters) {
  companion object {
    const val KEY = "CreateReleaseChannelJob"

    private val TAG = Log.tag(CreateReleaseChannelJob::class.java)

    fun create(): CreateReleaseChannelJob {
      return CreateReleaseChannelJob(
        Parameters.Builder()
          .setQueue("CreateReleaseChannelJob")
          .setMaxInstancesForFactory(1)
          .setMaxAttempts(3)
          .build()
      )
    }
  }

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.i(TAG, "Not registered, skipping.")
      return
    }

    if (ZonaRosaStore.releaseChannel.releaseChannelRecipientId != null) {
      Log.i(TAG, "Already created Release Channel recipient ${ZonaRosaStore.releaseChannel.releaseChannelRecipientId}")

      val recipient = Recipient.resolved(ZonaRosaStore.releaseChannel.releaseChannelRecipientId!!)
      if (recipient.profileAvatar.isNullOrEmpty() || !ZonaRosaStore.releaseChannel.hasUpdatedAvatar) {
        ZonaRosaStore.releaseChannel.hasUpdatedAvatar = true
        setAvatar(recipient.id)
      }
    } else {
      val recipients = ZonaRosaDatabase.recipients

      val releaseChannelId: RecipientId = recipients.insertReleaseChannelRecipient()
      ZonaRosaStore.releaseChannel.setReleaseChannelRecipientId(releaseChannelId)
      ZonaRosaStore.releaseChannel.hasUpdatedAvatar = true

      recipients.setProfileName(releaseChannelId, ProfileName.asGiven("ZonaRosa"))
      recipients.setMuted(releaseChannelId, Long.MAX_VALUE)
      setAvatar(releaseChannelId)
    }
  }

  private fun setAvatar(id: RecipientId) {
    val latch = CountDownLatch(1)
    AvatarRenderer.renderAvatar(
      context,
      Avatar.Resource(
        R.drawable.ic_zonarosa_logo_large,
        Avatars.ColorPair(ContextCompat.getColor(context, R.color.notification_background_ultramarine), ContextCompat.getColor(context, R.color.core_white), "")
      ),
      onAvatarRendered = { media ->
        AvatarHelper.setAvatar(context, id, BlobProvider.getInstance().getStream(context, media.uri))
        ZonaRosaDatabase.recipients.setProfileAvatar(id, "local")
        latch.countDown()
      },
      onRenderFailed = { t ->
        Log.w(TAG, t)
        latch.countDown()
      }
    )

    try {
      val completed: Boolean = latch.await(30, TimeUnit.SECONDS)
      if (!completed) {
        throw RetryLaterException()
      }
    } catch (e: InterruptedException) {
      throw RetryLaterException()
    }
  }

  override fun onShouldRetry(e: Exception): Boolean = e is RetryLaterException

  class Factory : Job.Factory<CreateReleaseChannelJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): CreateReleaseChannelJob {
      return CreateReleaseChannelJob(parameters)
    }
  }
}

package io.zonarosa.messenger.components.settings.app.privacy.advanced

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.firebase.installations.FirebaseInstallations
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.MultiDeviceConfigurationUpdateJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.NetworkResultUtil
import io.zonarosa.service.api.push.exceptions.AuthorizationFailedException
import java.io.IOException
import java.util.concurrent.ExecutionException

private val TAG = Log.tag(AdvancedPrivacySettingsRepository::class.java)

class AdvancedPrivacySettingsRepository(private val context: Context) {

  fun disablePushMessages(consumer: (DisablePushMessagesResult) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val result = try {
        try {
          NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.account.clearFcmToken())
        } catch (e: AuthorizationFailedException) {
          Log.w(TAG, e)
        }
        if (ZonaRosaStore.account.fcmEnabled) {
          Tasks.await(FirebaseInstallations.getInstance().delete())
        }
        DisablePushMessagesResult.SUCCESS
      } catch (ioe: IOException) {
        Log.w(TAG, ioe)
        DisablePushMessagesResult.NETWORK_ERROR
      } catch (e: InterruptedException) {
        Log.w(TAG, "Interrupted while deleting", e)
        DisablePushMessagesResult.NETWORK_ERROR
      } catch (e: ExecutionException) {
        Log.w(TAG, "Error deleting", e.cause)
        DisablePushMessagesResult.NETWORK_ERROR
      }

      consumer(result)
    }
  }

  fun syncShowSealedSenderIconState() {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
      AppDependencies.jobManager.add(
        MultiDeviceConfigurationUpdateJob(
          ZonaRosaPreferences.isReadReceiptsEnabled(context),
          ZonaRosaPreferences.isTypingIndicatorsEnabled(context),
          ZonaRosaPreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(context),
          ZonaRosaStore.settings.isLinkPreviewsEnabled
        )
      )
    }
  }

  enum class DisablePushMessagesResult {
    SUCCESS,
    NETWORK_ERROR
  }
}

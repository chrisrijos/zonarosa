package io.zonarosa.messenger.components.settings.app.privacy.expire

import android.content.Context
import androidx.annotation.WorkerThread
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.ThreadTable
import io.zonarosa.messenger.groups.GroupChangeException
import io.zonarosa.messenger.groups.GroupManager
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.mms.OutgoingMessage
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.sms.MessageSender
import io.zonarosa.messenger.storage.StorageSyncHelper
import java.io.IOException

private val TAG: String = Log.tag(ExpireTimerSettingsRepository::class.java)

/**
 * Provide operations to set expire timer for individuals and groups.
 */
class ExpireTimerSettingsRepository(val context: Context) {

  fun setExpiration(recipientId: RecipientId, newExpirationTime: Int, consumer: (Result<Int>) -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      val recipient = Recipient.resolved(recipientId)
      if (recipient.groupId.isPresent && recipient.groupId.get().isPush) {
        try {
          GroupManager.updateGroupTimer(context, recipient.groupId.get().requirePush(), newExpirationTime)
          consumer.invoke(Result.success(newExpirationTime))
        } catch (e: GroupChangeException) {
          Log.w(TAG, e)
          consumer.invoke(Result.failure(e))
        } catch (e: IOException) {
          Log.w(TAG, e)
          consumer.invoke(Result.failure(e))
        }
      } else {
        val expireTimerVersion = ZonaRosaDatabase.recipients.setExpireMessagesAndIncrementVersion(recipientId, newExpirationTime)
        val outgoingMessage = OutgoingMessage.expirationUpdateMessage(Recipient.resolved(recipientId), System.currentTimeMillis(), newExpirationTime * 1000L, expireTimerVersion)
        MessageSender.send(context, outgoingMessage, getThreadId(recipientId), MessageSender.SendType.ZONAROSA, null, null)
        consumer.invoke(Result.success(newExpirationTime))
      }
    }
  }

  fun setUniversalExpireTimerSeconds(newExpirationTime: Int, onDone: () -> Unit) {
    ZonaRosaExecutors.BOUNDED.execute {
      ZonaRosaStore.settings.universalExpireTimer = newExpirationTime
      ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
      StorageSyncHelper.scheduleSyncForDataChange()
      onDone.invoke()
    }
  }

  @WorkerThread
  private fun getThreadId(recipientId: RecipientId): Long {
    val threadTable: ThreadTable = ZonaRosaDatabase.threads
    val recipient: Recipient = Recipient.resolved(recipientId)
    return threadTable.getOrCreateThreadIdFor(recipient)
  }
}

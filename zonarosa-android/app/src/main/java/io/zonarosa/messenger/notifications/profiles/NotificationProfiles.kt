package io.zonarosa.messenger.notifications.profiles

import android.content.Context
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.keyvalue.NotificationProfileValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.messenger.util.formatHours
import io.zonarosa.messenger.util.toLocalDateTime
import io.zonarosa.messenger.util.toLocalTime
import io.zonarosa.messenger.util.toMillis
import io.zonarosa.messenger.util.toOffset
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Helper for determining the single, currently active Notification Profile (if any) and also how to describe
 * how long the active profile will be on for.
 */
object NotificationProfiles {

  val TAG = Log.tag(NotificationProfiles::class.java)

  @JvmStatic
  @JvmOverloads
  fun getActiveProfile(profiles: List<NotificationProfile>, now: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault(), shouldSync: Boolean = false): NotificationProfile? {
    val storeValues: NotificationProfileValues = ZonaRosaStore.notificationProfile
    val localNow: LocalDateTime = now.toLocalDateTime(zoneId)

    val manualProfile: NotificationProfile? = if (now < storeValues.manuallyEnabledUntil) {
      profiles.firstOrNull { it.id == storeValues.manuallyEnabledProfile }
    } else {
      null
    }

    val scheduledProfile: NotificationProfile? = profiles.sortedDescending().filter { it.schedule.isCurrentlyActive(now, zoneId) }.firstOrNull { profile ->
      profile.schedule.startDateTime(localNow).toMillis(zoneId.toOffset()) > storeValues.manuallyDisabledAt
    }

    if (shouldSync && shouldClearManualOverride(manualProfile, scheduledProfile)) {
      ZonaRosaExecutors.UNBOUNDED.execute {
        ZonaRosaDatabase.recipients.markNeedsSync(Recipient.self().id)
        StorageSyncHelper.scheduleSyncForDataChange()
      }
    }

    if (manualProfile == null || scheduledProfile == null) {
      return manualProfile ?: scheduledProfile
    }

    return manualProfile
  }

  private fun shouldClearManualOverride(manualProfile: NotificationProfile?, scheduledProfile: NotificationProfile?): Boolean {
    val storeValues: NotificationProfileValues = ZonaRosaStore.notificationProfile
    var shouldScheduleSync = false

    if (manualProfile == null && storeValues.manuallyEnabledProfile != 0L) {
      Log.i(TAG, "Clearing override: ${storeValues.manuallyEnabledProfile} and ${storeValues.manuallyEnabledUntil}")
      storeValues.manuallyEnabledProfile = 0
      storeValues.manuallyEnabledUntil = 0
      shouldScheduleSync = true
    }

    if (scheduledProfile != null && storeValues.manuallyDisabledAt != 0L) {
      Log.i(TAG, "Clearing override: ${storeValues.manuallyDisabledAt}")
      storeValues.manuallyDisabledAt = 0
      shouldScheduleSync = true
    }

    return shouldScheduleSync
  }

  fun getActiveProfileDescription(context: Context, profile: NotificationProfile, now: Long = System.currentTimeMillis()): String {
    val storeValues: NotificationProfileValues = ZonaRosaStore.notificationProfile

    if (profile.id == storeValues.manuallyEnabledProfile) {
      if (storeValues.manuallyEnabledUntil.isForever()) {
        return context.getString(R.string.NotificationProfilesFragment__on)
      } else if (now < storeValues.manuallyEnabledUntil) {
        return context.getString(R.string.NotificationProfileSelection__on_until_s, storeValues.manuallyEnabledUntil.toLocalTime().formatHours(context))
      }
    }

    return context.getString(R.string.NotificationProfileSelection__on_until_s, profile.schedule.endTime().formatHours(context))
  }

  private fun Long.isForever(): Boolean {
    return this == Long.MAX_VALUE
  }
}

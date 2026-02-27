package io.zonarosa.messenger.util

import android.content.Context
import android.content.pm.PackageManager
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.RefreshAttributesJob
import io.zonarosa.messenger.jobs.RemoteConfigRefreshJob
import io.zonarosa.messenger.jobs.RetrieveRemoteAnnouncementsJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import java.time.Duration

object VersionTracker {
  private val TAG = Log.tag(VersionTracker::class.java)

  @JvmStatic
  fun getLastSeenVersion(context: Context): Int {
    return ZonaRosaPreferences.getLastVersionCode(context)
  }

  @JvmStatic
  fun updateLastSeenVersion(context: Context) {
    val currentVersionCode = BuildConfig.VERSION_CODE
    val lastVersionCode = ZonaRosaPreferences.getLastVersionCode(context)

    if (currentVersionCode != lastVersionCode) {
      Log.i(TAG, "Upgraded from $lastVersionCode to $currentVersionCode. Clearing client deprecation.", true)
      ZonaRosaStore.misc.isClientDeprecated = false
      ZonaRosaStore.remoteConfig.eTag = ""
      val jobChain = listOf(RemoteConfigRefreshJob(), RefreshAttributesJob())
      AppDependencies.jobManager.startChain(jobChain).enqueue()
      RetrieveRemoteAnnouncementsJob.enqueue(true)
      LocalMetrics.getInstance().clear()
    }

    ZonaRosaPreferences.setLastVersionCode(context, currentVersionCode)
  }

  @JvmStatic
  fun getDaysSinceFirstInstalled(context: Context): Long {
    return try {
      val installTimestamp = context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime
      Duration.ofMillis(System.currentTimeMillis() - installTimestamp).toDays()
    } catch (e: PackageManager.NameNotFoundException) {
      Log.w(TAG, e)
      0
    }
  }
}

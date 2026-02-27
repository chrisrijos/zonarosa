/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.R
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.notifications.NotificationChannels
import io.zonarosa.messenger.notifications.NotificationIds
import io.zonarosa.messenger.util.ServiceUtil
import kotlin.time.Duration.Companion.days

/**
 * Notifies users that their build expired and redirects to the download page on click.
 */
class DeprecatedNotificationJob private constructor(parameters: Parameters) : Job(parameters) {
  companion object {
    const val KEY: String = "DeprecatedNotificationJob"
    private val TAG = Log.tag(DeprecatedNotificationJob::class.java)

    @JvmStatic
    fun enqueue() {
      AppDependencies.jobManager.add(DeprecatedNotificationJob())
    }
  }

  private constructor() : this(
    Parameters.Builder()
      .setQueue("DeprecatedNotificationJob")
      .setLifespan(7.days.inWholeMilliseconds)
      .setMaxAttempts(Parameters.UNLIMITED)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    if (NotificationChannels.getInstance().areNotificationsEnabled()) {
      val intent: Intent

      if (BuildConfig.MANAGES_APP_UPDATES) {
        Log.d(TAG, "Showing deprecated notification for website APK")
        intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://zonarosa.io/android/apk"))
      } else {
        Log.d(TAG, "Showing deprecated notification for PlayStore")
        val packageName = context.packageName
        intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
      }

      val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
      val builder = NotificationCompat.Builder(context, NotificationChannels.getInstance().APP_ALERTS)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle(context.getString(R.string.DeprecatedNotificationJob_update_zonarosa))
        .setContentText(context.getString(R.string.DeprecatedNotificationJob_this_version_of_zonarosa_has_expired))
        .setContentIntent(pendingIntent)

      ServiceUtil.getNotificationManager(context).notify(NotificationIds.APK_UPDATE_PROMPT_INSTALL, builder.build())
    }

    return Result.success()
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<DeprecatedNotificationJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): DeprecatedNotificationJob {
      return DeprecatedNotificationJob(parameters)
    }
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service

import android.content.Context
import androidx.annotation.VisibleForTesting
import io.zonarosa.messenger.jobs.BackupMessagesJob
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.toMillis
import io.zonarosa.messenger.util.toOffset
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

class MessageBackupListener : PersistentAlarmManagerListener() {
  override fun shouldScheduleExact(): Boolean {
    return true
  }

  @VisibleForTesting
  public override fun getNextScheduledExecutionTime(context: Context): Long {
    val nextTime = ZonaRosaStore.backup.nextBackupTime
    return if (nextTime < 0 || nextTime > (System.currentTimeMillis() + 2.days.inWholeMilliseconds)) {
      setNextBackupTimeToIntervalFromNow()
    } else {
      nextTime
    }
  }

  override fun onAlarm(context: Context, scheduledTime: Long): Long {
    if (ZonaRosaStore.backup.areBackupsEnabled) {
      BackupMessagesJob.enqueue()
    }
    return setNextBackupTimeToIntervalFromNow()
  }

  companion object {
    private val BACKUP_JITTER_WINDOW_SECONDS = 10.minutes.inWholeSeconds.toInt()

    @JvmStatic
    fun schedule(context: Context?) {
      if (ZonaRosaStore.backup.areBackupsEnabled) {
        MessageBackupListener().onReceive(context, getScheduleIntent())
      }
    }

    @VisibleForTesting
    @JvmStatic
    fun getNextDailyBackupTimeFromNowWithJitter(now: LocalDateTime, hour: Int, minute: Int, maxJitterSeconds: Int, randomSource: Random = Random()): LocalDateTime {
      var next = now.withHour(hour).withMinute(minute).withSecond(0)

      val endOfJitterWindowForNow = now.plusSeconds(maxJitterSeconds.toLong() / 2)
      while (!endOfJitterWindowForNow.isBefore(next)) {
        next = next.plusDays(1)
      }

      val jitter = randomSource.nextInt(maxJitterSeconds) - maxJitterSeconds / 2
      return next.plusSeconds(jitter.toLong())
    }

    @VisibleForTesting
    fun setNextBackupTimeToIntervalFromNow(zoneId: ZoneId = ZoneId.systemDefault(), now: LocalDateTime = LocalDateTime.now(zoneId), maxJitterSeconds: Int = BACKUP_JITTER_WINDOW_SECONDS, randomSource: Random = Random()): Long {
      val hour = ZonaRosaStore.settings.zonarosaBackupHour
      val minute = ZonaRosaStore.settings.zonarosaBackupMinute
      val next = getNextDailyBackupTimeFromNowWithJitter(now, hour, minute, maxJitterSeconds, randomSource)
      val nextTime = next.toMillis(zoneId.toOffset())
      ZonaRosaStore.backup.nextBackupTime = nextTime
      return nextTime
    }
  }
}

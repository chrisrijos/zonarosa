package io.zonarosa.messenger.service;


import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.jobs.LocalBackupJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.util.JavaTimeExtensionsKt;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class LocalBackupListener extends PersistentAlarmManagerListener {

  private static final int BACKUP_JITTER_WINDOW_SECONDS = Math.toIntExact(TimeUnit.MINUTES.toSeconds(10));

  @Override
  protected boolean shouldScheduleExact() {
    return true;
  }

  @Override
  protected long getNextScheduledExecutionTime(Context context) {
    return ZonaRosaPreferences.getNextBackupTime(context);
  }

  @Override
  protected long onAlarm(Context context, long scheduledTime) {
    if (ZonaRosaStore.settings().isBackupEnabled()) {
      LocalBackupJob.enqueue(false);
    }

    if (ZonaRosaStore.backup().getNewLocalBackupsEnabled()) {
      LocalBackupJob.enqueueArchive(ZonaRosaStore.settings().isBackupEnabled());
    }

    return setNextBackupTimeToIntervalFromNow(context);
  }

  public static void schedule(Context context) {
    if (ZonaRosaStore.settings().isBackupEnabled() || ZonaRosaStore.backup().getNewLocalBackupsEnabled()) {
      new LocalBackupListener().onReceive(context, getScheduleIntent());
    }
  }

  public static long setNextBackupTimeToIntervalFromNow(@NonNull Context context) {
    LocalDateTime now    = LocalDateTime.now();
    int           hour   = ZonaRosaStore.settings().getBackupHour();
    int           minute = ZonaRosaStore.settings().getBackupMinute();
    LocalDateTime next   = MessageBackupListener.getNextDailyBackupTimeFromNowWithJitter(now, hour, minute, BACKUP_JITTER_WINDOW_SECONDS, new Random());

    long nextTime = JavaTimeExtensionsKt.toMillis(next);

    ZonaRosaPreferences.setNextBackupTime(context, nextTime);

    return nextTime;
  }
}

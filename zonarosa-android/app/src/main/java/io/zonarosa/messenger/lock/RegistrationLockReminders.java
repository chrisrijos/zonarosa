package io.zonarosa.messenger.lock;


import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.util.ZonaRosaPreferences;

import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class RegistrationLockReminders {

  private static final NavigableSet<Long> INTERVALS = new TreeSet<Long>() {{
    add(TimeUnit.HOURS.toMillis(6));
    add(TimeUnit.HOURS.toMillis(12));
    add(TimeUnit.DAYS.toMillis(1));
    add(TimeUnit.DAYS.toMillis(3));
    add(TimeUnit.DAYS.toMillis(7));
  }};

  public static final long INITIAL_INTERVAL = INTERVALS.first();

  public static boolean needsReminder(@NonNull Context context) {
    long lastReminderTime = ZonaRosaPreferences.getRegistrationLockLastReminderTime(context);
    long nextIntervalTime = ZonaRosaPreferences.getRegistrationLockNextReminderInterval(context);

    return System.currentTimeMillis() > lastReminderTime + nextIntervalTime;
  }

  public static void scheduleReminder(@NonNull Context context, boolean success) {
    if (success) {
      long timeSinceLastReminder = System.currentTimeMillis() - ZonaRosaPreferences.getRegistrationLockLastReminderTime(context);
      Long nextReminderInterval = INTERVALS.higher(timeSinceLastReminder);

      if (nextReminderInterval == null) {
        nextReminderInterval = INTERVALS.last();
      }

      ZonaRosaPreferences.setRegistrationLockLastReminderTime(context, System.currentTimeMillis());
      ZonaRosaPreferences.setRegistrationLockNextReminderInterval(context, nextReminderInterval);
    } else {
      long timeSinceLastReminder = ZonaRosaPreferences.getRegistrationLockLastReminderTime(context) + TimeUnit.MINUTES.toMillis(5);
      ZonaRosaPreferences.setRegistrationLockLastReminderTime(context, timeSinceLastReminder);
    }
  }
}

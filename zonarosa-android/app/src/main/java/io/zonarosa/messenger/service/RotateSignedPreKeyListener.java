package io.zonarosa.messenger.service;


import android.content.Context;

import io.zonarosa.messenger.jobs.PreKeysSyncJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

public class RotateSignedPreKeyListener extends PersistentAlarmManagerListener {

  @Override
  protected long getNextScheduledExecutionTime(Context context) {
    return ZonaRosaPreferences.getSignedPreKeyRotationTime(context);
  }

  @Override
  protected long onAlarm(Context context, long scheduledTime) {
    if (scheduledTime != 0 && ZonaRosaStore.account().isRegistered()) {
      PreKeysSyncJob.enqueue();
    }

    long nextTime = System.currentTimeMillis() + PreKeysSyncJob.REFRESH_INTERVAL;
    ZonaRosaPreferences.setSignedPreKeyRotationTime(context, nextTime);

    return nextTime;
  }

  public static void schedule(Context context) {
    new RotateSignedPreKeyListener().onReceive(context, getScheduleIntent());
  }
}

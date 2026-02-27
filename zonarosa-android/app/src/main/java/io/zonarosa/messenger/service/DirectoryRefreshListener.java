package io.zonarosa.messenger.service;


import android.content.Context;

import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.DirectoryRefreshJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

import java.util.concurrent.TimeUnit;

public class DirectoryRefreshListener extends PersistentAlarmManagerListener {

  @Override
  protected long getNextScheduledExecutionTime(Context context) {
    return ZonaRosaPreferences.getDirectoryRefreshTime(context);
  }

  @Override
  protected long onAlarm(Context context, long scheduledTime) {
    if (scheduledTime != 0 && ZonaRosaStore.account().isRegistered()) {
      AppDependencies.getJobManager().add(new DirectoryRefreshJob(true));
    }

    long newTime;

    if (ZonaRosaStore.misc().isCdsBlocked()) {
      newTime = Math.min(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(6),
                         ZonaRosaStore.misc().getCdsBlockedUtil());
    } else {
      newTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(RemoteConfig.cdsRefreshIntervalSeconds());
      ZonaRosaPreferences.setDirectoryRefreshTime(context, newTime);
    }

    ZonaRosaPreferences.setDirectoryRefreshTime(context, newTime);

    return newTime;
  }

  public static void schedule(Context context) {
    new DirectoryRefreshListener().onReceive(context, getScheduleIntent());
  }
}

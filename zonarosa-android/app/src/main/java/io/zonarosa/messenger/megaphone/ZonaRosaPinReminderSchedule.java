package io.zonarosa.messenger.megaphone;

import io.zonarosa.messenger.keyvalue.ZonaRosaStore;

final class ZonaRosaPinReminderSchedule implements MegaphoneSchedule {

  @Override
  public boolean shouldDisplay(int seenCount, long lastSeen, long firstVisible, long currentTime) {
    if (ZonaRosaStore.svr().hasOptedOut()) {
      return false;
    }

    if (!ZonaRosaStore.svr().hasPin()) {
      return false;
    }

    if (ZonaRosaStore.account().isLinkedDevice()) {
      return false;
    }

    if (!ZonaRosaStore.pin().arePinRemindersEnabled()) {
      return false;
    }

    if (!ZonaRosaStore.account().isRegistered()) {
      return false;
    }

    if (ZonaRosaStore.account().isLinkedDevice()) {
      return false;
    }

    long lastReminderTime = ZonaRosaStore.pin().getLastReminderTime();
    long interval         = ZonaRosaStore.pin().getCurrentInterval();

    return currentTime - lastReminderTime >= interval;
  }
}

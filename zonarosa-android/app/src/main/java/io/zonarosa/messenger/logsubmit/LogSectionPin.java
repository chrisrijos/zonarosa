package io.zonarosa.messenger.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.keyvalue.ZonaRosaStore;

public class LogSectionPin implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "PIN STATE";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    return new StringBuilder().append("Last Successful Reminder Entry: ").append(ZonaRosaStore.pin().getLastSuccessfulEntryTime()).append("\n")
                              .append("Last Reminder Time: ").append(ZonaRosaStore.pin().getLastReminderTime()).append("\n")
                              .append("Next Reminder Interval: ").append(ZonaRosaStore.pin().getCurrentInterval()).append("\n")
                              .append("Reglock: ").append(ZonaRosaStore.svr().isRegistrationLockEnabled()).append("\n")
                              .append("ZonaRosa PIN: ").append(ZonaRosaStore.svr().hasPin()).append("\n")
                              .append("Restored via AEP: ").append(ZonaRosaStore.account().restoredAccountEntropyPool()).append("\n")
                              .append("Opted Out: ").append(ZonaRosaStore.svr().hasOptedOut()).append("\n")
                              .append("Last Creation Failed: ").append(ZonaRosaStore.svr().lastPinCreateFailed()).append("\n")
                              .append("Needs Account Restore: ").append(ZonaRosaStore.storageService().getNeedsAccountRestore()).append("\n")
                              .append("PIN Required at Registration: ").append(ZonaRosaStore.registration().pinWasRequiredAtRegistration()).append("\n")
                              .append("Registration Complete: ").append(ZonaRosaStore.registration().isRegistrationComplete());

  }
}

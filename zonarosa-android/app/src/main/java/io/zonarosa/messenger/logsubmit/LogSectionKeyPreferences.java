package io.zonarosa.messenger.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.keyvalue.KeepMessagesDuration;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.core.util.Util;

final class LogSectionKeyPreferences implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "KEY PREFERENCES";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    return new StringBuilder().append("Screen Lock              : ").append(ZonaRosaStore.settings().getScreenLockEnabled()).append("\n")
                              .append("Screen Lock Timeout      : ").append(ZonaRosaStore.settings().getScreenLockTimeout()).append("\n")
                              .append("Password Disabled        : ").append(ZonaRosaStore.settings().getPassphraseDisabled()).append("\n")
                              .append("Prefer Contact Photos    : ").append(ZonaRosaStore.settings().isPreferSystemContactPhotos()).append("\n")
                              .append("Call Data Mode           : ").append(ZonaRosaStore.settings().getCallDataMode()).append("\n")
                              .append("Media Quality            : ").append(ZonaRosaStore.settings().getSentMediaQuality()).append("\n")
                              .append("Client Deprecated        : ").append(ZonaRosaStore.misc().isClientDeprecated()).append("\n")
                              .append("Push Registered          : ").append(ZonaRosaStore.account().isRegistered()).append("\n")
                              .append("Unauthorized Received    : ").append(ZonaRosaPreferences.isUnauthorizedReceived(context)).append("\n")
                              .append("self.isRegistered()      : ").append(ZonaRosaStore.account().getAci() == null ? "false"     : Recipient.self().isRegistered()).append("\n")
                              .append("Thread Trimming          : ").append(getThreadTrimmingString()).append("\n")
                              .append("Censorship Setting       : ").append(ZonaRosaStore.settings().getCensorshipCircumventionEnabled()).append("\n")
                              .append("Network Reachable        : ").append(ZonaRosaStore.misc().isServiceReachableWithoutCircumvention()).append(", last checked: ").append(ZonaRosaStore.misc().getLastCensorshipServiceReachabilityCheckTime()).append("\n")
                              .append("Wifi Download            : ").append(Util.join(ZonaRosaPreferences.getWifiMediaDownloadAllowed(context), ",")).append("\n")
                              .append("Roaming Download         : ").append(Util.join(ZonaRosaPreferences.getRoamingMediaDownloadAllowed(context), ",")).append("\n")
                              .append("Mobile Download          : ").append(Util.join(ZonaRosaPreferences.getMobileMediaDownloadAllowed(context), ",")).append("\n")
                              .append("Phone Number Sharing     : ").append(ZonaRosaStore.phoneNumberPrivacy().isPhoneNumberSharingEnabled()).append(" (").append(ZonaRosaStore.phoneNumberPrivacy().getPhoneNumberSharingMode()).append(")\n")
                              .append("Phone Number Discoverable: ").append(ZonaRosaStore.phoneNumberPrivacy().getPhoneNumberDiscoverabilityMode()).append("\n")
                              .append("Incognito keyboard       : ").append(ZonaRosaPreferences.isIncognitoKeyboardEnabled(context)).append("\n");
  }

  private static String getThreadTrimmingString() {
    if (ZonaRosaStore.settings().isTrimByLengthEnabled()) {
      return "Enabled - Max length of " + ZonaRosaStore.settings().getThreadTrimLength();
    } else if (ZonaRosaStore.settings().getKeepMessagesDuration() != KeepMessagesDuration.FOREVER) {
      return "Enabled - Max age of " + ZonaRosaStore.settings().getKeepMessagesDuration();
    } else {
      return "Disabled";
    }
  }
}

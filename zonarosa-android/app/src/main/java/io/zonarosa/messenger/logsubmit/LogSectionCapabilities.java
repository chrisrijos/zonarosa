package io.zonarosa.messenger.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.messenger.AppCapabilities;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.RecipientRecord;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.service.api.account.AccountAttributes;

public final class LogSectionCapabilities implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "CAPABILITIES";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    if (!ZonaRosaStore.account().isRegistered()) {
      return "Unregistered";
    }

    if (ZonaRosaStore.account().getE164() == null || ZonaRosaStore.account().getAci() == null) {
      return "Self not yet available!";
    }

    Recipient self = Recipient.self();

    AccountAttributes.Capabilities localCapabilities  = AppCapabilities.getCapabilities(false);
    RecipientRecord.Capabilities   globalCapabilities = ZonaRosaDatabase.recipients().getCapabilities(self.getId());

    StringBuilder builder = new StringBuilder().append("-- Local").append("\n")
                                               .append("VersionedExpirationTimer: ").append(localCapabilities.getVersionedExpirationTimer()).append("\n")
                                               .append("\n")
                                               .append("-- Global").append("\n")
                                               .append("None").append("\n");

    // Left as an example for when we want to add new ones
//    if (globalCapabilities != null) {
//      builder.append("StorageServiceEncryptionV2: ").append(globalCapabilities.getStorageServiceEncryptionV2()).append("\n");
//      builder.append("\n");
//    } else {
//      builder.append("Self not found!");
//    }

    return builder;
  }
}

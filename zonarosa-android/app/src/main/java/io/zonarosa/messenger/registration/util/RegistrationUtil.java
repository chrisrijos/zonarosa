/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.util;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.backup.v2.BackupRepository;
import io.zonarosa.messenger.backup.v2.MessageBackupTier;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.ArchiveBackupIdReservationJob;
import io.zonarosa.messenger.jobs.DirectoryRefreshJob;
import io.zonarosa.messenger.jobs.EmojiSearchIndexDownloadJob;
import io.zonarosa.messenger.jobs.PostRegistrationBackupRedemptionJob;
import io.zonarosa.messenger.jobs.RefreshAttributesJob;
import io.zonarosa.messenger.jobs.StorageSyncJob;
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues.PhoneNumberDiscoverabilityMode;
import io.zonarosa.messenger.keyvalue.RestoreDecisionStateUtil;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.util.RemoteConfig;

public final class RegistrationUtil {

  private static final String TAG = Log.tag(RegistrationUtil.class);

  private RegistrationUtil() {}

  /**
   * There's several events where a registration may or may not be considered complete based on what
   * path a user has taken. This will only truly mark registration as complete if all of the
   * requirements are met.
   */
  public static void maybeMarkRegistrationComplete() {
    if (!ZonaRosaStore.registration().isRegistrationComplete() &&
        ZonaRosaStore.account().isRegistered() &&
        !Recipient.self().getProfileName().isEmpty() &&
        (ZonaRosaStore.svr().hasPin() || ZonaRosaStore.svr().hasOptedOut() || ZonaRosaStore.account().isLinkedDevice()) &&
        RestoreDecisionStateUtil.isTerminal(ZonaRosaStore.registration().getRestoreDecisionState()))
    {
      Log.i(TAG, "Marking registration completed.", new Throwable());
      ZonaRosaStore.registration().markRegistrationComplete();
      ZonaRosaStore.registration().setLocalRegistrationMetadata(null);
      ZonaRosaStore.registration().setRestoreMethodToken(null);

      if (ZonaRosaStore.phoneNumberPrivacy().getPhoneNumberDiscoverabilityMode() == PhoneNumberDiscoverabilityMode.UNDECIDED) {
        Log.w(TAG, "Phone number discoverability mode is still UNDECIDED. Setting to DISCOVERABLE.");
        ZonaRosaStore.phoneNumberPrivacy().setPhoneNumberDiscoverabilityMode(PhoneNumberDiscoverabilityMode.DISCOVERABLE);
      }

      AppDependencies.getJobManager().startChain(new RefreshAttributesJob())
                     .then(StorageSyncJob.forRemoteChange())
                     .then(new DirectoryRefreshJob(false))
                     .enqueue();

      ZonaRosaStore.emoji().clearSearchIndexMetadata();
      EmojiSearchIndexDownloadJob.scheduleImmediately();


      BackupRepository.INSTANCE.resetInitializedStateAndAuthCredentials();
      AppDependencies.getJobManager().add(new ArchiveBackupIdReservationJob());
      AppDependencies.getJobManager().add(new PostRegistrationBackupRedemptionJob());

    } else if (!ZonaRosaStore.registration().isRegistrationComplete()) {
      Log.i(TAG, "Registration is not yet complete.", new Throwable());
    }
  }
}

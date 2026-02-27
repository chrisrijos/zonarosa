package io.zonarosa.messenger;

import android.content.Context;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobs.DeleteAbandonedAttachmentsJob;
import io.zonarosa.messenger.jobs.EmojiSearchIndexDownloadJob;
import io.zonarosa.messenger.jobs.QuoteThumbnailBackfillJob;
import io.zonarosa.messenger.jobs.StickerPackDownloadJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.migrations.ApplicationMigrations;
import io.zonarosa.messenger.migrations.QuoteThumbnailBackfillMigrationJob;
import io.zonarosa.messenger.stickers.BlessedPacks;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.core.util.Util;

/**
 * Rule of thumb: if there's something you want to do on the first app launch that involves
 * persisting state to the database, you'll almost certainly *also* want to do it post backup
 * restore, since a backup restore will wipe the current state of the database.
 */
public final class AppInitialization {

  private static final String TAG = Log.tag(AppInitialization.class);

  private AppInitialization() {}

  public static void onFirstEverAppLaunch(@NonNull Context context) {
    Log.i(TAG, "onFirstEverAppLaunch()");

    ZonaRosaPreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    ZonaRosaPreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    ZonaRosaPreferences.setLastVersionCode(context, BuildConfig.VERSION_CODE);
    ZonaRosaPreferences.setHasSeenStickerIntroTooltip(context, true);
    ZonaRosaStore.settings().setPassphraseDisabled(true);
    ZonaRosaPreferences.setReadReceiptsEnabled(context, true);
    ZonaRosaPreferences.setTypingIndicatorsEnabled(context, true);
    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    ZonaRosaStore.onFirstEverAppLaunch();
    AppDependencies.getJobManager().addAll(BlessedPacks.getFirstInstallJobs());
  }

  public static void onPostBackupRestore(@NonNull Context context) {
    Log.i(TAG, "onPostBackupRestore()");

    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    ZonaRosaStore.onPostBackupRestore();
    ZonaRosaStore.onFirstEverAppLaunch();
    ZonaRosaStore.onboarding().clearAll();
    ZonaRosaStore.settings().setPassphraseDisabled(true);
    ZonaRosaStore.notificationProfile().setHasSeenTooltip(true);
    ZonaRosaPreferences.onPostBackupRestore(context);
    ZonaRosaStore.settings().setPassphraseDisabled(true);
    AppDependencies.getJobManager().addAll(BlessedPacks.getFirstInstallJobs());
    EmojiSearchIndexDownloadJob.scheduleImmediately();
    DeleteAbandonedAttachmentsJob.enqueue();

    if (ZonaRosaStore.misc().startedQuoteThumbnailMigration()) {
      AppDependencies.getJobManager().add(new QuoteThumbnailBackfillJob());
    } else {
      AppDependencies.getJobManager().add(new QuoteThumbnailBackfillMigrationJob());
    }
  }

  /**
   * Temporary migration method that does the safest bits of {@link #onFirstEverAppLaunch(Context)}
   */
  public static void onRepairFirstEverAppLaunch(@NonNull Context context) {
    Log.w(TAG, "onRepairFirstEverAppLaunch()");

    ZonaRosaPreferences.setAppMigrationVersion(context, ApplicationMigrations.CURRENT_VERSION);
    ZonaRosaPreferences.setJobManagerVersion(context, JobManager.CURRENT_VERSION);
    ZonaRosaPreferences.setLastVersionCode(context, BuildConfig.VERSION_CODE);
    ZonaRosaPreferences.setHasSeenStickerIntroTooltip(context, true);
    ZonaRosaStore.settings().setPassphraseDisabled(true);
    AppDependencies.getMegaphoneRepository().onFirstEverAppLaunch();
    ZonaRosaStore.onFirstEverAppLaunch();
    AppDependencies.getJobManager().addAll(BlessedPacks.getFirstInstallJobs());
  }
}

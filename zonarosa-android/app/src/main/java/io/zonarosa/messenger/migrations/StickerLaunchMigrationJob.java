package io.zonarosa.messenger.migrations;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.StickerTable;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobs.MultiDeviceStickerPackOperationJob;
import io.zonarosa.messenger.jobs.StickerPackDownloadJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.stickers.BlessedPacks;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

public class StickerLaunchMigrationJob extends MigrationJob {

  public static final String KEY = "StickerLaunchMigrationJob";

  StickerLaunchMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private StickerLaunchMigrationJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public boolean isUiBlocking() {
    return false;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void performMigration() {
    installPack(context, BlessedPacks.ZOZO);
    installPack(context, BlessedPacks.BANDIT);
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  private static void installPack(@NonNull Context context, @NonNull BlessedPacks.Pack pack) {
    JobManager   jobManager      = AppDependencies.getJobManager();
    StickerTable stickerDatabase = ZonaRosaDatabase.stickers();

    if (stickerDatabase.isPackAvailableAsReference(pack.getPackId())) {
      stickerDatabase.markPackAsInstalled(pack.getPackId(), false);
    }

    jobManager.add(StickerPackDownloadJob.forInstall(pack.getPackId(), pack.getPackKey(), false));

    if (ZonaRosaStore.account().isMultiDevice()) {
      jobManager.add(new MultiDeviceStickerPackOperationJob(pack.getPackId(), pack.getPackKey(), MultiDeviceStickerPackOperationJob.Type.INSTALL));
    }
  }

  public static class Factory implements Job.Factory<StickerLaunchMigrationJob> {
    @Override
    public @NonNull
    StickerLaunchMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new StickerLaunchMigrationJob(parameters);
    }
  }
}

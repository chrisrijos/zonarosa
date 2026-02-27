package io.zonarosa.messenger.migrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.RecipientTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.model.RecipientRecord;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.storage.StorageSyncHelper;
import io.zonarosa.service.api.storage.ZonaRosaAccountRecord;
import io.zonarosa.service.api.storage.StorageId;
import io.zonarosa.service.internal.storage.protos.AccountRecord;

import java.io.IOException;

/**
 * Check for unknown fields stored on self and attempt to apply them.
 */
public class ApplyUnknownFieldsToSelfMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(ApplyUnknownFieldsToSelfMigrationJob.class);

  public static final String KEY = "ApplyUnknownFieldsToSelfMigrationJob";

  ApplyUnknownFieldsToSelfMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private ApplyUnknownFieldsToSelfMigrationJob(@NonNull Parameters parameters) {
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
    if (!ZonaRosaStore.account().isRegistered() || ZonaRosaStore.account().getAci() == null) {
      Log.w(TAG, "Not registered!");
      return;
    }

    Recipient       self;
    RecipientRecord settings;

    try {
      self     = Recipient.self();
      settings = ZonaRosaDatabase.recipients().getRecordForSync(self.getId());
    } catch (RecipientTable.MissingRecipientException e) {
      Log.w(TAG, "Unable to find self");
      return;
    }

    if (settings == null || settings.getSyncExtras().getStorageProto() == null) {
      Log.d(TAG, "No unknowns to apply");
      return;
    }

    try {
      StorageId           storageId           = StorageId.forAccount(self.getStorageId());
      AccountRecord       accountRecord       = AccountRecord.ADAPTER.decode(settings.getSyncExtras().getStorageProto());
      ZonaRosaAccountRecord zonarosaAccountRecord = new ZonaRosaAccountRecord(storageId, accountRecord);

      Log.d(TAG, "Applying potentially now known unknowns");
      StorageSyncHelper.applyAccountStorageSyncUpdates(context, self, zonarosaAccountRecord, false);
    } catch (IOException e) {
      Log.w(TAG, e);
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<ApplyUnknownFieldsToSelfMigrationJob> {
    @Override
    public @NonNull ApplyUnknownFieldsToSelfMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new ApplyUnknownFieldsToSelfMigrationJob(parameters);
    }
  }
}

package io.zonarosa.messenger.migrations;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.core.models.ServiceId.ACI;

import java.io.IOException;
import java.util.Objects;

/**
 * Couple migrations steps need to happen after we move to UUIDS.
 *  - We need to get our own UUID.
 *  - We need to fetch the new UUID sealed sender cert.
 *  - We need to do a directory sync so we can guarantee that all active users have UUIDs.
 */
public class UuidMigrationJob extends MigrationJob {

  public static final String KEY = "UuidMigrationJob";

  private static final String TAG = Log.tag(UuidMigrationJob.class);

  UuidMigrationJob() {
    this(new Parameters.Builder().addConstraint(NetworkConstraint.KEY).build());
  }

  private UuidMigrationJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  boolean isUiBlocking() {
    return false;
  }

  @Override
  void performMigration() throws Exception {
    if (!ZonaRosaStore.account().isRegistered() || TextUtils.isEmpty(ZonaRosaStore.account().getE164())) {
      Log.w(TAG, "Not registered! Skipping migration, as it wouldn't do anything.");
      return;
    }

    ensureSelfRecipientExists(context);
    fetchOwnUuid(context);
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return e instanceof IOException;
  }

  private static void ensureSelfRecipientExists(@NonNull Context context) {
    ZonaRosaDatabase.recipients().getOrInsertFromE164(Objects.requireNonNull(ZonaRosaStore.account().getE164()));
  }

  private static void fetchOwnUuid(@NonNull Context context) throws IOException {
    RecipientId self      = Recipient.self().getId();
    ACI         localUuid = ACI.parseOrNull(AppDependencies.getZonaRosaServiceAccountManager().getWhoAmI().getAci());

    if (localUuid == null) {
      throw new IOException("Invalid UUID!");
    }

    ZonaRosaDatabase.recipients().markRegisteredOrThrow(self, localUuid);
    ZonaRosaStore.account().setAci(localUuid);
  }

  public static class Factory implements Job.Factory<UuidMigrationJob> {
    @Override
    public @NonNull UuidMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new UuidMigrationJob(parameters);
    }
  }
}

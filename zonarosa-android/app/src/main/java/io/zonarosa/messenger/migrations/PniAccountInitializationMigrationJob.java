package io.zonarosa.messenger.migrations;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.protocol.state.PreKeyRecord;
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord;
import io.zonarosa.messenger.crypto.PreKeyUtil;
import io.zonarosa.messenger.crypto.storage.PreKeyMetadataStore;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.net.ZonaRosaNetwork;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.service.api.NetworkResultUtil;
import io.zonarosa.service.api.ZonaRosaServiceAccountDataStore;
import io.zonarosa.service.api.account.PreKeyUpload;
import io.zonarosa.core.models.ServiceId.PNI;
import io.zonarosa.service.api.push.ServiceIdType;

import java.io.IOException;
import java.util.List;

/**
 * Initializes various aspects of the PNI identity. Notably:
 * - Creates an identity key
 * - Creates and uploads one-time prekeys
 * - Creates and uploads signed prekeys
 */
public class PniAccountInitializationMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(PniAccountInitializationMigrationJob.class);

  public static final String KEY = "PniAccountInitializationMigrationJob";

  PniAccountInitializationMigrationJob() {
    this(new Parameters.Builder()
                       .addConstraint(NetworkConstraint.KEY)
                       .build());
  }

  private PniAccountInitializationMigrationJob(@NonNull Parameters parameters) {
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
  public void performMigration() throws IOException {
    if (ZonaRosaStore.account().isLinkedDevice()) {
      Log.i(TAG, "Linked device, skipping");
      return;
    }

    PNI pni = ZonaRosaStore.account().getPni();

    if (pni == null || ZonaRosaStore.account().getAci() == null || !Recipient.self().isRegistered()) {
      Log.w(TAG, "Not yet registered! No need to perform this migration.");
      return;
    }

    if (!ZonaRosaStore.account().hasPniIdentityKey()) {
      Log.i(TAG, "Generating PNI identity.");
      ZonaRosaStore.account().generatePniIdentityKeyIfNecessary();
    } else {
      Log.w(TAG, "Already generated the PNI identity. Skipping this step.");
    }

    ZonaRosaServiceAccountDataStore protocolStore  = AppDependencies.getProtocolStore().pni();
    PreKeyMetadataStore           metadataStore  = ZonaRosaStore.account().pniPreKeys();

    if (!metadataStore.isSignedPreKeyRegistered()) {
      Log.i(TAG, "Uploading signed prekey for PNI.");
      SignedPreKeyRecord signedPreKey   = PreKeyUtil.generateAndStoreSignedPreKey(protocolStore, metadataStore);
      List<PreKeyRecord> oneTimePreKeys = PreKeyUtil.generateAndStoreOneTimeEcPreKeys(protocolStore, metadataStore);

      NetworkResultUtil.toPreKeysLegacy(ZonaRosaNetwork.keys().setPreKeys(new PreKeyUpload(ServiceIdType.PNI, signedPreKey, oneTimePreKeys, null, null)));
      metadataStore.setActiveSignedPreKeyId(signedPreKey.getId());
      metadataStore.setSignedPreKeyRegistered(true);
    } else {
      Log.w(TAG, "Already uploaded signed prekey for PNI. Skipping this step.");
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return e instanceof IOException;
  }

  public static class Factory implements Job.Factory<PniAccountInitializationMigrationJob> {
    @Override
    public @NonNull PniAccountInitializationMigrationJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new PniAccountInitializationMigrationJob(parameters);
    }
  }
}

package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore;
import io.zonarosa.messenger.crypto.PreKeyUtil;
import io.zonarosa.messenger.crypto.storage.PreKeyMetadataStore;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;

/**
 * Deprecated. Only exists for previously-enqueued jobs. 
 * Use {@link PreKeyUtil#cleanSignedPreKeys(ZonaRosaProtocolStore, PreKeyMetadataStore)} instead.
 */
@Deprecated
public class CleanPreKeysJob extends BaseJob {

  public static final String KEY = "CleanPreKeysJob";

  private static final String TAG = Log.tag(CleanPreKeysJob.class);

  private CleanPreKeysJob(@NonNull Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public @Nullable byte[] serialize() {
    return null;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() {
    PreKeyUtil.cleanSignedPreKeys(AppDependencies.getProtocolStore().aci(), ZonaRosaStore.account().aciPreKeys());
    PreKeyUtil.cleanSignedPreKeys(AppDependencies.getProtocolStore().pni(), ZonaRosaStore.account().pniPreKeys());
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception throwable) {
    return false;
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to execute clean signed prekeys task.");
  }

  public static final class Factory implements Job.Factory<CleanPreKeysJob> {
    @Override
    public @NonNull CleanPreKeysJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new CleanPreKeysJob(parameters);
    }
  }
}

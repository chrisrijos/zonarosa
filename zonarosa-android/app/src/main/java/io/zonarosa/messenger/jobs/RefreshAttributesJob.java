package io.zonarosa.messenger.jobs;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.Base64;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.AppCapabilities;
import io.zonarosa.messenger.crypto.ProfileKeyUtil;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.JsonJobData;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues.PhoneNumberDiscoverabilityMode;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.keyvalue.SvrValues;
import io.zonarosa.messenger.net.ZonaRosaNetwork;
import io.zonarosa.messenger.registration.secondary.DeviceNameCipher;
import io.zonarosa.messenger.registration.data.RegistrationRepository;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.service.api.NetworkResultUtil;
import io.zonarosa.service.api.account.AccountAttributes;
import io.zonarosa.service.api.crypto.UnidentifiedAccess;
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class RefreshAttributesJob extends BaseJob {

  public static final String KEY = "RefreshAttributesJob";

  private static final String TAG = Log.tag(RefreshAttributesJob.class);

  private static final String KEY_FORCED = "forced";

  private static volatile boolean hasRefreshedThisAppCycle;

  private final boolean forced;

  public static RefreshAttributesJob forAccountRestore() {
    return new RefreshAttributesJob(true, Parameters.PRIORITY_HIGH);
  }

  public RefreshAttributesJob() {
    this(true);
  }

  /**
   * @param forced True if you want this job to run no matter what. False if you only want this job
   *               to run if it hasn't run yet this app cycle.
   */
  public RefreshAttributesJob(boolean forced) {
    this(forced, Parameters.PRIORITY_DEFAULT);
  }

  private RefreshAttributesJob(boolean forced, @Parameters.Priority int priority) {
    this(new Job.Parameters.Builder()
                           .setGlobalPriority(priority)
                           .addConstraint(NetworkConstraint.KEY)
                           .setQueue("RefreshAttributesJob")
                           .setMaxInstancesForFactory(2)
                           .setLifespan(TimeUnit.DAYS.toMillis(30))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .build(),
         forced);
  }

  private RefreshAttributesJob(@NonNull Job.Parameters parameters, boolean forced) {
    super(parameters);
    this.forced = forced;
  }

  @Override
  public @Nullable byte[] serialize() {
    return new JsonJobData.Builder().putBoolean(KEY_FORCED, forced).serialize();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws IOException {
    if (!ZonaRosaStore.account().isRegistered() || ZonaRosaStore.account().getE164() == null) {
      Log.w(TAG, "Not yet registered. Skipping.");
      return;
    }

    if (!forced && hasRefreshedThisAppCycle) {
      Log.d(TAG, "Already refreshed this app cycle. Skipping.");
      return;
    }

    int       registrationId              = ZonaRosaStore.account().getRegistrationId();
    boolean   fetchesMessages             = !ZonaRosaStore.account().isFcmEnabled() || ZonaRosaStore.internal().isWebsocketModeForced();
    byte[]    unidentifiedAccessKey       = UnidentifiedAccess.deriveAccessKeyFrom(ProfileKeyUtil.getSelfProfileKey());
    boolean   universalUnidentifiedAccess = ZonaRosaPreferences.isUniversalUnidentifiedAccess(context);
    String    registrationLockV2          = null;
    SvrValues svrValues                   = ZonaRosaStore.svr();
    int       pniRegistrationId           = RegistrationRepository.getPniRegistrationId();
    String    recoveryPassword            = svrValues.getMasterKey().deriveRegistrationRecoveryPassword();

    if (svrValues.isRegistrationLockEnabled()) {
      registrationLockV2 = svrValues.getRegistrationLockToken();
    }

    boolean phoneNumberDiscoverable = ZonaRosaStore.phoneNumberPrivacy().getPhoneNumberDiscoverabilityMode() == PhoneNumberDiscoverabilityMode.DISCOVERABLE;

    String deviceName = ZonaRosaStore.account().getDeviceName();
    byte[] encryptedDeviceName = (deviceName == null) ? null : DeviceNameCipher.encryptDeviceName(deviceName.getBytes(StandardCharsets.UTF_8), ZonaRosaStore.account().getAciIdentityKey());

    AccountAttributes.Capabilities capabilities = AppCapabilities.getCapabilities(svrValues.hasPin() && !svrValues.hasOptedOut());
    Log.i(TAG, "Calling setAccountAttributes() reglockV2? " + !TextUtils.isEmpty(registrationLockV2) + ", pin? " + svrValues.hasPin() + ", restoredAEP? " + ZonaRosaStore.account().restoredAccountEntropyPool() +
               "\n    Recovery password? " + !TextUtils.isEmpty(recoveryPassword) +
               "\n    Phone number discoverable : " + phoneNumberDiscoverable +
               "\n    Device Name : " + (encryptedDeviceName != null) +
               "\n  Capabilities: " + capabilities);

    AccountAttributes accountAttributes = new AccountAttributes(
        null,
        registrationId,
        fetchesMessages,
        registrationLockV2,
        unidentifiedAccessKey,
        universalUnidentifiedAccess,
        capabilities,
        phoneNumberDiscoverable,
        (encryptedDeviceName == null) ? null : Base64.encodeWithPadding(encryptedDeviceName),
        pniRegistrationId,
        recoveryPassword
    );

    NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.account().setAccountAttributes(accountAttributes));

    hasRefreshedThisAppCycle = true;
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof IOException && !(e instanceof NonSuccessfulResponseCodeException);
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to update account attributes!");
  }

  public static class Factory implements Job.Factory<RefreshAttributesJob> {
    @Override
    public @NonNull RefreshAttributesJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      JsonJobData data = JsonJobData.deserialize(serializedData);
      return new RefreshAttributesJob(parameters, data.getBooleanOrDefault(KEY_FORCED, true));
    }
  }
}

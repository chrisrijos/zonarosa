package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.keyvalue.CertificateType;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.net.ZonaRosaNetwork;
import io.zonarosa.messenger.util.ExceptionHelper;
import io.zonarosa.service.api.NetworkResultUtil;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public final class RotateCertificateJob extends BaseJob {

  public static final String KEY = "RotateCertificateJob";

  private static final String TAG = Log.tag(RotateCertificateJob.class);

  public RotateCertificateJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("__ROTATE_SENDER_CERTIFICATE__")
                           .addConstraint(NetworkConstraint.KEY)
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .build());
  }

  private RotateCertificateJob(@NonNull Job.Parameters parameters) {
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
  public void onAdded() {}

  @Override
  public void onRun() throws IOException {
    if (!ZonaRosaStore.account().isRegistered()) {
      Log.w(TAG, "Not yet registered. Ignoring.");
      return;
    }

    synchronized (RotateCertificateJob.class) {
      Collection<CertificateType> certificateTypes = ZonaRosaStore.phoneNumberPrivacy()
                                                                .getAllCertificateTypes();

      Log.i(TAG, "Rotating these certificates " + certificateTypes);

      for (CertificateType certificateType: certificateTypes) {
        byte[] certificate;

        switch (certificateType) {
          case ACI_AND_E164: certificate = NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.certificate().getSenderCertificate()); break;
          case ACI_ONLY    : certificate = NetworkResultUtil.toBasicLegacy(ZonaRosaNetwork.certificate().getSenderCertificateForPhoneNumberPrivacy()); break;
          default          : throw new AssertionError();
        }

        Log.i(TAG, String.format("Successfully got %s certificate", certificateType));
        ZonaRosaStore.certificate()
                   .setUnidentifiedAccessCertificate(certificateType, certificate);
      }
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return ExceptionHelper.isRetryableIOException(e);
  }

  @Override
  public void onFailure() {
    Log.w(TAG, "Failed to rotate sender certificate!");
  }

  public static final class Factory implements Job.Factory<RotateCertificateJob> {
    @Override
    public @NonNull RotateCertificateJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new RotateCertificateJob(parameters);
    }
  }
}

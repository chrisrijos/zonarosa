package io.zonarosa.messenger.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.BuildConfig;
import io.zonarosa.messenger.jobmanager.Job;
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint;
import io.zonarosa.messenger.transport.RetryLaterException;
import io.zonarosa.messenger.util.ZonaRosaPreferences;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ServiceOutageDetectionJob extends BaseJob {

  public static final String KEY = "ServiceOutageDetectionJob";

  private static final String TAG = Log.tag(ServiceOutageDetectionJob.class);

  private static final String IP_SUCCESS = "127.0.0.1";
  private static final String IP_FAILURE = "127.0.0.2";
  private static final long   CHECK_TIME = 1000 * 60;

  public ServiceOutageDetectionJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("ServiceOutageDetectionJob")
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(5)
                           .setMaxInstancesForFactory(1)
                           .build());
  }

  private ServiceOutageDetectionJob(@NonNull Job.Parameters parameters) {
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
  public void onRun() throws RetryLaterException {
    Log.i(TAG, "onRun()");

    long timeSinceLastCheck = System.currentTimeMillis() - ZonaRosaPreferences.getLastOutageCheckTime(context);
    if (timeSinceLastCheck < CHECK_TIME) {
      Log.w(TAG, "Skipping service outage check. Too soon.");
      return;
    }

    try {
      InetAddress address = InetAddress.getByName(BuildConfig.ZONAROSA_SERVICE_STATUS_URL);
      Log.i(TAG, "Received outage check address: " + address.getHostAddress());

      if (IP_SUCCESS.equals(address.getHostAddress())) {
        Log.i(TAG, "Service is available.");
        ZonaRosaPreferences.setServiceOutage(context, false);
      } else if (IP_FAILURE.equals(address.getHostAddress())) {
        Log.w(TAG, "Service is down.");
        ZonaRosaPreferences.setServiceOutage(context, true);
      } else {
        Log.w(TAG, "Service status check returned an unrecognized IP address. Could be a weird network state. Prompting retry.");
        throw new RetryLaterException(new Exception("Unrecognized service outage IP address."));
      }

      ZonaRosaPreferences.setLastOutageCheckTime(context, System.currentTimeMillis());
    } catch (UnknownHostException e) {
      throw new RetryLaterException(e);
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof RetryLaterException;
  }

  @Override
  public void onFailure() {
    Log.i(TAG, "Service status check could not complete. Assuming success to avoid false positives due to bad network.");
    ZonaRosaPreferences.setServiceOutage(context, false);
    ZonaRosaPreferences.setLastOutageCheckTime(context, System.currentTimeMillis());
  }

  public static final class Factory implements Job.Factory<ServiceOutageDetectionJob> {
    @Override
    public @NonNull ServiceOutageDetectionJob create(@NonNull Parameters parameters, @Nullable byte[] serializedData) {
      return new ServiceOutageDetectionJob(parameters);
    }
  }
}

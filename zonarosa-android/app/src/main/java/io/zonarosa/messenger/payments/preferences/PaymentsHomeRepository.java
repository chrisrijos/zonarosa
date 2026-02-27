package io.zonarosa.messenger.payments.preferences;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.jobs.PaymentLedgerUpdateJob;
import io.zonarosa.messenger.jobs.ProfileUploadJob;
import io.zonarosa.messenger.jobs.SendPaymentsActivatedJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.util.AsynchronousCallback;
import io.zonarosa.messenger.util.ProfileUtil;
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException;
import io.zonarosa.service.internal.push.exceptions.PaymentsRegionException;

import java.io.IOException;

public class PaymentsHomeRepository {

  private static final String TAG = Log.tag(PaymentsHomeRepository.class);

  public void activatePayments(@NonNull AsynchronousCallback.WorkerThread<Void, Error> callback) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      ZonaRosaStore.payments().setMobileCoinPaymentsEnabled(true);
      try {
        ProfileUtil.uploadProfile(AppDependencies.getApplication());
        AppDependencies.getJobManager()
                       .startChain(PaymentLedgerUpdateJob.updateLedger())
                       .then(new SendPaymentsActivatedJob())
                       .enqueue();
        callback.onComplete(null);
      } catch (PaymentsRegionException e) {
        ZonaRosaStore.payments().setMobileCoinPaymentsEnabled(false);
        Log.w(TAG, "Problem enabling payments in region", e);
        callback.onError(Error.RegionError);
      } catch (NonSuccessfulResponseCodeException e) {
        ZonaRosaStore.payments().setMobileCoinPaymentsEnabled(false);
        Log.w(TAG, "Problem enabling payments", e);
        callback.onError(Error.NetworkError);
      } catch (IOException e) {
        ZonaRosaStore.payments().setMobileCoinPaymentsEnabled(false);
        Log.w(TAG, "Problem enabling payments", e);
        tryToRestoreProfile();
        callback.onError(Error.NetworkError);
      }
    });
  }

  private void tryToRestoreProfile() {
    try {
      ProfileUtil.uploadProfile(AppDependencies.getApplication());
      Log.i(TAG, "Restored profile");
    } catch (IOException e) {
      Log.w(TAG, "Problem uploading profile", e);
    }
  }

  public void deactivatePayments(@NonNull Consumer<Boolean> consumer) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      ZonaRosaStore.payments().setMobileCoinPaymentsEnabled(false);
      AppDependencies.getJobManager().add(new ProfileUploadJob());
      consumer.accept(!ZonaRosaStore.payments().mobileCoinPaymentsEnabled());
    });
  }

  public enum Error {
    NetworkError,
    RegionError
  }
}

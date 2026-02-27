package io.zonarosa.messenger.payments.currency;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.payments.Payments;
import io.zonarosa.messenger.util.AsynchronousCallback;

import java.io.IOException;

public final class CurrencyExchangeRepository {

  private static final String TAG = Log.tag(CurrencyExchangeRepository.class);

  private final Payments payments;

  public CurrencyExchangeRepository(@NonNull Payments payments) {
    this.payments = payments;
  }

  @AnyThread
  public void getCurrencyExchange(@NonNull AsynchronousCallback.WorkerThread<CurrencyExchange, Throwable> callback, boolean refreshIfAble) {
    ZonaRosaExecutors.BOUNDED.execute(() -> {
      try {
        callback.onComplete(payments.getCurrencyExchange(refreshIfAble));
      } catch (IOException e) {
        Log.w(TAG, e);
        callback.onError(e);
      }
    });
  }
}

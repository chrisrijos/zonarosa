package io.zonarosa.messenger.push;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.google.android.gms.security.ProviderInstaller;

import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.BuildConfig;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.service.api.ZonaRosaServiceAccountManager;

public class AccountManagerFactory {

  private static AccountManagerFactory instance;
  public static AccountManagerFactory getInstance() {
    if (instance == null) {
      synchronized (AccountManagerFactory.class) {
        if (instance == null) {
          instance = new AccountManagerFactory();
        }
      }
    }
    return instance;
  }

  @VisibleForTesting
  public static void setInstance(@NonNull AccountManagerFactory accountManagerFactory) {
    synchronized (AccountManagerFactory.class) {
      instance = accountManagerFactory;
    }
  }
  private static final String TAG = Log.tag(AccountManagerFactory.class);

  /**
   * Should only be used during registration when you haven't yet been assigned an ACI.
   */
  public @NonNull ZonaRosaServiceAccountManager createUnauthenticated(@NonNull Context context,
                                                                    @NonNull String e164,
                                                                    int deviceId,
                                                                    @NonNull String password)
  {
    if (new ZonaRosaServiceNetworkAccess(context).isCensored(e164)) {
      ZonaRosaExecutors.BOUNDED.execute(() -> {
        try {
          ProviderInstaller.installIfNeeded(context);
        } catch (Throwable t) {
          Log.w(TAG, t);
        }
      });
    }

    return ZonaRosaServiceAccountManager.createWithStaticCredentials(
        AppDependencies.getZonaRosaServiceNetworkAccess().getConfiguration(e164),
        null,
        null,
        e164,
        deviceId,
        password,
        BuildConfig.ZONAROSA_AGENT,
        RemoteConfig.okHttpAutomaticRetry(),
        RemoteConfig.groupLimits().getHardLimit()
    );
  }

}

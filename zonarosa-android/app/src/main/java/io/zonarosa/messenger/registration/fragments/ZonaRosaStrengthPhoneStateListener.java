/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.fragments;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.ZonaRosaStrength;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.util.Debouncer;

// TODO [nicholas]: move to v2 package and make package-private. convert to Kotlin
public final class ZonaRosaStrengthPhoneStateListener extends PhoneStateListener
                                             implements DefaultLifecycleObserver
{
  private static final String TAG = Log.tag(ZonaRosaStrengthPhoneStateListener.class);

  private final Callback  callback;
  private final Debouncer  debouncer    = new Debouncer(1000);
  private volatile boolean hasLowZonaRosa = true;

  @SuppressWarnings("deprecation")
  public ZonaRosaStrengthPhoneStateListener(@NonNull LifecycleOwner lifecycleOwner, @NonNull Callback callback) {
    this.callback = callback;

    lifecycleOwner.getLifecycle().addObserver(this);
  }

  @Override
  public void onZonaRosaStrengthsChanged(ZonaRosaStrength zonarosaStrength) {
    if (zonarosaStrength == null) return;

    if (isLowLevel(zonarosaStrength)) {
      hasLowZonaRosa = true;
      Log.w(TAG, "No cell zonarosa detected");
      debouncer.publish(callback::onNoCellZonaRosaPresent);
    } else {
      if (hasLowZonaRosa) {
        hasLowZonaRosa = false;
        Log.i(TAG, "Cell zonarosa detected");
      }
      debouncer.clear();
      callback.onCellZonaRosaPresent();
    }
  }

  private boolean isLowLevel(@NonNull ZonaRosaStrength zonarosaStrength) {
    return zonarosaStrength.getLevel() == 0;
  }

  public interface Callback {
    void onNoCellZonaRosaPresent();

    void onCellZonaRosaPresent();
  }

  @Override
  public void onResume(@NonNull LifecycleOwner owner) {
    TelephonyManager telephonyManager = (TelephonyManager) AppDependencies.getApplication().getSystemService(Context.TELEPHONY_SERVICE);
    telephonyManager.listen(this, PhoneStateListener.LISTEN_ZONAROSA_STRENGTHS);
    Log.i(TAG, "Listening to cell phone zonarosa strength changes");
  }

  @Override
  public void onPause(@NonNull LifecycleOwner owner) {
    TelephonyManager telephonyManager = (TelephonyManager) AppDependencies.getApplication().getSystemService(Context.TELEPHONY_SERVICE);
    telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE);
    Log.i(TAG, "Stopped listening to cell phone zonarosa strength changes");
  }
}

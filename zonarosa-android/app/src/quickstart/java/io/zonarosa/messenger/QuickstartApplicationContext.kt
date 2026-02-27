/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.os.Bundle
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Application subclass for the quickstart build variant.
 * On first launch, if the account is not yet registered, it triggers
 * [QuickstartInitializer] to import pre-baked credentials from assets.
 */
class QuickstartApplicationContext : ApplicationContext() {

  companion object {
    private val TAG = Log.tag(QuickstartApplicationContext::class.java)
  }

  override fun onCreate() {
    super.onCreate()
    if (!ZonaRosaStore.account.isRegistered) {
      Log.i(TAG, "Account not registered, attempting quickstart initialization...")
      QuickstartInitializer.initialize(this)

      if (QuickstartInitializer.pendingBackupDir != null) {
        Log.i(TAG, "Pending backup detected, will redirect to restore activity")
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
          override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity is QuickstartRestoreActivity) return
            unregisterActivityLifecycleCallbacks(this)
            activity.startActivity(Intent(activity, QuickstartRestoreActivity::class.java))
            activity.finish()
          }

          override fun onActivityStarted(activity: Activity) = Unit
          override fun onActivityResumed(activity: Activity) = Unit
          override fun onActivityPaused(activity: Activity) = Unit
          override fun onActivityStopped(activity: Activity) = Unit
          override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
          override fun onActivityDestroyed(activity: Activity) = Unit
        })
      }
    }
  }
}

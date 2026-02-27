/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobmanager.impl

import android.app.Application
import android.app.job.JobInfo
import android.content.Context
import io.zonarosa.messenger.jobmanager.Constraint
import io.zonarosa.messenger.util.NetworkUtil

/**
 * Constraint that, when added, means that a job cannot be performed unless the user has Wifi
 */
class WifiConstraint(private val application: Application) : Constraint {

  companion object {
    const val KEY = "WifiConstraint"

    fun isMet(context: Context): Boolean {
      return NetworkUtil.isConnectedWifi(context)
    }
  }

  override fun isMet(): Boolean {
    return isMet(application)
  }

  override fun getFactoryKey(): String = KEY

  override fun applyToJobInfo(jobInfoBuilder: JobInfo.Builder) = Unit

  class Factory(val application: Application) : Constraint.Factory<WifiConstraint> {
    override fun create(): WifiConstraint {
      return WifiConstraint(application)
    }
  }
}

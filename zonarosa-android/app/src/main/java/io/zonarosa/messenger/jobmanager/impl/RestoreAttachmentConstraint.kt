/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobmanager.impl

import android.app.Application
import android.app.job.JobInfo
import android.content.Context
import io.zonarosa.messenger.jobmanager.Constraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Constraint that, when added, means that a job cannot be performed unless the user either has Wifi or, if they enabled it, cellular
 */
class RestoreAttachmentConstraint(private val application: Application) : Constraint {

  companion object {
    const val KEY = "RestoreAttachmentConstraint"

    fun isMet(context: Context): Boolean {
      if (ZonaRosaStore.backup.restoreWithCellular) {
        return NetworkConstraint.isMet(context)
      }
      return WifiConstraint.isMet(context)
    }
  }

  override fun isMet(): Boolean {
    return isMet(application)
  }

  override fun getFactoryKey(): String = KEY

  override fun applyToJobInfo(jobInfoBuilder: JobInfo.Builder) = Unit

  class Factory(val application: Application) : Constraint.Factory<RestoreAttachmentConstraint> {
    override fun create(): RestoreAttachmentConstraint {
      return RestoreAttachmentConstraint(application)
    }
  }
}

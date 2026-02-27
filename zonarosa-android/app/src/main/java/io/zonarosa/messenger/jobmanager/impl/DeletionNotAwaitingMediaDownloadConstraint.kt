/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobmanager.impl

import android.app.job.JobInfo
import io.zonarosa.messenger.backup.DeletionState
import io.zonarosa.messenger.jobmanager.Constraint
import io.zonarosa.messenger.jobmanager.ConstraintObserver
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * When we are awaiting media download, we want to suppress the running of the
 * deletion job such that once media *is* downloaded it can finish off deleting
 * the backup.
 */
object DeletionNotAwaitingMediaDownloadConstraint : Constraint {

  const val KEY = "DeletionNotAwaitingMediaDownloadConstraint"

  override fun isMet(): Boolean {
    return ZonaRosaStore.backup.deletionState != DeletionState.AWAITING_MEDIA_DOWNLOAD
  }

  override fun getFactoryKey(): String = KEY

  override fun applyToJobInfo(jobInfoBuilder: JobInfo.Builder) = Unit

  object Observer : ConstraintObserver {
    val listeners: MutableSet<ConstraintObserver.Notifier> = mutableSetOf()

    override fun register(notifier: ConstraintObserver.Notifier) {
      listeners += notifier
    }

    fun notifyListeners() {
      for (listener in listeners) {
        listener.onConstraintMet(KEY)
      }
    }
  }

  class Factory : Constraint.Factory<DeletionNotAwaitingMediaDownloadConstraint> {
    override fun create(): DeletionNotAwaitingMediaDownloadConstraint {
      return DeletionNotAwaitingMediaDownloadConstraint
    }
  }
}

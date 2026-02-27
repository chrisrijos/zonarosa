/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobmanager.impl

import android.app.job.JobInfo
import io.zonarosa.messenger.jobmanager.Constraint
import io.zonarosa.messenger.jobmanager.ConstraintObserver
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * A constraint that is met so long as the current user is registered.
 */
object RegisteredConstraint : Constraint {

  const val KEY = "RegisteredConstraint"

  override fun isMet(): Boolean {
    return ZonaRosaStore.account.isRegistered && ZonaRosaStore.account.aci != null
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

  class Factory : Constraint.Factory<RegisteredConstraint> {
    override fun create(): RegisteredConstraint {
      return RegisteredConstraint
    }
  }
}

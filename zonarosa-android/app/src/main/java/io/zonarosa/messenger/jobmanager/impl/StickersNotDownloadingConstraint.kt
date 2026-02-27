/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobmanager.impl

import android.app.job.JobInfo
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Constraint
import io.zonarosa.messenger.jobmanager.ConstraintObserver
import io.zonarosa.messenger.jobs.StickerDownloadJob
import io.zonarosa.messenger.jobs.StickerPackDownloadJob

/**
 * When met, no sticker download jobs should be in the job queue/running.
 */
object StickersNotDownloadingConstraint : Constraint {

  const val KEY = "StickersNotDownloadingConstraint"

  private val factoryKeys = setOf(StickerPackDownloadJob.KEY, StickerDownloadJob.KEY)

  override fun isMet(): Boolean {
    return AppDependencies.jobManager.areFactoriesEmpty(factoryKeys)
  }

  override fun getFactoryKey(): String = KEY

  override fun applyToJobInfo(jobInfoBuilder: JobInfo.Builder) = Unit

  object Observer : ConstraintObserver {
    override fun register(notifier: ConstraintObserver.Notifier) {
      AppDependencies.jobManager.addListener({ job -> factoryKeys.contains(job.factoryKey) }) { job, jobState ->
        if (jobState.isComplete) {
          if (isMet) {
            notifier.onConstraintMet(KEY)
          }
        }
      }
    }
  }

  class Factory : Constraint.Factory<StickersNotDownloadingConstraint> {
    override fun create(): StickersNotDownloadingConstraint {
      return StickersNotDownloadingConstraint
    }
  }
}

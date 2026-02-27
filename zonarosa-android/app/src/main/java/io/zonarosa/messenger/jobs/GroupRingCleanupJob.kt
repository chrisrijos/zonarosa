/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import java.util.concurrent.TimeUnit

/**
 * Cleans database of stale group rings which can occur if the device or application
 * crashes while an incoming ring is happening.
 */
class GroupRingCleanupJob private constructor(parameters: Parameters) : BaseJob(parameters) {
  companion object {
    const val KEY = "GroupRingCleanupJob"

    @JvmStatic
    fun enqueue() {
      AppDependencies.jobManager.add(
        GroupRingCleanupJob(
          Parameters.Builder()
            .addConstraint(NetworkConstraint.KEY)
            .setLifespan(TimeUnit.HOURS.toMillis(1))
            .setMaxInstancesForFactory(1)
            .setQueue(KEY)
            .build()
        )
      )
    }
  }

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    ZonaRosaDatabase.calls.getLatestRingingCalls().forEach {
      AppDependencies.zonarosaCallManager.peekGroupCall(it.peer)
    }

    ZonaRosaDatabase.calls.markRingingCallsAsMissed()
  }

  override fun onShouldRetry(e: Exception): Boolean = false

  class Factory : Job.Factory<GroupRingCleanupJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): GroupRingCleanupJob {
      return GroupRingCleanupJob(parameters)
    }
  }
}

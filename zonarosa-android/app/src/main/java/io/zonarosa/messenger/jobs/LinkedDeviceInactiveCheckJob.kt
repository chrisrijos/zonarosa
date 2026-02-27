/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.roundedString
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.devicelist.protos.DeviceName
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.keyvalue.protos.LeastActiveLinkedDevice
import io.zonarosa.messenger.registration.secondary.DeviceNameCipher
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import java.io.IOException
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/**
 * Designed as a routine check to keep an eye on how active our linked devices are.
 */
class LinkedDeviceInactiveCheckJob private constructor(
  parameters: Parameters = Parameters.Builder()
    .setQueue("LinkedDeviceInactiveCheckJob")
    .setMaxInstancesForFactory(2)
    .setLifespan(30.days.inWholeMilliseconds)
    .setMaxAttempts(Parameters.UNLIMITED)
    .addConstraint(NetworkConstraint.KEY)
    .build()
) : Job(parameters) {

  companion object {
    private val TAG = Log.tag(LinkedDeviceInactiveCheckJob::class.java)
    const val KEY = "LinkedDeviceInactiveCheckJob"

    @JvmStatic
    fun enqueue() {
      AppDependencies.jobManager.add(LinkedDeviceInactiveCheckJob())
    }

    @JvmStatic
    fun enqueueIfNecessary() {
      if (!ZonaRosaStore.account.isRegistered) {
        Log.i(TAG, "Not registered, skipping enqueue.")
        return
      }

      val timeSinceLastCheck = System.currentTimeMillis() - ZonaRosaStore.misc.linkedDeviceLastActiveCheckTime
      if (timeSinceLastCheck > 1.days.inWholeMilliseconds || timeSinceLastCheck < 0) {
        AppDependencies.jobManager.add(LinkedDeviceInactiveCheckJob())
      }
    }
  }

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.i(TAG, "Not registered, skipping.")
      return Result.success()
    }

    if (ZonaRosaStore.account.isLinkedDevice) {
      Log.i(TAG, "Not primary, skipping")
      return Result.success()
    }

    val devices = try {
      AppDependencies
        .linkDeviceApi
        .getDevices()
        .successOrThrow()
        .filter { it.id != ZonaRosaServiceAddress.DEFAULT_DEVICE_ID }
    } catch (e: IOException) {
      return Result.retry(defaultBackoff())
    }

    if (devices.isEmpty()) {
      Log.i(TAG, "No linked devices found.")

      ZonaRosaStore.account.isMultiDevice = false
      ZonaRosaStore.misc.leastActiveLinkedDevice = null
      ZonaRosaStore.misc.linkedDeviceLastActiveCheckTime = System.currentTimeMillis()

      return Result.success()
    }

    val leastActiveDevice: LeastActiveLinkedDevice? = devices
      .filter { it.name != null }
      .minByOrNull { it.lastSeen }
      ?.let {
        val nameProto = DeviceName.ADAPTER.decode(Base64.decode(it.getName()))
        val decryptedBytes = DeviceNameCipher.decryptDeviceName(nameProto, AppDependencies.protocolStore.aci().identityKeyPair) ?: return@let null
        val name = String(decryptedBytes)

        LeastActiveLinkedDevice(
          name = name,
          lastActiveTimestamp = it.lastSeen
        )
      }

    if (leastActiveDevice == null) {
      Log.w(TAG, "Failed to decrypt linked device name.")
      ZonaRosaStore.account.isMultiDevice = true
      ZonaRosaStore.misc.leastActiveLinkedDevice = null
      ZonaRosaStore.misc.linkedDeviceLastActiveCheckTime = System.currentTimeMillis()
      return Result.success()
    }

    val timeSinceActive = System.currentTimeMillis() - leastActiveDevice.lastActiveTimestamp
    Log.i(TAG, "Least active linked device was last active ${timeSinceActive.milliseconds.toDouble(DurationUnit.DAYS).roundedString(2)} days ago ($timeSinceActive ms).")

    ZonaRosaStore.account.isMultiDevice = true
    ZonaRosaStore.misc.leastActiveLinkedDevice = leastActiveDevice
    ZonaRosaStore.misc.linkedDeviceLastActiveCheckTime = System.currentTimeMillis()

    return Result.success()
  }

  override fun onFailure() {
  }

  class Factory : Job.Factory<LinkedDeviceInactiveCheckJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): LinkedDeviceInactiveCheckJob {
      return LinkedDeviceInactiveCheckJob(parameters)
    }
  }
}

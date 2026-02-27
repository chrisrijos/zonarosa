package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.protos.DeviceNameChangeJobData
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.service.api.crypto.UntrustedIdentityException
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.internal.push.SyncMessage
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Sends a sync message that a linked device has changed its name
 */
class DeviceNameChangeJob private constructor(
  private val data: DeviceNameChangeJobData,
  parameters: Parameters
) : Job(parameters) {
  companion object {
    const val KEY: String = "DeviceNameChangeJob"
    private val TAG = Log.tag(DeviceNameChangeJob::class.java)
  }

  constructor(
    deviceId: Int
  ) : this(
    DeviceNameChangeJobData(deviceId),
    Parameters.Builder()
      .addConstraint(NetworkConstraint.KEY)
      .setQueue("DeviceNameChangeJob")
      .setLifespan(TimeUnit.DAYS.toMillis(1))
      .setMaxAttempts(Parameters.UNLIMITED)
      .build()
  )

  override fun serialize(): ByteArray {
    return data.encode()
  }

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    if (!Recipient.self().isRegistered) {
      Log.w(TAG, "Not registered")
      return Result.failure()
    }

    return try {
      val result = AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(
        ZonaRosaServiceSyncMessage.forDeviceNameChange(SyncMessage.DeviceNameChange(data.deviceId))
      )
      if (result.isSuccess) {
        Result.success()
      } else {
        Log.w(TAG, "Unable to send device name sync - trying later")
        Result.retry(defaultBackoff())
      }
    } catch (e: IOException) {
      Log.w(TAG, "Unable to send device name sync - trying later", e)
      Result.retry(defaultBackoff())
    } catch (e: UntrustedIdentityException) {
      Log.w(TAG, "Unable to send device name sync", e)
      Result.failure()
    }
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<DeviceNameChangeJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): DeviceNameChangeJob {
      return DeviceNameChangeJob(DeviceNameChangeJobData.ADAPTER.decode(serializedData!!), parameters)
    }
  }
}

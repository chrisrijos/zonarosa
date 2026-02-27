package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.isNotNullOrBlank
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import kotlin.time.Duration.Companion.days

/**
 * Job to refresh remote configs. Utilizes eTags so a 304 is returned if content is unchanged since last fetch.
 */
class RemoteConfigRefreshJob private constructor(parameters: Parameters) : Job(parameters) {
  companion object {
    const val KEY: String = "RemoteConfigRefreshJob"
    private val TAG = Log.tag(RemoteConfigRefreshJob::class.java)
  }

  constructor() : this(
    Parameters.Builder()
      .setQueue(KEY)
      .addConstraint(NetworkConstraint.KEY)
      .setMaxInstancesForFactory(1)
      .setMaxAttempts(Parameters.UNLIMITED)
      .setLifespan(1.days.inWholeMilliseconds)
      .build()
  )

  override fun serialize(): ByteArray? {
    return null
  }

  override fun getFactoryKey(): String {
    return KEY
  }

  override fun run(): Result {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.w(TAG, "Not registered. Skipping.")
      return Result.success()
    }

    return when (val result = ZonaRosaNetwork.remoteConfig.getRemoteConfig(ZonaRosaStore.remoteConfig.eTag)) {
      is NetworkResult.Success -> {
        RemoteConfig.update(result.result.config)
        ZonaRosaStore.misc.setLastKnownServerTime(result.result.serverEpochTimeMilliseconds, System.currentTimeMillis())
        if (result.result.eTag.isNotNullOrBlank()) {
          ZonaRosaStore.remoteConfig.eTag = result.result.eTag
        }
        Result.success()
      }

      is NetworkResult.ApplicationError -> Result.failure()
      is NetworkResult.NetworkError -> Result.retry(defaultBackoff())
      is NetworkResult.StatusCodeError ->
        if (result.code == 304) {
          Log.i(TAG, "Remote config has not changed since last pull.")
          ZonaRosaStore.remoteConfig.lastFetchTime = System.currentTimeMillis()
          ZonaRosaStore.misc.setLastKnownServerTime(result.header(ZonaRosaWebSocket.SERVER_DELIVERED_TIMESTAMP_HEADER)?.toLongOrNull() ?: System.currentTimeMillis(), System.currentTimeMillis())
          Result.success()
        } else {
          Result.retry(defaultBackoff())
        }
    }
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<RemoteConfigRefreshJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): RemoteConfigRefreshJob {
      return RemoteConfigRefreshJob(parameters)
    }
  }
}

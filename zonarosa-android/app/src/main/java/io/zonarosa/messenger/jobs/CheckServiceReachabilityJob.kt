package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.stories.Stories
import io.zonarosa.service.api.websocket.WebSocketConnectionState
import io.zonarosa.service.internal.util.StaticCredentialsProvider
import io.zonarosa.service.internal.websocket.OkHttpWebSocketConnection
import java.util.Optional
import java.util.concurrent.TimeUnit

/**
 * Checks to see if a censored user can establish a websocket connection with an uncensored network configuration.
 */
class CheckServiceReachabilityJob private constructor(params: Parameters) : BaseJob(params) {

  constructor() : this(
    Parameters.Builder()
      .addConstraint(NetworkConstraint.KEY)
      .setLifespan(TimeUnit.HOURS.toMillis(12))
      .setMaxAttempts(1)
      .build()
  )

  companion object {
    private val TAG = Log.tag(CheckServiceReachabilityJob::class.java)

    const val KEY = "CheckServiceReachabilityJob"

    @JvmStatic
    fun enqueueIfNecessary() {
      val isCensored = AppDependencies.zonarosaServiceNetworkAccess.isCensored()
      val timeSinceLastCheck = System.currentTimeMillis() - ZonaRosaStore.misc.lastCensorshipServiceReachabilityCheckTime
      if (ZonaRosaStore.account.isRegistered && isCensored && timeSinceLastCheck > TimeUnit.DAYS.toMillis(1)) {
        AppDependencies.jobManager.add(CheckServiceReachabilityJob())
      }
    }
  }

  override fun serialize(): ByteArray? {
    return null
  }

  override fun getFactoryKey(): String {
    return KEY
  }

  override fun onRun() {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.w(TAG, "Not registered, skipping.")
      ZonaRosaStore.misc.lastCensorshipServiceReachabilityCheckTime = System.currentTimeMillis()
      return
    }

    if (!AppDependencies.zonarosaServiceNetworkAccess.isCensored()) {
      Log.w(TAG, "Not currently censored, skipping.")
      ZonaRosaStore.misc.lastCensorshipServiceReachabilityCheckTime = System.currentTimeMillis()
      return
    }

    ZonaRosaStore.misc.lastCensorshipServiceReachabilityCheckTime = System.currentTimeMillis()

    val uncensoredWebsocket = OkHttpWebSocketConnection(
      "uncensored-test",
      AppDependencies.zonarosaServiceNetworkAccess.uncensoredConfiguration,
      Optional.of(
        StaticCredentialsProvider(
          ZonaRosaStore.account.aci,
          ZonaRosaStore.account.pni,
          ZonaRosaStore.account.e164,
          ZonaRosaStore.account.deviceId,
          ZonaRosaStore.account.servicePassword
        )
      ),
      BuildConfig.ZONAROSA_AGENT,
      null,
      "",
      Stories.isFeatureEnabled()
    )

    try {
      val startTime = System.currentTimeMillis()

      val state: WebSocketConnectionState = uncensoredWebsocket.connect()
        .filter { it == WebSocketConnectionState.CONNECTED || it == WebSocketConnectionState.FAILED }
        .timeout(30, TimeUnit.SECONDS)
        .blockingFirst(WebSocketConnectionState.FAILED)

      if (state == WebSocketConnectionState.CONNECTED) {
        Log.i(TAG, "Established connection in ${System.currentTimeMillis() - startTime} ms! Service is reachable!")
        ZonaRosaStore.misc.isServiceReachableWithoutCircumvention = true
      } else {
        Log.w(TAG, "Failed to establish a connection in ${System.currentTimeMillis() - startTime} ms.")
        ZonaRosaStore.misc.isServiceReachableWithoutCircumvention = false
      }
    } catch (exception: Exception) {
      Log.w(TAG, "Failed to connect to the websocket.", exception)
      ZonaRosaStore.misc.isServiceReachableWithoutCircumvention = false
    } finally {
      uncensoredWebsocket.disconnect()
    }
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return false
  }

  override fun onFailure() {
  }

  class Factory : Job.Factory<CheckServiceReachabilityJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): CheckServiceReachabilityJob {
      return CheckServiceReachabilityJob(parameters)
    }
  }
}

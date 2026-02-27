package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.pin.SvrRepository
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Refresh KBS authentication credentials for talking to KBS during re-registration.
 */
class RefreshSvrCredentialsJob private constructor(parameters: Parameters) : BaseJob(parameters) {

  companion object {
    const val KEY = "RefreshKbsCredentialsJob"

    private val TAG = Log.tag(RefreshSvrCredentialsJob::class.java)
    private val FREQUENCY: Duration = 15.days

    @JvmStatic
    fun enqueueIfNecessary() {
      if (ZonaRosaStore.svr.hasPin() && ZonaRosaStore.account.isRegistered) {
        val lastTimestamp = ZonaRosaStore.svr.lastRefreshAuthTimestamp
        if (lastTimestamp + FREQUENCY.inWholeMilliseconds < System.currentTimeMillis() || lastTimestamp > System.currentTimeMillis()) {
          AppDependencies.jobManager.add(RefreshSvrCredentialsJob())
        } else {
          Log.d(TAG, "Do not need to refresh credentials. Last refresh: $lastTimestamp")
        }
      }
    }
  }

  private constructor() : this(
    parameters = Parameters.Builder()
      .setQueue("RefreshKbsCredentials")
      .addConstraint(NetworkConstraint.KEY)
      .setMaxInstancesForQueue(2)
      .setMaxAttempts(3)
      .setLifespan(1.days.inWholeMilliseconds)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onRun() {
    if (!ZonaRosaStore.account.isRegistered) {
      Log.w(TAG, "Not registered! Skipping.")
      return
    }

    SvrRepository.refreshAndStoreAuthorization()
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return e is IOException && e !is NonSuccessfulResponseCodeException
  }

  override fun onFailure() = Unit

  class Factory : Job.Factory<RefreshSvrCredentialsJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): RefreshSvrCredentialsJob {
      return RefreshSvrCredentialsJob(parameters)
    }
  }
}

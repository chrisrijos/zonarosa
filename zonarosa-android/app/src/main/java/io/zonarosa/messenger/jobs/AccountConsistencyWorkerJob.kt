package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.util.ProfileUtil
import io.zonarosa.service.api.profiles.ZonaRosaServiceProfile
import java.io.IOException
import kotlin.time.Duration.Companion.days

/**
 * The worker job for [io.zonarosa.messenger.migrations.AccountConsistencyMigrationJob].
 */
class AccountConsistencyWorkerJob private constructor(parameters: Parameters) : BaseJob(parameters) {

  companion object {
    private val TAG = Log.tag(AccountConsistencyWorkerJob::class.java)

    const val KEY = "AccountConsistencyWorkerJob"

    @JvmStatic
    fun enqueueIfNecessary() {
      if (ZonaRosaStore.account.isPrimaryDevice && System.currentTimeMillis() - ZonaRosaStore.misc.lastConsistencyCheckTime > 3.days.inWholeMilliseconds) {
        AppDependencies.jobManager.add(AccountConsistencyWorkerJob())
      }
    }
  }

  constructor() : this(
    Parameters.Builder()
      .setMaxInstancesForFactory(1)
      .addConstraint(NetworkConstraint.KEY)
      .setMaxAttempts(Parameters.UNLIMITED)
      .setLifespan(30.days.inWholeMilliseconds)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  override fun onFailure() = Unit

  override fun onRun() {
    if (!ZonaRosaStore.account.hasAciIdentityKey()) {
      Log.i(TAG, "No identity set yet, skipping.")
      return
    }

    if (!ZonaRosaStore.account.isRegistered || ZonaRosaStore.account.aci == null) {
      Log.i(TAG, "Not yet registered, skipping.")
      return
    }

    if (ZonaRosaStore.account.isLinkedDevice) {
      Log.i(TAG, "Linked device, skipping.")
      return
    }

    val aciProfile: ZonaRosaServiceProfile = ProfileUtil.retrieveProfileSync(context, Recipient.self(), ZonaRosaServiceProfile.RequestType.PROFILE, false).profile
    val encodedAciPublicKey = Base64.encodeWithPadding(ZonaRosaStore.account.aciIdentityKey.publicKey.serialize())

    if (aciProfile.identityKey != encodedAciPublicKey) {
      Log.w(TAG, "ACI identity key on profile differed from the one we have locally! Marking ourselves unregistered.")

      ZonaRosaStore.account.setRegistered(false)
      ZonaRosaStore.registration.clearRegistrationComplete()
      ZonaRosaStore.registration.hasUploadedProfile = false

      ZonaRosaStore.misc.lastConsistencyCheckTime = System.currentTimeMillis()
      return
    }

    val pniProfile: ZonaRosaServiceProfile = ProfileUtil.retrieveProfileSync(ZonaRosaStore.account.pni!!, ZonaRosaServiceProfile.RequestType.PROFILE).profile
    val encodedPniPublicKey = Base64.encodeWithPadding(ZonaRosaStore.account.pniIdentityKey.publicKey.serialize())

    if (pniProfile.identityKey != encodedPniPublicKey) {
      Log.w(TAG, "PNI identity key on profile differed from the one we have locally!")

      ZonaRosaStore.account.setRegistered(false)
      ZonaRosaStore.registration.clearRegistrationComplete()
      ZonaRosaStore.registration.hasUploadedProfile = false
      return
    }

    Log.i(TAG, "Everything matched.")

    ZonaRosaStore.misc.lastConsistencyCheckTime = System.currentTimeMillis()
  }

  override fun onShouldRetry(e: Exception): Boolean {
    return e is IOException
  }

  class Factory : Job.Factory<AccountConsistencyWorkerJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AccountConsistencyWorkerJob {
      return AccountConsistencyWorkerJob(parameters)
    }
  }
}

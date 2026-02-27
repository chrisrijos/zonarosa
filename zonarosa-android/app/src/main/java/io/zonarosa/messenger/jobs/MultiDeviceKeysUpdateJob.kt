package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.NotPushRegisteredException
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.service.api.crypto.UntrustedIdentityException
import io.zonarosa.service.api.messages.multidevice.KeysMessage
import io.zonarosa.service.api.messages.multidevice.ZonaRosaServiceSyncMessage
import io.zonarosa.service.api.push.exceptions.PushNetworkException
import io.zonarosa.service.api.push.exceptions.ServerRejectedException
import java.io.IOException

class MultiDeviceKeysUpdateJob private constructor(parameters: Parameters) : BaseJob(parameters) {

  companion object {
    const val KEY: String = "MultiDeviceKeysUpdateJob"

    private val TAG = Log.tag(MultiDeviceKeysUpdateJob::class.java)
  }

  constructor() : this(
    Parameters.Builder()
      .setQueue("MultiDeviceKeysUpdateJob")
      .setMaxInstancesForFactory(2)
      .addConstraint(NetworkConstraint.KEY)
      .setMaxAttempts(10)
      .build()
  )

  override fun serialize(): ByteArray? = null

  override fun getFactoryKey(): String = KEY

  @Throws(IOException::class, UntrustedIdentityException::class)
  public override fun onRun() {
    if (!Recipient.self().isRegistered) {
      throw NotPushRegisteredException()
    }

    if (!ZonaRosaStore.account.isMultiDevice) {
      Log.i(TAG, "Not multi device, aborting...")
      return
    }

    if (ZonaRosaStore.account.isLinkedDevice) {
      Log.i(TAG, "Not primary device, aborting...")
      return
    }

    val syncMessage = ZonaRosaServiceSyncMessage.forKeys(
      KeysMessage(
        storageService = ZonaRosaStore.storageService.storageKey,
        master = ZonaRosaStore.svr.masterKey,
        accountEntropyPool = ZonaRosaStore.account.accountEntropyPool,
        mediaRootBackupKey = ZonaRosaStore.backup.mediaRootBackupKey
      )
    )

    AppDependencies.zonarosaServiceMessageSender.sendSyncMessage(syncMessage)
  }

  public override fun onShouldRetry(e: Exception): Boolean {
    if (e is ServerRejectedException) return false
    return e is PushNetworkException
  }

  override fun onFailure() {
  }

  class Factory : Job.Factory<MultiDeviceKeysUpdateJob?> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): MultiDeviceKeysUpdateJob {
      return MultiDeviceKeysUpdateJob(parameters)
    }
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.changenumber

import androidx.annotation.WorkerThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.ZonaRosaProtocolAddress
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.libzonarosa.protocol.state.ZonaRosaProtocolStore
import io.zonarosa.libzonarosa.protocol.state.SignedPreKeyRecord
import io.zonarosa.libzonarosa.protocol.util.KeyHelper
import io.zonarosa.libzonarosa.protocol.util.Medium
import io.zonarosa.messenger.crypto.PreKeyUtil
import io.zonarosa.messenger.database.IdentityTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.databaseprotos.PendingChangeNumberMetadata
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.impl.BackoffUtil
import io.zonarosa.messenger.jobs.RefreshAttributesJob
import io.zonarosa.messenger.keyvalue.CertificateType
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.pin.SvrRepository
import io.zonarosa.messenger.pin.SvrWrongPinException
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.registration.viewmodel.SvrAuthCredentialSet
import io.zonarosa.messenger.storage.StorageSyncHelper
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.ZonaRosaServiceAccountManager
import io.zonarosa.service.api.ZonaRosaServiceMessageSender
import io.zonarosa.service.api.SvrNoDataException
import io.zonarosa.service.api.account.ChangePhoneNumberRequest
import io.zonarosa.service.api.account.PreKeyUpload
import io.zonarosa.service.api.push.ServiceIdType
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import io.zonarosa.service.api.push.SignedPreKeyEntity
import io.zonarosa.service.internal.push.KyberPreKeyEntity
import io.zonarosa.service.internal.push.MismatchedDevices
import io.zonarosa.service.internal.push.OutgoingPushMessage
import io.zonarosa.service.internal.push.SyncMessage
import io.zonarosa.service.internal.push.VerifyAccountResponse
import io.zonarosa.service.internal.push.WhoAmIResponse
import java.io.IOException
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Repository to perform data operations during change number.
 *
 * @see [io.zonarosa.messenger.registration.data.RegistrationRepository]
 */
class ChangeNumberRepository(
  private val accountManager: ZonaRosaServiceAccountManager = AppDependencies.zonarosaServiceAccountManager,
  private val messageSender: ZonaRosaServiceMessageSender = AppDependencies.zonarosaServiceMessageSender
) {

  companion object {
    private val TAG = Log.tag(ChangeNumberRepository::class.java)
  }

  fun whoAmI(): WhoAmIResponse {
    return accountManager.whoAmI
  }

  suspend fun ensureDecryptionsDrained(timeout: Duration = 15.seconds) = withTimeoutOrNull(timeout) {
    suspendCancellableCoroutine {
      val drainedListener = object : Runnable {
        override fun run() {
          AppDependencies
            .incomingMessageObserver
            .removeDecryptionDrainedListener(this)
          Log.d(TAG, "Decryptions drained.")
          it.resume(true)
        }
      }

      it.invokeOnCancellation { cancellationCause ->
        AppDependencies
          .incomingMessageObserver
          .removeDecryptionDrainedListener(drainedListener)
        Log.d(TAG, "Decryptions draining canceled.", cancellationCause)
      }

      AppDependencies
        .incomingMessageObserver
        .addDecryptionDrainedListener(drainedListener)
      Log.d(TAG, "Waiting for decryption drain.")
    }
  }

  @WorkerThread
  fun changeLocalNumber(e164: String, pni: ServiceId.PNI) {
    ZonaRosaDatabase.recipients.updateSelfE164(e164, pni)
    AppDependencies.recipientCache.clear()

    if (e164 != ZonaRosaStore.account.requireE164()) {
      ZonaRosaDatabase.recipients.rotateStorageId(Recipient.self().fresh().id)
      StorageSyncHelper.scheduleSyncForDataChange()
    }

    ZonaRosaStore.account.setE164(e164)
    ZonaRosaStore.account.setPni(pni)
    AppDependencies.resetProtocolStores()

    AppDependencies.groupsV2Authorization.clear()

    val metadata: PendingChangeNumberMetadata? = ZonaRosaStore.misc.pendingChangeNumberMetadata
    if (metadata == null) {
      Log.w(TAG, "No change number metadata, this shouldn't happen")
      throw AssertionError("No change number metadata")
    }

    val pniIdentityKeyPair = IdentityKeyPair(metadata.pniIdentityKeyPair.toByteArray())
    val pniRegistrationId = metadata.pniRegistrationId
    val pniSignedPreyKeyId = metadata.pniSignedPreKeyId
    val pniLastResortKyberPreKeyId = metadata.pniLastResortKyberPreKeyId

    val pniProtocolStore = AppDependencies.protocolStore.pni()
    val pniMetadataStore = ZonaRosaStore.account.pniPreKeys

    ZonaRosaStore.account.pniRegistrationId = pniRegistrationId
    ZonaRosaStore.account.setPniIdentityKeyAfterChangeNumber(pniIdentityKeyPair)

    val signedPreKey = pniProtocolStore.loadSignedPreKey(pniSignedPreyKeyId)
    val oneTimeEcPreKeys = PreKeyUtil.generateAndStoreOneTimeEcPreKeys(pniProtocolStore, pniMetadataStore)
    val lastResortKyberPreKey = pniProtocolStore.loadLastResortKyberPreKeys().firstOrNull { it.id == pniLastResortKyberPreKeyId }
    val oneTimeKyberPreKeys = PreKeyUtil.generateAndStoreOneTimeKyberPreKeys(pniProtocolStore, pniMetadataStore)

    if (lastResortKyberPreKey == null) {
      Log.w(TAG, "Last-resort kyber prekey is missing!")
    }

    pniMetadataStore.activeSignedPreKeyId = signedPreKey.id
    Log.i(TAG, "Submitting prekeys with PNI identity key: ${pniIdentityKeyPair.publicKey.fingerprint}")

    retryChangeLocalNumberNetworkOperation {
      ZonaRosaNetwork.keys.setPreKeys(
        PreKeyUpload(
          serviceIdType = ServiceIdType.PNI,
          signedPreKey = signedPreKey,
          oneTimeEcPreKeys = oneTimeEcPreKeys,
          lastResortKyberPreKey = lastResortKyberPreKey,
          oneTimeKyberPreKeys = oneTimeKyberPreKeys
        )
      )
    }.successOrThrow()

    pniMetadataStore.isSignedPreKeyRegistered = true
    pniMetadataStore.lastResortKyberPreKeyId = pniLastResortKyberPreKeyId

    pniProtocolStore.identities().saveIdentityWithoutSideEffects(
      Recipient.self().id,
      pni,
      pniProtocolStore.identityKeyPair.publicKey,
      IdentityTable.VerifiedStatus.VERIFIED,
      true,
      System.currentTimeMillis(),
      true
    )

    ZonaRosaStore.misc.hasPniInitializedDevices = true
    AppDependencies.groupsV2Authorization.clear()

    Recipient.self().fresh()
    StorageSyncHelper.scheduleSyncForDataChange()

    AppDependencies.resetNetwork()
    AppDependencies.startNetwork()

    AppDependencies.jobManager.add(RefreshAttributesJob())

    rotateCertificates()

    ZonaRosaStore.misc.unlockChangeNumber()
  }

  @WorkerThread
  private fun rotateCertificates() {
    val certificateTypes = ZonaRosaStore.phoneNumberPrivacy.allCertificateTypes

    Log.i(TAG, "Rotating these certificates $certificateTypes")

    for (certificateType in certificateTypes) {
      val certificate: ByteArray? = when (certificateType) {
        CertificateType.ACI_AND_E164 -> retryChangeLocalNumberNetworkOperation { ZonaRosaNetwork.certificate.getSenderCertificate() }.successOrThrow()
        CertificateType.ACI_ONLY -> retryChangeLocalNumberNetworkOperation { ZonaRosaNetwork.certificate.getSenderCertificateForPhoneNumberPrivacy() }.successOrThrow()
        else -> throw AssertionError()
      }

      Log.i(TAG, "Successfully got $certificateType certificate")

      ZonaRosaStore.certificate.setUnidentifiedAccessCertificate(certificateType, certificate)
    }
  }

  private fun <T> retryChangeLocalNumberNetworkOperation(operation: () -> NetworkResult<T>): NetworkResult<T> {
    var tries = 0
    var result = operation()
    while (tries < 5) {
      when (result) {
        is NetworkResult.Success,
        is NetworkResult.ApplicationError -> return result
        is NetworkResult.StatusCodeError,
        is NetworkResult.NetworkError -> Log.w(TAG, "Network related error attempting change number operation, try: $tries", result.getCause())
      }

      tries++
      BackoffUtil.exponentialBackoff(tries, 10.seconds.inWholeMilliseconds)
      result = operation()
    }

    return result
  }

  suspend fun changeNumberWithRecoveryPassword(recoveryPassword: String, newE164: String): ChangeNumberResult {
    return changeNumberInternal(recoveryPassword = recoveryPassword, newE164 = newE164)
  }

  suspend fun changeNumberWithoutRegistrationLock(sessionId: String, newE164: String): ChangeNumberResult {
    return changeNumberInternal(sessionId = sessionId, newE164 = newE164)
  }

  suspend fun changeNumberWithRegistrationLock(
    sessionId: String,
    newE164: String,
    pin: String,
    svrAuthCredentials: SvrAuthCredentialSet
  ): ChangeNumberResult {
    val masterKey: MasterKey

    try {
      masterKey = SvrRepository.restoreMasterKeyPreRegistration(svrAuthCredentials, pin)
    } catch (e: SvrWrongPinException) {
      return ChangeNumberResult.SvrWrongPin(e)
    } catch (e: SvrNoDataException) {
      return ChangeNumberResult.SvrNoData(e)
    } catch (e: IOException) {
      return ChangeNumberResult.UnknownError(e)
    }

    val registrationLock = masterKey.deriveRegistrationLock()
    return changeNumberInternal(sessionId = sessionId, registrationLock = registrationLock, newE164 = newE164)
  }

  /**
   * Sends a request to the service to change the phone number associated with this account.
   */
  private suspend fun changeNumberInternal(sessionId: String? = null, recoveryPassword: String? = null, registrationLock: String? = null, newE164: String): ChangeNumberResult {
    check((sessionId != null && recoveryPassword == null) || (sessionId == null && recoveryPassword != null))
    var completed = false
    var attempts = 0
    lateinit var result: NetworkResult<VerifyAccountResponse>

    while (!completed && attempts < 5) {
      Log.i(TAG, "Attempt #$attempts")
      val (request: ChangePhoneNumberRequest, metadata: PendingChangeNumberMetadata) = createChangeNumberRequest(
        sessionId = sessionId,
        recoveryPassword = recoveryPassword,
        newE164 = newE164,
        registrationLock = registrationLock
      )

      ZonaRosaStore.misc.setPendingChangeNumberMetadata(metadata)
      ZonaRosaStore.misc.lockChangeNumber()
      withContext(Dispatchers.IO) {
        result = ZonaRosaNetwork.account.changeNumber(request)
      }

      if (result is NetworkResult.StatusCodeError && result.code == 409) {
        val mismatchedDevices: MismatchedDevices? = result.parseJsonBody()
        if (mismatchedDevices != null) {
          messageSender.handleChangeNumberMismatchDevices(mismatchedDevices)
        }
        attempts++
      } else {
        completed = true
      }
    }

    if (result is NetworkResult.StatusCodeError) {
      ZonaRosaStore.misc.unlockChangeNumber()
    }

    Log.i(TAG, "Returning change number network result.")
    return ChangeNumberResult.from(
      result.map { accountRegistrationResponse: VerifyAccountResponse ->
        NumberChangeResult(
          uuid = accountRegistrationResponse.uuid,
          pni = accountRegistrationResponse.pni,
          storageCapable = accountRegistrationResponse.storageCapable,
          number = accountRegistrationResponse.number
        )
      }
    )
  }

  @WorkerThread
  private fun createChangeNumberRequest(
    sessionId: String? = null,
    recoveryPassword: String? = null,
    newE164: String,
    registrationLock: String? = null
  ): ChangeNumberRequestData {
    val selfIdentifier: String = ZonaRosaStore.account.requireAci().toString()
    val aciProtocolStore: ZonaRosaProtocolStore = AppDependencies.protocolStore.aci()

    val pniIdentity: IdentityKeyPair = IdentityKeyPair.generate()
    val deviceMessages = mutableListOf<OutgoingPushMessage>()
    val devicePniSignedPreKeys = mutableMapOf<Int, SignedPreKeyEntity>()
    val devicePniLastResortKyberPreKeys = mutableMapOf<Int, KyberPreKeyEntity>()
    val pniRegistrationIds = mutableMapOf<Int, Int>()
    val primaryDeviceId: Int = ZonaRosaServiceAddress.DEFAULT_DEVICE_ID

    val devices: List<Int> = listOf(primaryDeviceId) + aciProtocolStore.getSubDeviceSessions(selfIdentifier)

    devices
      .filter { it == primaryDeviceId || aciProtocolStore.containsSession(ZonaRosaProtocolAddress(selfIdentifier, it)) }
      .forEach { deviceId ->
        // Signed Prekeys
        val signedPreKeyRecord: SignedPreKeyRecord = if (deviceId == primaryDeviceId) {
          PreKeyUtil.generateAndStoreSignedPreKey(AppDependencies.protocolStore.pni(), ZonaRosaStore.account.pniPreKeys, pniIdentity.privateKey)
        } else {
          PreKeyUtil.generateSignedPreKey(SecureRandom().nextInt(Medium.MAX_VALUE), pniIdentity.privateKey)
        }
        devicePniSignedPreKeys[deviceId] = SignedPreKeyEntity(signedPreKeyRecord.id, signedPreKeyRecord.keyPair.publicKey, signedPreKeyRecord.signature)

        // Last-resort kyber prekeys
        val lastResortKyberPreKeyRecord: KyberPreKeyRecord = if (deviceId == primaryDeviceId) {
          PreKeyUtil.generateAndStoreLastResortKyberPreKey(AppDependencies.protocolStore.pni(), ZonaRosaStore.account.pniPreKeys, pniIdentity.privateKey)
        } else {
          PreKeyUtil.generateLastResortKyberPreKey(SecureRandom().nextInt(Medium.MAX_VALUE), pniIdentity.privateKey)
        }
        devicePniLastResortKyberPreKeys[deviceId] = KyberPreKeyEntity(lastResortKyberPreKeyRecord.id, lastResortKyberPreKeyRecord.keyPair.publicKey, lastResortKyberPreKeyRecord.signature)

        // Registration Ids
        var pniRegistrationId = -1

        while (pniRegistrationId < 0 || pniRegistrationIds.values.contains(pniRegistrationId)) {
          pniRegistrationId = KeyHelper.generateRegistrationId(false)
        }
        pniRegistrationIds[deviceId] = pniRegistrationId

        // Device Messages
        if (deviceId != primaryDeviceId) {
          val pniChangeNumber = SyncMessage.PniChangeNumber(
            identityKeyPair = pniIdentity.serialize().toByteString(),
            signedPreKey = signedPreKeyRecord.serialize().toByteString(),
            lastResortKyberPreKey = lastResortKyberPreKeyRecord.serialize().toByteString(),
            registrationId = pniRegistrationId,
            newE164 = newE164
          )

          deviceMessages += messageSender.getEncryptedSyncPniInitializeDeviceMessage(deviceId, pniChangeNumber)
        }
      }

    val request = ChangePhoneNumberRequest(
      sessionId,
      recoveryPassword,
      newE164,
      registrationLock,
      pniIdentity.publicKey,
      deviceMessages,
      devicePniSignedPreKeys.mapKeys { it.key.toString() },
      devicePniLastResortKyberPreKeys.mapKeys { it.key.toString() },
      pniRegistrationIds.mapKeys { it.key.toString() }
    )

    val metadata = PendingChangeNumberMetadata(
      previousPni = ZonaRosaStore.account.pni!!.toByteString(),
      pniIdentityKeyPair = pniIdentity.serialize().toByteString(),
      pniRegistrationId = pniRegistrationIds[primaryDeviceId]!!,
      pniSignedPreKeyId = devicePniSignedPreKeys[primaryDeviceId]!!.keyId,
      pniLastResortKyberPreKeyId = devicePniLastResortKyberPreKeys[primaryDeviceId]!!.keyId,
      previousE164 = ZonaRosaStore.account.requireE164(),
      newE164 = newE164
    )

    return ChangeNumberRequestData(request, metadata)
  }

  private data class ChangeNumberRequestData(val changeNumberRequest: ChangePhoneNumberRequest, val pendingChangeNumberMetadata: PendingChangeNumberMetadata)

  data class NumberChangeResult(
    val uuid: String,
    val pni: String,
    val storageCapable: Boolean,
    val number: String
  )
}

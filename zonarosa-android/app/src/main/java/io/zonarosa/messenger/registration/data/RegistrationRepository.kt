/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data

import android.app.backup.BackupManager
import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationManagerCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.ServiceId.PNI
import io.zonarosa.core.models.backup.MediaRootBackupKey
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.protocol.IdentityKeyPair
import io.zonarosa.libzonarosa.protocol.util.KeyHelper
import io.zonarosa.libzonarosa.zkgroup.profiles.ProfileKey
import io.zonarosa.messenger.AppCapabilities
import io.zonarosa.messenger.crypto.PreKeyUtil
import io.zonarosa.messenger.crypto.ProfileKeyUtil
import io.zonarosa.messenger.crypto.SenderKeyUtil
import io.zonarosa.messenger.crypto.storage.PreKeyMetadataStore
import io.zonarosa.messenger.crypto.storage.ZonaRosaServiceAccountDataStoreImpl
import io.zonarosa.messenger.database.IdentityTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.databaseprotos.LocalRegistrationMetadata
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.gcm.FcmUtil
import io.zonarosa.messenger.jobmanager.runJobBlocking
import io.zonarosa.messenger.jobs.CheckKeyTransparencyJob
import io.zonarosa.messenger.jobs.DirectoryRefreshJob
import io.zonarosa.messenger.jobs.PreKeysSyncJob
import io.zonarosa.messenger.jobs.RefreshOwnProfileJob
import io.zonarosa.messenger.jobs.RotateCertificateJob
import io.zonarosa.messenger.keyvalue.PhoneNumberPrivacyValues
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.net.ZonaRosaNetwork
import io.zonarosa.messenger.notifications.NotificationIds
import io.zonarosa.messenger.pin.Svr3Migration
import io.zonarosa.messenger.pin.SvrRepository
import io.zonarosa.messenger.pin.SvrWrongPinException
import io.zonarosa.messenger.profiles.AvatarHelper
import io.zonarosa.messenger.push.AccountManagerFactory
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.registration.data.LocalRegistrationMetadataUtil.getAciIdentityKeyPair
import io.zonarosa.messenger.registration.data.LocalRegistrationMetadataUtil.getAciPreKeyCollection
import io.zonarosa.messenger.registration.data.LocalRegistrationMetadataUtil.getPniIdentityKeyPair
import io.zonarosa.messenger.registration.data.LocalRegistrationMetadataUtil.getPniPreKeyCollection
import io.zonarosa.messenger.registration.data.network.BackupAuthCheckResult
import io.zonarosa.messenger.registration.data.network.RegisterAccountResult
import io.zonarosa.messenger.registration.data.network.RegistrationSessionCheckResult
import io.zonarosa.messenger.registration.data.network.RegistrationSessionCreationResult
import io.zonarosa.messenger.registration.data.network.RegistrationSessionResult
import io.zonarosa.messenger.registration.data.network.VerificationCodeRequestResult
import io.zonarosa.messenger.registration.fcm.PushChallengeRequest
import io.zonarosa.messenger.registration.secondary.DeviceNameCipher
import io.zonarosa.messenger.registration.viewmodel.SvrAuthCredentialSet
import io.zonarosa.messenger.service.DirectoryRefreshListener
import io.zonarosa.messenger.service.RotateSignedPreKeyListener
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.SvrNoDataException
import io.zonarosa.service.api.account.AccountAttributes
import io.zonarosa.service.api.account.PreKeyCollection
import io.zonarosa.service.api.crypto.UnidentifiedAccess
import io.zonarosa.service.api.kbs.PinHashUtil
import io.zonarosa.service.api.link.TransferArchiveResponse
import io.zonarosa.service.api.push.ZonaRosaServiceAddress
import io.zonarosa.service.api.registration.RegistrationApi
import io.zonarosa.service.api.svr.Svr3Credentials
import io.zonarosa.service.internal.push.AuthCredentials
import io.zonarosa.service.internal.push.ProvisionMessage
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.push.RegistrationSessionMetadataResponse
import io.zonarosa.service.internal.push.VerifyAccountResponse
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Optional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A repository that deals with disk I/O during account registration.
 */
object RegistrationRepository {

  private val TAG = Log.tag(RegistrationRepository::class.java)

  private val PUSH_REQUEST_TIMEOUT = 5.seconds.inWholeMilliseconds

  /**
   * Retrieve the FCM token from the Firebase service.
   */
  suspend fun getFcmToken(context: Context): String? = withContext(Dispatchers.Default) {
    FcmUtil.getToken(context).orElse(null)
  }

  /**
   * Queries, and creates if needed, the local registration ID.
   */
  @JvmStatic
  fun getRegistrationId(): Int {
    // TODO [regv2]: make creation more explicit instead of hiding it in this getter
    var registrationId = ZonaRosaStore.account.registrationId
    if (registrationId == 0) {
      registrationId = KeyHelper.generateRegistrationId(false)
      ZonaRosaStore.account.registrationId = registrationId
    }
    return registrationId
  }

  /**
   * Queries, and creates if needed, the local PNI registration ID.
   */
  @JvmStatic
  fun getPniRegistrationId(): Int {
    // TODO [regv2]: make creation more explicit instead of hiding it in this getter
    var pniRegistrationId = ZonaRosaStore.account.pniRegistrationId
    if (pniRegistrationId == 0) {
      pniRegistrationId = KeyHelper.generateRegistrationId(false)
      ZonaRosaStore.account.pniRegistrationId = pniRegistrationId
    }
    return pniRegistrationId
  }

  /**
   * Queries, and creates if needed, the local profile key.
   */
  @JvmStatic
  suspend fun getProfileKey(e164: String): ProfileKey = withContext(Dispatchers.IO) {
    // TODO [regv2]: make creation more explicit instead of hiding it in this getter
    val recipientTable = ZonaRosaDatabase.recipients
    val recipient = recipientTable.getByE164(e164)
    var profileKey = if (recipient.isPresent) {
      ProfileKeyUtil.profileKeyOrNull(Recipient.resolved(recipient.get()).profileKey)
    } else {
      null
    }
    if (profileKey == null) {
      profileKey = ProfileKeyUtil.createNew()
      Log.i(TAG, "No profile key found, created a new one")
    }
    profileKey
  }

  /**
   * Takes a server response from a successful registration and persists the relevant data.
   */
  @JvmStatic
  suspend fun registerAccountLocally(context: Context, data: LocalRegistrationMetadata) = withContext(Dispatchers.IO) {
    Log.v(TAG, "registerAccountLocally()")
    if (data.linkedDeviceInfo != null) {
      ZonaRosaStore.account.deviceId = data.linkedDeviceInfo.deviceId
      ZonaRosaStore.account.deviceName = data.linkedDeviceInfo.deviceName
    }

    val aciIdentityKeyPair = data.getAciIdentityKeyPair()
    val pniIdentityKeyPair = data.getPniIdentityKeyPair()
    ZonaRosaStore.account.restoreAciIdentityKeyFromBackup(aciIdentityKeyPair.publicKey.serialize(), aciIdentityKeyPair.privateKey.serialize())
    ZonaRosaStore.account.restorePniIdentityKeyFromBackup(pniIdentityKeyPair.publicKey.serialize(), pniIdentityKeyPair.privateKey.serialize())

    val aciPreKeyCollection = data.getAciPreKeyCollection()
    val pniPreKeyCollection = data.getPniPreKeyCollection()
    val aci: ACI = ACI.parseOrThrow(data.aci)
    val pni: PNI = PNI.parseOrThrow(data.pni)
    val hasPin: Boolean = data.hasPin

    ZonaRosaStore.account.setAci(aci)
    ZonaRosaStore.account.setPni(pni)

    AppDependencies.resetProtocolStores()

    AppDependencies.protocolStore.aci().sessions().archiveAllSessions()
    AppDependencies.protocolStore.pni().sessions().archiveAllSessions()
    SenderKeyUtil.clearAllState()

    val aciProtocolStore = AppDependencies.protocolStore.aci()
    val aciMetadataStore = ZonaRosaStore.account.aciPreKeys

    val pniProtocolStore = AppDependencies.protocolStore.pni()
    val pniMetadataStore = ZonaRosaStore.account.pniPreKeys

    storeSignedAndLastResortPreKeys(aciProtocolStore, aciMetadataStore, aciPreKeyCollection)
    storeSignedAndLastResortPreKeys(pniProtocolStore, pniMetadataStore, pniPreKeyCollection)

    val recipientTable = ZonaRosaDatabase.recipients
    val selfId = Recipient.trustedPush(aci, pni, data.e164).id

    recipientTable.setProfileSharing(selfId, true)
    recipientTable.markRegisteredOrThrow(selfId, aci)
    recipientTable.linkIdsForSelf(aci, pni, data.e164)
    recipientTable.setProfileKey(selfId, ProfileKey(data.profileKey.toByteArray()))

    AppDependencies.recipientCache.clearSelf()

    ZonaRosaStore.account.setE164(data.e164)
    ZonaRosaStore.account.fcmToken = data.fcmToken
    ZonaRosaStore.account.fcmEnabled = data.fcmEnabled

    val now = System.currentTimeMillis()
    saveOwnIdentityKey(selfId, aci, aciProtocolStore, now)
    saveOwnIdentityKey(selfId, pni, pniProtocolStore, now)

    if (data.linkedDeviceInfo != null) {
      if (data.linkedDeviceInfo.accountEntropyPool != null) {
        ZonaRosaStore.account.setAccountEntropyPoolFromPrimaryDevice(AccountEntropyPool(data.linkedDeviceInfo.accountEntropyPool))
      }

      if (data.linkedDeviceInfo.mediaRootBackupKey != null) {
        ZonaRosaStore.backup.mediaRootBackupKey = MediaRootBackupKey(data.linkedDeviceInfo.mediaRootBackupKey.toByteArray())
      }
    }

    ZonaRosaStore.account.setServicePassword(data.servicePassword)
    ZonaRosaStore.account.setRegistered(true)
    ZonaRosaPreferences.setPromptedPushRegistration(context, true)
    ZonaRosaPreferences.setUnauthorizedReceived(context, false)
    NotificationManagerCompat.from(context).cancel(NotificationIds.UNREGISTERED_NOTIFICATION_ID)

    val masterKey = if (data.masterKey != null) MasterKey(data.masterKey.toByteArray()) else null
    SvrRepository.onRegistrationComplete(masterKey, data.pin, hasPin, data.reglockEnabled, ZonaRosaStore.account.restoredAccountEntropyPool)

    AppDependencies.resetNetwork()
    AppDependencies.startNetwork()
    PreKeysSyncJob.enqueue()

    recipientTable.clearSelfKeyTransparencyData()
    CheckKeyTransparencyJob.enqueueIfNecessary(addDelay = true)

    val jobManager = AppDependencies.jobManager

    if (data.linkedDeviceInfo == null) {
      jobManager.add(DirectoryRefreshJob(false))
      jobManager.add(RotateCertificateJob())

      DirectoryRefreshListener.schedule(context)
      RotateSignedPreKeyListener.schedule(context)
    } else {
      ZonaRosaStore.account.isMultiDevice = true
      jobManager.runJobBlocking(RefreshOwnProfileJob(), 30.seconds)

      jobManager.add(RotateCertificateJob())
      RotateSignedPreKeyListener.schedule(context)
    }
  }

  @JvmStatic
  private fun saveOwnIdentityKey(selfId: RecipientId, serviceId: ServiceId, protocolStore: ZonaRosaServiceAccountDataStoreImpl, now: Long) {
    protocolStore.identities().saveIdentityWithoutSideEffects(
      selfId,
      serviceId,
      protocolStore.identityKeyPair.publicKey,
      IdentityTable.VerifiedStatus.VERIFIED,
      true,
      now,
      true
    )
  }

  @JvmStatic
  private fun storeSignedAndLastResortPreKeys(protocolStore: ZonaRosaServiceAccountDataStoreImpl, metadataStore: PreKeyMetadataStore, preKeyCollection: PreKeyCollection) {
    PreKeyUtil.storeSignedPreKey(protocolStore, metadataStore, preKeyCollection.signedPreKey)
    metadataStore.isSignedPreKeyRegistered = true
    metadataStore.activeSignedPreKeyId = preKeyCollection.signedPreKey.id
    metadataStore.lastSignedPreKeyRotationTime = System.currentTimeMillis()

    PreKeyUtil.storeLastResortKyberPreKey(protocolStore, metadataStore, preKeyCollection.lastResortKyberPreKey)
    metadataStore.lastResortKyberPreKeyId = preKeyCollection.lastResortKyberPreKey.id
    metadataStore.lastResortKyberPreKeyRotationTime = System.currentTimeMillis()
  }

  fun canUseLocalRecoveryPassword(): Boolean {
    val recoveryPassword = ZonaRosaStore.svr.recoveryPassword
    val pinHash = ZonaRosaStore.svr.localPinHash
    return recoveryPassword != null && pinHash != null
  }

  fun doesPinMatchLocalHash(pin: String): Boolean {
    val pinHash = ZonaRosaStore.svr.localPinHash ?: throw IllegalStateException("Local PIN hash is not present!")
    return PinHashUtil.verifyLocalPinHash(pinHash, pin)
  }

  suspend fun fetchMasterKeyFromSvrRemote(pin: String, svr2Credentials: AuthCredentials?, svr3Credentials: Svr3Credentials?): MasterKey = withContext(Dispatchers.IO) {
    val credentialSet = SvrAuthCredentialSet(svr2Credentials = svr2Credentials, svr3Credentials = svr3Credentials)
    val masterKey = SvrRepository.restoreMasterKeyPreRegistration(credentialSet, pin)
    return@withContext masterKey
  }

  /**
   * Validates a session ID.
   */
  private suspend fun validateSession(context: Context, sessionId: String, e164: String, password: String): RegistrationSessionCheckResult = withContext(Dispatchers.IO) {
    val api: RegistrationApi = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, password).registrationApi
    Log.d(TAG, "Validating registration session with service.")
    val registrationSessionResult = api.getRegistrationSessionStatus(sessionId)
    return@withContext RegistrationSessionCheckResult.from(registrationSessionResult)
  }

  /**
   * Initiates a new registration session on the service.
   */
  suspend fun createSession(context: Context, e164: String, password: String, mcc: String?, mnc: String?): RegistrationSessionCreationResult = withContext(Dispatchers.IO) {
    Log.d(TAG, "About to create a registration session…")
    val fcmToken: String? = FcmUtil.getToken(context).orElse(null)
    val api: RegistrationApi = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, password).registrationApi

    val registrationSessionResult = if (fcmToken == null) {
      Log.d(TAG, "Creating registration session without FCM token.")
      api.createRegistrationSession(null, mcc, mnc)
    } else {
      Log.d(TAG, "Creating registration session with FCM token.")
      createSessionAndBlockForPushChallenge(api, fcmToken, mcc, mnc)
    }
    val result = RegistrationSessionCreationResult.from(registrationSessionResult)
    if (result is RegistrationSessionCreationResult.Success) {
      Log.d(TAG, "Updating registration session and E164 in value store.")
      ZonaRosaStore.registration.sessionId = result.sessionId
      ZonaRosaStore.registration.sessionE164 = e164
    }

    return@withContext result
  }

  /**
   * Validates an existing session, if its ID is provided. If the session is expired/invalid, or none is provided, it will attempt to initiate a new session.
   */
  suspend fun createOrValidateSession(context: Context, sessionId: String?, e164: String, password: String, mcc: String?, mnc: String?): RegistrationSessionResult {
    val savedSessionId = if (sessionId == null && e164 == ZonaRosaStore.registration.sessionE164) {
      ZonaRosaStore.registration.sessionId
    } else {
      sessionId
    }

    if (savedSessionId != null) {
      Log.d(TAG, "Validating existing registration session.")
      val sessionValidationResult = validateSession(context, savedSessionId, e164, password)
      when (sessionValidationResult) {
        is RegistrationSessionCheckResult.Success -> {
          Log.d(TAG, "Existing registration session is valid.")
          return sessionValidationResult
        }

        is RegistrationSessionCheckResult.UnknownError -> {
          Log.w(TAG, "Encountered error when validating existing session.", sessionValidationResult.getCause())
          return sessionValidationResult
        }

        is RegistrationSessionCheckResult.SessionNotFound -> {
          Log.i(TAG, "Current session is invalid or has expired. Must create new one.")
          // fall through to creation
        }
      }
    }
    return createSession(context, e164, password, mcc, mnc)
  }

  /**
   * Asks the service to send a verification code through one of our supported channels (SMS, phone call).
   */
  suspend fun requestSmsCode(context: Context, sessionId: String, e164: String, password: String, mode: E164VerificationMode): VerificationCodeRequestResult = withContext(Dispatchers.IO) {
    val api: RegistrationApi = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, password).registrationApi

    val codeRequestResult = api.requestSmsVerificationCode(sessionId, Locale.getDefault(), mode.isSmsRetrieverSupported, mode.transport)

    return@withContext VerificationCodeRequestResult.from(codeRequestResult)
  }

  /**
   * Submits the user-entered verification code to the service.
   */
  suspend fun submitVerificationCode(context: Context, sessionId: String, registrationData: RegistrationData): VerificationCodeRequestResult = withContext(Dispatchers.IO) {
    val api: RegistrationApi = AccountManagerFactory.getInstance().createUnauthenticated(context, registrationData.e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, registrationData.password).registrationApi
    val result = api.verifyAccount(sessionId = sessionId, verificationCode = registrationData.code)
    return@withContext VerificationCodeRequestResult.from(result)
  }

  /**
   * Submits the solved captcha token to the service.
   */
  suspend fun submitCaptchaToken(context: Context, e164: String, password: String, sessionId: String, captchaToken: String): VerificationCodeRequestResult = withContext(Dispatchers.IO) {
    val api: RegistrationApi = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, password).registrationApi
    val captchaSubmissionResult = api.submitCaptchaToken(sessionId = sessionId, captchaToken = captchaToken)
    return@withContext VerificationCodeRequestResult.from(captchaSubmissionResult)
  }

  suspend fun requestAndVerifyPushToken(context: Context, sessionId: String, e164: String, password: String) = withContext(Dispatchers.IO) {
    val fcmToken = getFcmToken(context)
    val accountManager = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, password)
    val pushChallenge = PushChallengeRequest.getPushChallengeBlocking(accountManager, sessionId, Optional.ofNullable(fcmToken), PUSH_REQUEST_TIMEOUT).orElse(null)
    val pushSubmissionResult = accountManager.registrationApi.submitPushChallengeToken(sessionId = sessionId, pushChallengeToken = pushChallenge)
    return@withContext VerificationCodeRequestResult.from(pushSubmissionResult)
  }

  /**
   * Submit the necessary assets as a verified account so that the user can actually use the service.
   */
  suspend fun registerAccount(context: Context, sessionId: String?, registrationData: RegistrationData, pin: String? = null, masterKeyProducer: MasterKeyProducer? = null): RegisterAccountResult = withContext(Dispatchers.IO) {
    Log.v(TAG, "registerAccount()")
    val api: RegistrationApi = AccountManagerFactory.getInstance().createUnauthenticated(context, registrationData.e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, registrationData.password).registrationApi

    val universalUnidentifiedAccess: Boolean = ZonaRosaPreferences.isUniversalUnidentifiedAccess(context)
    val unidentifiedAccessKey: ByteArray = UnidentifiedAccess.deriveAccessKeyFrom(registrationData.profileKey)

    val masterKey: MasterKey?
    try {
      masterKey = masterKeyProducer?.produceMasterKey()
    } catch (e: SvrNoDataException) {
      return@withContext RegisterAccountResult.SvrNoData(e)
    } catch (e: SvrWrongPinException) {
      return@withContext RegisterAccountResult.SvrWrongPin(e)
    } catch (e: IOException) {
      return@withContext RegisterAccountResult.UnknownError(e)
    }

    val registrationLock: String? = masterKey?.deriveRegistrationLock()

    val accountAttributes = AccountAttributes(
      zonarosaingKey = null,
      registrationId = registrationData.registrationId,
      fetchesMessages = registrationData.isNotFcm,
      registrationLock = registrationLock,
      unidentifiedAccessKey = unidentifiedAccessKey,
      unrestrictedUnidentifiedAccess = universalUnidentifiedAccess,
      capabilities = AppCapabilities.getCapabilities(true),
      discoverableByPhoneNumber = ZonaRosaStore.phoneNumberPrivacy.phoneNumberDiscoverabilityMode == PhoneNumberPrivacyValues.PhoneNumberDiscoverabilityMode.DISCOVERABLE,
      name = null,
      pniRegistrationId = registrationData.pniRegistrationId,
      recoveryPassword = registrationData.recoveryPassword
    )

    ZonaRosaStore.account.generateAciIdentityKeyIfNecessary()
    val aciIdentity: IdentityKeyPair = ZonaRosaStore.account.aciIdentityKey

    ZonaRosaStore.account.generatePniIdentityKeyIfNecessary()
    val pniIdentity: IdentityKeyPair = ZonaRosaStore.account.pniIdentityKey

    val aciPreKeyCollection = generateSignedAndLastResortPreKeys(aciIdentity, ZonaRosaStore.account.aciPreKeys)
    val pniPreKeyCollection = generateSignedAndLastResortPreKeys(pniIdentity, ZonaRosaStore.account.pniPreKeys)

    val result: NetworkResult<AccountRegistrationResult> = api.registerAccount(sessionId, registrationData.recoveryPassword, accountAttributes, aciPreKeyCollection, pniPreKeyCollection, registrationData.fcmToken, true)
      .map { accountRegistrationResponse: VerifyAccountResponse ->
        AccountRegistrationResult(
          uuid = accountRegistrationResponse.uuid,
          pni = accountRegistrationResponse.pni,
          storageCapable = accountRegistrationResponse.storageCapable,
          number = accountRegistrationResponse.number,
          masterKey = masterKey,
          pin = pin,
          aciPreKeyCollection = aciPreKeyCollection,
          pniPreKeyCollection = pniPreKeyCollection,
          reRegistration = accountRegistrationResponse.reregistration
        )
      }

    return@withContext RegisterAccountResult.from(result)
  }

  @WorkerThread
  fun registerAsLinkedDevice(
    context: Context,
    deviceName: String,
    message: ProvisionMessage,
    registrationData: RegistrationData,
    aciIdentityKeyPair: IdentityKeyPair,
    pniIdentityKeyPair: IdentityKeyPair
  ): NetworkResult<RegisterAsLinkedDeviceResponse> {
    val aci = message.aciBinary?.let { ACI.parseOrThrow(it) } ?: ACI.parseOrThrow(message.aci)
    val pni = message.pniBinary?.let { PNI.parseOrThrow(it) } ?: PNI.parseOrThrow(message.pni)

    val universalUnidentifiedAccess = ZonaRosaPreferences.isUniversalUnidentifiedAccess(context)
    val unidentifiedAccessKey = UnidentifiedAccess.deriveAccessKeyFrom(registrationData.profileKey)

    val encryptedDeviceName = DeviceNameCipher.encryptDeviceName(deviceName.toByteArray(StandardCharsets.UTF_8), aciIdentityKeyPair)

    val accountAttributes = AccountAttributes(
      zonarosaingKey = null,
      registrationId = getRegistrationId(),
      fetchesMessages = registrationData.fcmToken == null,
      registrationLock = null,
      unidentifiedAccessKey = unidentifiedAccessKey,
      unrestrictedUnidentifiedAccess = universalUnidentifiedAccess,
      capabilities = AppCapabilities.getCapabilities(false),
      discoverableByPhoneNumber = false,
      name = Base64.encodeWithPadding(encryptedDeviceName),
      pniRegistrationId = getPniRegistrationId(),
      recoveryPassword = null
    )

    val aciPreKeys = generateSignedAndLastResortPreKeys(aciIdentityKeyPair, ZonaRosaStore.account.aciPreKeys)
    val pniPreKeys = generateSignedAndLastResortPreKeys(pniIdentityKeyPair, ZonaRosaStore.account.pniPreKeys)

    return AccountManagerFactory
      .getInstance()
      .createUnauthenticated(context, message.number!!, -1, registrationData.password)
      .registrationApi
      .registerAsSecondaryDevice(message.provisioningCode!!, accountAttributes, aciPreKeys, pniPreKeys, registrationData.fcmToken)
      .map { respone ->
        RegisterAsLinkedDeviceResponse(
          deviceId = respone.deviceId.toInt(),
          accountRegistrationResult = AccountRegistrationResult(
            uuid = aci.toString(),
            pni = pni.toString(),
            storageCapable = false,
            number = message.number!!,
            masterKey = MasterKey(message.masterKey!!.toByteArray()),
            pin = null,
            aciPreKeyCollection = aciPreKeys,
            pniPreKeyCollection = pniPreKeys,
            reRegistration = true
          )
        )
      }
  }

  private suspend fun createSessionAndBlockForPushChallenge(accountManager: RegistrationApi, fcmToken: String, mcc: String?, mnc: String?): NetworkResult<RegistrationSessionMetadataResponse> = withContext(Dispatchers.IO) {
    // TODO [regv2]: do not use event bus nor latch
    val subscriber = PushTokenChallengeSubscriber()
    val eventBus = EventBus.getDefault()
    eventBus.register(subscriber)

    try {
      Log.d(TAG, "Requesting a registration session with FCM token…")
      val sessionCreationResponse = accountManager.createRegistrationSession(fcmToken, mcc, mnc)
      if (sessionCreationResponse !is NetworkResult.Success) {
        return@withContext sessionCreationResponse
      }

      val receivedPush = subscriber.latch.await(PUSH_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
      eventBus.unregister(subscriber)

      if (receivedPush) {
        val challenge = subscriber.challenge
        if (challenge != null) {
          Log.i(TAG, "Push challenge token received.")
          return@withContext accountManager.submitPushChallengeToken(sessionCreationResponse.result.metadata.id, challenge)
        } else {
          Log.w(TAG, "Push received but challenge token was null.")
        }
      } else {
        Log.i(TAG, "Push challenge timed out.")
      }
      Log.i(TAG, "Push challenge unsuccessful. Continuing with session created without one.")
      return@withContext sessionCreationResponse
    } catch (ex: Exception) {
      Log.w(TAG, "Exception caught, but the earlier try block should have caught it?", ex)
      return@withContext NetworkResult.ApplicationError<RegistrationSessionMetadataResponse>(ex)
    }
  }

  suspend fun hasValidSvrAuthCredentials(context: Context, e164: String, password: String): BackupAuthCheckResult = withContext(Dispatchers.IO) {
    val api: RegistrationApi = AccountManagerFactory.getInstance().createUnauthenticated(context, e164, ZonaRosaServiceAddress.DEFAULT_DEVICE_ID, password).registrationApi

    val svr3Result = ZonaRosaStore.svr.svr3AuthTokens
      ?.takeIf { Svr3Migration.shouldReadFromSvr3 }
      ?.takeIf { it.isNotEmpty() }
      ?.toSvrCredentials()
      ?.let { authTokens ->
        api
          .validateSvr3AuthCredential(e164, authTokens)
          .runIfSuccessful {
            val removedInvalidTokens = ZonaRosaStore.svr.removeSvr3AuthTokens(it.invalid)
            if (removedInvalidTokens) {
              BackupManager(context).dataChanged()
            }
          }
          .let { BackupAuthCheckResult.fromV3(it) }
      }

    if (svr3Result is BackupAuthCheckResult.SuccessWithCredentials) {
      Log.d(TAG, "Found valid SVR3 credentials.")
      return@withContext svr3Result
    }

    Log.d(TAG, "No valid SVR3 credentials, looking for SVR2.")

    return@withContext ZonaRosaStore.svr.svr2AuthTokens
      ?.takeIf { it.isNotEmpty() }
      ?.toSvrCredentials()
      ?.let { authTokens ->
        api
          .validateSvr2AuthCredential(e164, authTokens)
          .runIfSuccessful {
            val removedInvalidTokens = ZonaRosaStore.svr.removeSvr2AuthTokens(it.invalid)
            if (removedInvalidTokens) {
              BackupManager(context).dataChanged()
            }
          }
          .let { BackupAuthCheckResult.fromV2(it) }
      } ?: BackupAuthCheckResult.SuccessWithoutCredentials()
  }

  /** Converts the basic-auth creds we have locally into username:password pairs that are suitable for handing off to the service. */
  private fun List<String?>.toSvrCredentials(): List<String> {
    return this
      .asSequence()
      .filterNotNull()
      .take(10)
      .map { it.replace("Basic ", "").trim() }
      .mapNotNull {
        try {
          Base64.decode(it)
        } catch (e: IOException) {
          Log.w(TAG, "Encountered error trying to decode a token!", e)
          null
        }
      }
      .map { String(it, StandardCharsets.ISO_8859_1) }
      .toList()
  }

  /**
   * Starts an SMS listener to auto-enter a verification code.
   *
   * The listener [lives for 5 minutes](https://developers.google.com/android/reference/com/google/android/gms/auth/api/phone/SmsRetrieverApi).
   *
   * @return whether or not the Play Services SMS Listener was successfully registered.
   */
  suspend fun registerSmsListener(context: Context): Boolean {
    Log.d(TAG, "Attempting to start verification code SMS retriever.")
    val started = withTimeoutOrNull(5.seconds.inWholeMilliseconds) {
      try {
        SmsRetriever.getClient(context).startSmsRetriever().await()
        Log.d(TAG, "Successfully started verification code SMS retriever.")
        return@withTimeoutOrNull true
      } catch (ex: Exception) {
        Log.w(TAG, "Could not start verification code SMS retriever due to exception.", ex)
        return@withTimeoutOrNull false
      }
    }

    if (started == null) {
      Log.w(TAG, "Could not start verification code SMS retriever due to timeout.")
    }

    return started == true
  }

  @VisibleForTesting
  fun generateSignedAndLastResortPreKeys(identity: IdentityKeyPair, metadataStore: PreKeyMetadataStore): PreKeyCollection {
    val signedPreKey = PreKeyUtil.generateSignedPreKey(metadataStore.nextSignedPreKeyId, identity.privateKey)
    val lastResortKyberPreKey = PreKeyUtil.generateLastResortKyberPreKey(metadataStore.nextKyberPreKeyId, identity.privateKey)

    return PreKeyCollection(
      identity.publicKey,
      signedPreKey,
      lastResortKyberPreKey
    )
  }

  fun isMissingProfileData(): Boolean {
    return Recipient.self().profileName.isEmpty || !AvatarHelper.hasAvatar(AppDependencies.application, Recipient.self().id)
  }

  suspend fun waitForLinkAndSyncBackupDetails(maxWaitTime: Duration = 60.seconds): TransferArchiveResponse? {
    val startTime = System.currentTimeMillis()
    var timeRemaining = maxWaitTime.inWholeMilliseconds

    while (timeRemaining > 0 && coroutineContext.isActive) {
      Log.d(TAG, "[waitForLinkAndSyncBackupDetails] Willing to wait for $timeRemaining ms...")

      when (val result = ZonaRosaNetwork.linkDevice.waitForPrimaryDevice(timeout = 60.seconds)) {
        is NetworkResult.Success -> {
          Log.i(TAG, "[waitForLinkAndSyncBackupDetails] Transfer archive data provided by primary")
          return result.result
        }
        is NetworkResult.ApplicationError -> {
          Log.e(TAG, "[waitForLinkAndSyncBackupDetails] Application error!", result.throwable)
          throw result.throwable
        }
        is NetworkResult.NetworkError -> {
          Log.w(TAG, "[waitForLinkAndSyncBackupDetails] Hit a network error while waiting for linking. Will try to wait again.", result.exception)
        }
        is NetworkResult.StatusCodeError -> {
          when (result.code) {
            400 -> {
              Log.w(TAG, "[waitForLinkAndSyncBackupDetails] Invalid timeout!")
              return null
            }
            429 -> {
              Log.w(TAG, "[waitForLinkAndSyncBackupDetails] Hit a rate-limit. Will try to wait again after delay: ${result.retryAfter()}.")
              result.retryAfter()?.let { retryAfter ->
                delay(retryAfter)
              }
            }
            else -> {
              Log.w(TAG, "[waitForLinkAndSyncBackupDetails] Hit an unknown status code of ${result.code}. Will try to wait again.")
            }
          }
        }
      }

      timeRemaining = maxWaitTime.inWholeMilliseconds - (System.currentTimeMillis() - startTime)
    }

    Log.w(TAG, "[waitForLinkAndSyncBackupDetails] Failed to get transfer archive data from primary")
    return null
  }

  fun interface MasterKeyProducer {
    @Throws(IOException::class, SvrWrongPinException::class, SvrNoDataException::class)
    fun produceMasterKey(): MasterKey
  }

  enum class E164VerificationMode(val isSmsRetrieverSupported: Boolean, val transport: PushServiceSocket.VerificationCodeTransport) {
    SMS_WITH_LISTENER(true, PushServiceSocket.VerificationCodeTransport.SMS),
    SMS_WITHOUT_LISTENER(false, PushServiceSocket.VerificationCodeTransport.SMS),
    PHONE_CALL(false, PushServiceSocket.VerificationCodeTransport.VOICE)
  }

  private class PushTokenChallengeSubscriber {
    var challenge: String? = null
    val latch = CountDownLatch(1)

    @Subscribe
    fun onChallengeEvent(pushChallengeEvent: PushChallengeRequest.PushChallengeEvent) {
      Log.d(TAG, "Push challenge received!")
      challenge = pushChallengeEvent.challenge
      latch.countDown()
    }
  }
}

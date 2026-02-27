/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.dependencies

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import io.zonarosa.core.models.MasterKey
import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.net.Network
import io.zonarosa.libzonarosa.protocol.util.Hex
import io.zonarosa.registration.NetworkController
import io.zonarosa.registration.NetworkController.AccountAttributes
import io.zonarosa.registration.NetworkController.CheckSvrCredentialsRequest
import io.zonarosa.registration.NetworkController.CheckSvrCredentialsResponse
import io.zonarosa.registration.NetworkController.CreateSessionError
import io.zonarosa.registration.NetworkController.GetSessionStatusError
import io.zonarosa.registration.NetworkController.PreKeyCollection
import io.zonarosa.registration.NetworkController.RegisterAccountError
import io.zonarosa.registration.NetworkController.RegisterAccountResponse
import io.zonarosa.registration.NetworkController.RegistrationLockResponse
import io.zonarosa.registration.NetworkController.RegistrationNetworkResult
import io.zonarosa.registration.NetworkController.RequestVerificationCodeError
import io.zonarosa.registration.NetworkController.SessionMetadata
import io.zonarosa.registration.NetworkController.SubmitVerificationCodeError
import io.zonarosa.registration.NetworkController.ThirdPartyServiceErrorResponse
import io.zonarosa.registration.NetworkController.UpdateSessionError
import io.zonarosa.registration.NetworkController.VerificationCodeTransport
import io.zonarosa.registration.sample.fcm.FcmUtil
import io.zonarosa.registration.sample.fcm.PushChallengeReceiver
import io.zonarosa.registration.sample.storage.RegistrationPreferences
import io.zonarosa.service.api.svr.SecureValueRecovery.BackupResponse
import io.zonarosa.service.api.svr.SecureValueRecovery.RestoreResponse
import io.zonarosa.service.api.svr.SecureValueRecoveryV2
import io.zonarosa.service.api.util.SleepTimer
import io.zonarosa.service.api.websocket.HealthMonitor
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.api.websocket.WebSocketFactory
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import io.zonarosa.service.internal.push.AuthCredentials
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.util.StaticCredentialsProvider
import io.zonarosa.service.internal.websocket.LibZonaRosaChatConnection
import java.io.IOException
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import io.zonarosa.service.api.account.AccountAttributes as ServiceAccountAttributes
import io.zonarosa.service.api.account.PreKeyCollection as ServicePreKeyCollection

class DemoNetworkController(
  private val context: android.content.Context,
  private val pushServiceSocket: PushServiceSocket,
  private val serviceConfiguration: ZonaRosaServiceConfiguration,
  private val svr2MrEnclave: String
) : NetworkController {

  companion object {
    private val TAG = Log.tag(DemoNetworkController::class)
  }

  private val json = Json { ignoreUnknownKeys = true }

  private val okHttpClient: okhttp3.OkHttpClient by lazy {
    val trustStore = serviceConfiguration.zonarosaServiceUrls[0].trustStore
    val keyStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType())
    keyStore.load(trustStore.keyStoreInputStream, trustStore.keyStorePassword.toCharArray())

    val tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm())
    tmf.init(keyStore)

    val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
    sslContext.init(null, tmf.trustManagers, null)

    val trustManager = tmf.trustManagers[0] as javax.net.ssl.X509TrustManager

    okhttp3.OkHttpClient.Builder()
      .sslSocketFactory(sslContext.socketFactory, trustManager)
      .build()
  }

  override suspend fun createSession(
    e164: String,
    fcmToken: String?,
    mcc: String?,
    mnc: String?
  ): RegistrationNetworkResult<SessionMetadata, CreateSessionError> = withContext(Dispatchers.IO) {
    try {
      pushServiceSocket.createVerificationSessionV2(e164, fcmToken, mcc, mnc).use { response ->
        when (response.code) {
          200 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Success(session)
          }
          422 -> {
            RegistrationNetworkResult.Failure(CreateSessionError.InvalidRequest(response.body.string()))
          }
          429 -> {
            RegistrationNetworkResult.Failure(CreateSessionError.RateLimited(response.retryAfter()))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun getSession(sessionId: String): RegistrationNetworkResult<SessionMetadata, GetSessionStatusError> = withContext(Dispatchers.IO) {
    try {
      pushServiceSocket.getSessionStatusV2(sessionId).use { response ->
        when (response.code) {
          200 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Success(session)
          }
          400 -> {
            RegistrationNetworkResult.Failure(GetSessionStatusError.InvalidRequest(response.body.string()))
          }
          404 -> {
            RegistrationNetworkResult.Failure(GetSessionStatusError.SessionNotFound(response.body.string()))
          }
          422 -> {
            RegistrationNetworkResult.Failure(GetSessionStatusError.InvalidSessionId(response.body.string()))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun updateSession(
    sessionId: String?,
    pushChallengeToken: String?,
    captchaToken: String?
  ): RegistrationNetworkResult<SessionMetadata, UpdateSessionError> = withContext(Dispatchers.IO) {
    try {
      pushServiceSocket.patchVerificationSessionV2(
        sessionId,
        null, // pushToken
        null, // mcc
        null, // mnc
        captchaToken,
        pushChallengeToken
      ).use { response ->
        when (response.code) {
          200 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Success(session)
          }
          400 -> {
            RegistrationNetworkResult.Failure(UpdateSessionError.InvalidRequest(response.body.string()))
          }
          409 -> {
            RegistrationNetworkResult.Failure(UpdateSessionError.RejectedUpdate(response.body.string()))
          }
          429 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Failure(UpdateSessionError.RateLimited(response.retryAfter(), session))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun requestVerificationCode(
    sessionId: String,
    locale: Locale?,
    androidSmsRetrieverSupported: Boolean,
    transport: VerificationCodeTransport
  ): RegistrationNetworkResult<SessionMetadata, RequestVerificationCodeError> = withContext(Dispatchers.IO) {
    try {
      val socketTransport = when (transport) {
        VerificationCodeTransport.SMS -> PushServiceSocket.VerificationCodeTransport.SMS
        VerificationCodeTransport.VOICE -> PushServiceSocket.VerificationCodeTransport.VOICE
      }

      pushServiceSocket.requestVerificationCodeV2(
        sessionId,
        locale,
        androidSmsRetrieverSupported,
        socketTransport
      ).use { response ->
        when (response.code) {
          200 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Success(session)
          }
          400 -> {
            RegistrationNetworkResult.Failure(RequestVerificationCodeError.InvalidSessionId(response.body.string()))
          }
          404 -> {
            RegistrationNetworkResult.Failure(RequestVerificationCodeError.SessionNotFound(response.body.string()))
          }
          409 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Failure(RequestVerificationCodeError.MissingRequestInformationOrAlreadyVerified(session))
          }
          418 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Failure(RequestVerificationCodeError.CouldNotFulfillWithRequestedTransport(session))
          }
          429 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Failure(RequestVerificationCodeError.RateLimited(response.retryAfter(), session))
          }
          440 -> {
            val errorBody = json.decodeFromString<ThirdPartyServiceErrorResponse>(response.body.string())
            RegistrationNetworkResult.Failure(RequestVerificationCodeError.ThirdPartyServiceError(errorBody))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun submitVerificationCode(
    sessionId: String,
    verificationCode: String
  ): RegistrationNetworkResult<SessionMetadata, SubmitVerificationCodeError> = withContext(Dispatchers.IO) {
    try {
      pushServiceSocket.submitVerificationCodeV2(sessionId, verificationCode).use { response ->
        when (response.code) {
          200 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Success(session)
          }
          400 -> {
            RegistrationNetworkResult.Failure(SubmitVerificationCodeError.InvalidSessionIdOrVerificationCode(response.body.string()))
          }
          404 -> {
            RegistrationNetworkResult.Failure(SubmitVerificationCodeError.SessionNotFound(response.body.string()))
          }
          409 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Failure(SubmitVerificationCodeError.SessionAlreadyVerifiedOrNoCodeRequested(session))
          }
          429 -> {
            val session = json.decodeFromString<SessionMetadata>(response.body.string())
            RegistrationNetworkResult.Failure(SubmitVerificationCodeError.RateLimited(response.retryAfter(), session))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun registerAccount(
    e164: String,
    password: String,
    sessionId: String?,
    recoveryPassword: String?,
    attributes: AccountAttributes,
    aciPreKeys: PreKeyCollection,
    pniPreKeys: PreKeyCollection,
    fcmToken: String?,
    skipDeviceTransfer: Boolean
  ): RegistrationNetworkResult<RegisterAccountResponse, RegisterAccountError> = withContext(Dispatchers.IO) {
    check(sessionId != null || recoveryPassword != null) { "Either sessionId or recoveryPassword must be provided" }
    check(sessionId == null || recoveryPassword == null) { "Either sessionId or recoveryPassword must be provided, but not both" }

    try {
      val serviceAttributes = attributes.toServiceAccountAttributes()
      val serviceAciPreKeys = aciPreKeys.toServicePreKeyCollection()
      val servicePniPreKeys = pniPreKeys.toServicePreKeyCollection()

      pushServiceSocket.submitRegistrationRequestV2(
        e164,
        password,
        sessionId,
        recoveryPassword,
        serviceAttributes,
        serviceAciPreKeys,
        servicePniPreKeys,
        fcmToken,
        skipDeviceTransfer
      ).use { response ->
        when (response.code) {
          200 -> {
            val result = json.decodeFromString<RegisterAccountResponse>(response.body.string())
            RegistrationNetworkResult.Success(result)
          }
          401 -> {
            RegistrationNetworkResult.Failure(RegisterAccountError.SessionNotFoundOrNotVerified(response.body.string()))
          }
          403 -> {
            RegistrationNetworkResult.Failure(RegisterAccountError.RegistrationRecoveryPasswordIncorrect(response.body.string()))
          }
          409 -> {
            RegistrationNetworkResult.Failure(RegisterAccountError.DeviceTransferPossible)
          }
          422 -> {
            RegistrationNetworkResult.Failure(RegisterAccountError.InvalidRequest(response.body.string()))
          }
          423 -> {
            val lockResponse = json.decodeFromString<RegistrationLockResponse>(response.body.string())
            RegistrationNetworkResult.Failure(RegisterAccountError.RegistrationLock(lockResponse))
          }
          429 -> {
            RegistrationNetworkResult.Failure(RegisterAccountError.RateLimited(response.retryAfter()))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun getFcmToken(): String? {
    return try {
      FcmUtil.getToken(context)
    } catch (e: Exception) {
      Log.w(TAG, "Failed to get FCM token", e)
      null
    }
  }

  override suspend fun awaitPushChallengeToken(): String? {
    return try {
      PushChallengeReceiver.awaitChallenge()
    } catch (e: Exception) {
      Log.w(TAG, "Failed to await push challenge token", e)
      null
    }
  }

  override fun getCaptchaUrl(): String {
    return "https://zonarosacaptchas.org/staging/registration/generate.html"
  }

  override suspend fun restoreMasterKeyFromSvr(
    svrCredentials: NetworkController.SvrCredentials,
    pin: String
  ): RegistrationNetworkResult<NetworkController.MasterKeyResponse, NetworkController.RestoreMasterKeyError> = withContext(Dispatchers.IO) {
    try {
      val authCredentials = AuthCredentials.create(svrCredentials.username, svrCredentials.password)

      // Create a stub websocket that will never be used for pre-registration restore
      val stubWebSocketFactory = WebSocketFactory { throw UnsupportedOperationException("WebSocket not available during pre-registration") }
      val stubWebSocket = ZonaRosaWebSocket.AuthenticatedWebSocket(
        stubWebSocketFactory,
        { false },
        object : SleepTimer {
          override fun sleep(millis: Long) = Thread.sleep(millis)
        },
        0
      )

      val svr2 = SecureValueRecoveryV2(serviceConfiguration, svr2MrEnclave, stubWebSocket)

      when (val response = svr2.restoreDataPreRegistration(authCredentials, null, pin)) {
        is RestoreResponse.Success -> {
          Log.i(TAG, "[restoreMasterKeyFromSvr] Successfully restored master key from SVR2. Value: ${Hex.toStringCondensed(response.masterKey.serialize())}")
          RegistrationNetworkResult.Success(NetworkController.MasterKeyResponse(response.masterKey))
        }
        is RestoreResponse.PinMismatch -> {
          Log.w(TAG, "[restoreMasterKeyFromSvr] PIN mismatch. Tries remaining: ${response.triesRemaining}")
          RegistrationNetworkResult.Failure(NetworkController.RestoreMasterKeyError.WrongPin(response.triesRemaining))
        }
        is RestoreResponse.Missing -> {
          Log.w(TAG, "[restoreMasterKeyFromSvr] No SVR data found for user")
          RegistrationNetworkResult.Failure(NetworkController.RestoreMasterKeyError.NoDataFound)
        }
        is RestoreResponse.NetworkError -> {
          Log.w(TAG, "[restoreMasterKeyFromSvr] Network error", response.exception)
          RegistrationNetworkResult.NetworkError(response.exception)
        }
        is RestoreResponse.ApplicationError -> {
          Log.w(TAG, "[restoreMasterKeyFromSvr] Application error", response.exception)
          RegistrationNetworkResult.ApplicationError(response.exception)
        }
        is RestoreResponse.EnclaveNotFound -> {
          Log.w(TAG, "[restoreMasterKeyFromSvr] Enclave not found")
          RegistrationNetworkResult.ApplicationError(IllegalStateException("SVR2 enclave not found"))
        }
      }
    } catch (e: IOException) {
      Log.w(TAG, "[restoreMasterKeyFromSvr] IOException", e)
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      Log.w(TAG, "[restoreMasterKeyFromSvr] Exception", e)
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun setPinAndMasterKeyOnSvr(
    pin: String,
    masterKey: MasterKey
  ): RegistrationNetworkResult<NetworkController.SvrCredentials?, NetworkController.BackupMasterKeyError> = withContext(Dispatchers.IO) {
    try {
      val aci = RegistrationPreferences.aci
      val pni = RegistrationPreferences.pni
      val e164 = RegistrationPreferences.e164
      val password = RegistrationPreferences.servicePassword

      if (aci == null || e164 == null || password == null) {
        Log.w(TAG, "[backupMasterKeyToSvr] Credentials not available, cannot authenticate")
        return@withContext RegistrationNetworkResult.Failure(NetworkController.BackupMasterKeyError.NotRegistered)
      }

      val network = Network(Network.Environment.STAGING, "ZonaRosa-Android-Registration-Sample", emptyMap(), Network.BuildVariant.PRODUCTION)
      val credentialsProvider = StaticCredentialsProvider(aci, pni, e164, 1, password)
      val healthMonitor = object : HealthMonitor {
        override fun onKeepAliveResponse(sentTimestamp: Long, isIdentifiedWebSocket: Boolean) {}
        override fun onMessageError(status: Int, isIdentifiedWebSocket: Boolean) {}
      }

      val libZonaRosaConnection = LibZonaRosaChatConnection(
        name = "SVR-Backup",
        network = network,
        credentialsProvider = credentialsProvider,
        receiveStories = false,
        healthMonitor = healthMonitor
      )

      val authWebSocket = ZonaRosaWebSocket.AuthenticatedWebSocket(
        connectionFactory = { libZonaRosaConnection },
        canConnect = { true },
        sleepTimer = { millis -> Thread.sleep(millis) },
        disconnectTimeoutMs = 60.seconds.inWholeMilliseconds
      )

      authWebSocket.connect()

      val svr2 = SecureValueRecoveryV2(serviceConfiguration, svr2MrEnclave, authWebSocket)
      val session = svr2.setPin(pin, masterKey)
      val response = session.execute()

      authWebSocket.disconnect()

      when (response) {
        is BackupResponse.Success -> {
          Log.i(TAG, "[backupMasterKeyToSvr] Successfully backed up master key to SVR2. Value: ${Hex.toStringCondensed(masterKey.serialize())}")
          RegistrationNetworkResult.Success(NetworkController.SvrCredentials(response.authorization.username(), response.authorization.password()))
        }
        is BackupResponse.ApplicationError -> {
          Log.w(TAG, "[backupMasterKeyToSvr] Application error", response.exception)
          RegistrationNetworkResult.ApplicationError(response.exception)
        }
        is BackupResponse.NetworkError -> {
          Log.w(TAG, "[backupMasterKeyToSvr] Network error", response.exception)
          RegistrationNetworkResult.NetworkError(response.exception)
        }
        is BackupResponse.EnclaveNotFound -> {
          Log.w(TAG, "[backupMasterKeyToSvr] Enclave not found")
          RegistrationNetworkResult.Failure(NetworkController.BackupMasterKeyError.EnclaveNotFound)
        }
        is BackupResponse.ExposeFailure -> {
          Log.w(TAG, "[backupMasterKeyToSvr] Expose failure -- per spec, treat as success.")
          RegistrationNetworkResult.Success(null)
        }
        is BackupResponse.ServerRejected -> {
          Log.w(TAG, "[backupMasterKeyToSvr] Server rejected")
          RegistrationNetworkResult.NetworkError(IOException("Server rejected backup request"))
        }
        is BackupResponse.RateLimited -> {
          RegistrationNetworkResult.NetworkError(IOException("Rate limited"))
        }
      }
    } catch (e: IOException) {
      Log.w(TAG, "[backupMasterKeyToSvr] IOException", e)
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      Log.w(TAG, "[backupMasterKeyToSvr] Exception", e)
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun enqueueSvrGuessResetJob() {
    val pin = checkNotNull(RegistrationPreferences.pin) { "Pin is not set!" }
    val masterKey = checkNotNull(RegistrationPreferences.masterKey) { "Master key is not set!" }

    val result = setPinAndMasterKeyOnSvr(pin, masterKey)
    if (result !is RegistrationNetworkResult.Success) {
      Log.w(TAG, "Failed to set pin and master key on SVR! A real app would retry. Result: $result")
    }
  }

  override suspend fun enableRegistrationLock(): RegistrationNetworkResult<Unit, NetworkController.SetRegistrationLockError> = withContext(Dispatchers.IO) {
    val aci = RegistrationPreferences.aci
    val password = RegistrationPreferences.servicePassword
    val masterKey = RegistrationPreferences.masterKey

    if (aci == null || password == null) {
      Log.w(TAG, "[enableRegistrationLock] Credentials not available")
      return@withContext RegistrationNetworkResult.Failure(NetworkController.SetRegistrationLockError.NotRegistered)
    }

    if (masterKey == null) {
      Log.w(TAG, "[enableRegistrationLock] Master key not available")
      return@withContext RegistrationNetworkResult.Failure(NetworkController.SetRegistrationLockError.NoPinSet)
    }

    val registrationLockToken = masterKey.deriveRegistrationLock()

    try {
      val credentials = okhttp3.Credentials.basic(aci.toString(), password)
      val baseUrl = serviceConfiguration.zonarosaServiceUrls[0].url
      val requestBody = """{"registrationLock":"$registrationLockToken"}"""
        .toRequestBody("application/json".toMediaType())

      val request = okhttp3.Request.Builder()
        .url("$baseUrl/v1/accounts/registration_lock")
        .put(requestBody)
        .header("Authorization", credentials)
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        when (response.code) {
          200, 204 -> {
            Log.i(TAG, "[enableRegistrationLock] Successfully enabled registration lock")
            RegistrationNetworkResult.Success(Unit)
          }
          401 -> {
            RegistrationNetworkResult.Failure(NetworkController.SetRegistrationLockError.Unauthorized)
          }
          422 -> {
            RegistrationNetworkResult.Failure(NetworkController.SetRegistrationLockError.InvalidRequest(response.body?.string() ?: ""))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}"))
          }
        }
      }
    } catch (e: IOException) {
      Log.w(TAG, "[enableRegistrationLock] IOException", e)
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      Log.w(TAG, "[enableRegistrationLock] Exception", e)
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun disableRegistrationLock(): RegistrationNetworkResult<Unit, NetworkController.SetRegistrationLockError> = withContext(Dispatchers.IO) {
    val aci = RegistrationPreferences.aci
    val password = RegistrationPreferences.servicePassword

    if (aci == null || password == null) {
      Log.w(TAG, "[disableRegistrationLock] Credentials not available")
      return@withContext RegistrationNetworkResult.Failure(NetworkController.SetRegistrationLockError.NotRegistered)
    }

    try {
      val credentials = okhttp3.Credentials.basic(aci.toString(), password)
      val baseUrl = serviceConfiguration.zonarosaServiceUrls[0].url

      val request = okhttp3.Request.Builder()
        .url("$baseUrl/v1/accounts/registration_lock")
        .delete()
        .header("Authorization", credentials)
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        when (response.code) {
          200, 204 -> {
            Log.i(TAG, "[disableRegistrationLock] Successfully disabled registration lock")
            RegistrationNetworkResult.Success(Unit)
          }
          401 -> {
            RegistrationNetworkResult.Failure(NetworkController.SetRegistrationLockError.Unauthorized)
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}"))
          }
        }
      }
    } catch (e: IOException) {
      Log.w(TAG, "[disableRegistrationLock] IOException", e)
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      Log.w(TAG, "[disableRegistrationLock] Exception", e)
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun setAccountAttributes(
    attributes: AccountAttributes
  ): RegistrationNetworkResult<Unit, NetworkController.SetAccountAttributesError> = withContext(Dispatchers.IO) {
    val aci = RegistrationPreferences.aci
    val password = RegistrationPreferences.servicePassword

    if (aci == null || password == null) {
      Log.w(TAG, "[setAccountAttributes] Credentials not available")
      return@withContext RegistrationNetworkResult.Failure(NetworkController.SetAccountAttributesError.Unauthorized)
    }

    try {
      val credentials = okhttp3.Credentials.basic(aci.toString(), password)
      val baseUrl = serviceConfiguration.zonarosaServiceUrls[0].url
      val requestBody = json.encodeToString(AccountAttributes.serializer(), attributes)
        .toRequestBody("application/json".toMediaType())

      val request = okhttp3.Request.Builder()
        .url("$baseUrl/v1/accounts/attributes")
        .put(requestBody)
        .header("Authorization", credentials)
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        when (response.code) {
          200, 204 -> {
            Log.i(TAG, "[setAccountAttributes] Successfully updated account attributes")
            RegistrationNetworkResult.Success(Unit)
          }
          401 -> {
            RegistrationNetworkResult.Failure(NetworkController.SetAccountAttributesError.Unauthorized)
          }
          422 -> {
            RegistrationNetworkResult.Failure(NetworkController.SetAccountAttributesError.InvalidRequest(response.body?.string() ?: ""))
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body?.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      Log.w(TAG, "[setAccountAttributes] IOException", e)
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      Log.w(TAG, "[setAccountAttributes] Exception", e)
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun getSvrCredentials(): RegistrationNetworkResult<NetworkController.SvrCredentials, NetworkController.GetSvrCredentialsError> = withContext(Dispatchers.IO) {
    val aci = RegistrationPreferences.aci
    val password = RegistrationPreferences.servicePassword

    if (aci == null || password == null) {
      Log.w(TAG, "[getSvrCredentials] Credentials not available")
      return@withContext RegistrationNetworkResult.Failure(NetworkController.GetSvrCredentialsError.NoServiceCredentialsAvailable)
    }

    try {
      val credentials = okhttp3.Credentials.basic(aci.toString(), password)
      val baseUrl = serviceConfiguration.zonarosaServiceUrls[0].url

      val request = okhttp3.Request.Builder()
        .url("$baseUrl/v2/svr/auth")
        .get()
        .header("Authorization", credentials)
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        when (response.code) {
          200 -> {
            val svrCredentials = json.decodeFromString<NetworkController.SvrCredentials>(response.body.string())
            RegistrationNetworkResult.Success(svrCredentials)
          }
          401 -> {
            RegistrationNetworkResult.Failure(NetworkController.GetSvrCredentialsError.Unauthorized)
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      Log.w(TAG, "[getSvrCredentials] IOException", e)
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      Log.w(TAG, "[getSvrCredentials] Exception", e)
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  override suspend fun checkSvrCredentials(
    e164: String,
    credentials: List<NetworkController.SvrCredentials>
  ): RegistrationNetworkResult<CheckSvrCredentialsResponse, NetworkController.CheckSvrCredentialsError> = withContext(Dispatchers.IO) {
    try {
      val baseUrl = serviceConfiguration.zonarosaServiceUrls[0].url

      val requestBody = json.encodeToString(
        CheckSvrCredentialsRequest.serializer(),
        CheckSvrCredentialsRequest.createForCredentials(number = e164, credentials)
      ).toRequestBody("application/json".toMediaType())

      val request = okhttp3.Request.Builder()
        .url("$baseUrl/v2/svr/auth/check")
        .post(requestBody)
        .build()

      okHttpClient.newCall(request).execute().use { response ->
        when (response.code) {
          200 -> {
            val result = json.decodeFromString<CheckSvrCredentialsResponse>(response.body.string())
            RegistrationNetworkResult.Success(result)
          }
          400, 422 -> {
            RegistrationNetworkResult.Failure(NetworkController.CheckSvrCredentialsError.InvalidRequest(response.body.string()))
          }
          401 -> {
            RegistrationNetworkResult.Failure(NetworkController.CheckSvrCredentialsError.Unauthorized)
          }
          else -> {
            RegistrationNetworkResult.ApplicationError(IllegalStateException("Unexpected response code: ${response.code}, body: ${response.body?.string()}"))
          }
        }
      }
    } catch (e: IOException) {
      Log.w(TAG, "[checkSvrCredentials] IOException", e)
      RegistrationNetworkResult.NetworkError(e)
    } catch (e: Exception) {
      Log.w(TAG, "[checkSvrCredentials] Exception", e)
      RegistrationNetworkResult.ApplicationError(e)
    }
  }

  private fun AccountAttributes.toServiceAccountAttributes(): ServiceAccountAttributes {
    return ServiceAccountAttributes(
      zonarosaingKey,
      registrationId,
      fetchesMessages,
      registrationLock,
      unidentifiedAccessKey,
      unrestrictedUnidentifiedAccess,
      capabilities?.toServiceCapabilities(),
      discoverableByPhoneNumber,
      name,
      pniRegistrationId,
      recoveryPassword
    )
  }

  private fun AccountAttributes.Capabilities.toServiceCapabilities(): ServiceAccountAttributes.Capabilities {
    return ServiceAccountAttributes.Capabilities(
      storage,
      versionedExpirationTimer,
      attachmentBackfill,
      spqr
    )
  }

  private fun PreKeyCollection.toServicePreKeyCollection(): ServicePreKeyCollection {
    return ServicePreKeyCollection(
      identityKey = identityKey,
      signedPreKey = signedPreKey,
      lastResortKyberPreKey = lastResortKyberPreKey
    )
  }

  private fun Response.retryAfter(): Duration {
    return this.header("Retry-After")?.toLongOrNull()?.seconds ?: 0.seconds
  }
}

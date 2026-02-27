/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.dependencies

import android.app.Application
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.subjects.Subject
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.orNull
import io.zonarosa.core.util.resettableLazy
import io.zonarosa.libzonarosa.net.Network
import io.zonarosa.libzonarosa.zkgroup.receipts.ClientZkReceiptOperations
import io.zonarosa.messenger.crypto.storage.ZonaRosaServiceDataStoreImpl
import io.zonarosa.messenger.groups.GroupsV2Authorization
import io.zonarosa.messenger.groups.GroupsV2AuthorizationMemoryValueCache
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.messages.IncomingMessageObserver
import io.zonarosa.messenger.net.StandardUserAgentInterceptor
import io.zonarosa.messenger.payments.Payments
import io.zonarosa.messenger.push.ZonaRosaServiceNetworkAccess
import io.zonarosa.messenger.push.ZonaRosaServiceTrustStore
import io.zonarosa.service.api.ZonaRosaServiceAccountManager
import io.zonarosa.service.api.ZonaRosaServiceMessageReceiver
import io.zonarosa.service.api.ZonaRosaServiceMessageSender
import io.zonarosa.service.api.account.AccountApi
import io.zonarosa.service.api.archive.ArchiveApi
import io.zonarosa.service.api.attachment.AttachmentApi
import io.zonarosa.service.api.calling.CallingApi
import io.zonarosa.service.api.cds.CdsApi
import io.zonarosa.service.api.certificate.CertificateApi
import io.zonarosa.service.api.donations.DonationsApi
import io.zonarosa.service.api.groupsv2.GroupsV2Operations
import io.zonarosa.service.api.keys.KeysApi
import io.zonarosa.service.api.link.LinkDeviceApi
import io.zonarosa.service.api.message.MessageApi
import io.zonarosa.service.api.payments.PaymentsApi
import io.zonarosa.service.api.profiles.ProfileApi
import io.zonarosa.service.api.provisioning.ProvisioningApi
import io.zonarosa.service.api.push.TrustStore
import io.zonarosa.service.api.ratelimit.RateLimitChallengeApi
import io.zonarosa.service.api.registration.RegistrationApi
import io.zonarosa.service.api.remoteconfig.RemoteConfigApi
import io.zonarosa.service.api.services.DonationsService
import io.zonarosa.service.api.services.ProfileService
import io.zonarosa.service.api.storage.StorageServiceApi
import io.zonarosa.service.api.svr.SvrBApi
import io.zonarosa.service.api.username.UsernameApi
import io.zonarosa.service.api.util.Tls12SocketFactory
import io.zonarosa.service.api.util.TlsProxySocketFactory
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.api.websocket.WebSocketConnectionState
import io.zonarosa.service.api.websocket.WebSocketUnavailableException
import io.zonarosa.service.internal.push.PushServiceSocket
import io.zonarosa.service.internal.util.BlacklistingTrustManager
import io.zonarosa.service.internal.util.Util
import java.security.KeyManagementException
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * A subset of [AppDependencies] that relies on the network. We bundle them together because when the network
 * needs to get reset, we just throw out the whole thing and recreate it.
 */
class NetworkDependenciesModule(
  private val application: Application,
  private val provider: AppDependencies.Provider,
  private val webSocketStateSubject: Subject<WebSocketConnectionState>
) {

  companion object {
    private val TAG = "NetworkDependencies"
  }

  private val disposables: CompositeDisposable = CompositeDisposable()

  val zonarosaServiceNetworkAccess: ZonaRosaServiceNetworkAccess by lazy {
    provider.provideZonaRosaServiceNetworkAccess()
  }

  private val _protocolStore = resettableLazy {
    provider.provideProtocolStore()
  }
  val protocolStore: ZonaRosaServiceDataStoreImpl by _protocolStore

  private val _zonarosaServiceMessageSender = resettableLazy {
    provider.provideZonaRosaServiceMessageSender(protocolStore, pushServiceSocket, attachmentApi, messageApi, keysApi)
  }
  val zonarosaServiceMessageSender: ZonaRosaServiceMessageSender by _zonarosaServiceMessageSender

  val incomingMessageObserver: IncomingMessageObserver by lazy {
    provider.provideIncomingMessageObserver(authWebSocket, unauthWebSocket)
  }

  val pushServiceSocket: PushServiceSocket by lazy {
    provider.providePushServiceSocket(zonarosaServiceNetworkAccess.getConfiguration(), groupsV2Operations)
  }

  val zonarosaServiceAccountManager: ZonaRosaServiceAccountManager by lazy {
    provider.provideZonaRosaServiceAccountManager(authWebSocket, accountApi, pushServiceSocket, groupsV2Operations)
  }

  val libzonarosaNetwork: Network by lazy {
    provider.provideLibzonarosaNetwork(zonarosaServiceNetworkAccess.getConfiguration())
  }

  val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket by lazy {
    provider.provideAuthWebSocket({ zonarosaServiceNetworkAccess.getConfiguration() }, { libzonarosaNetwork }).also {
      disposables += it.state.subscribe { s -> webSocketStateSubject.onNext(s) }
    }
  }

  val unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket by lazy {
    provider.provideUnauthWebSocket({ zonarosaServiceNetworkAccess.getConfiguration() }, { libzonarosaNetwork })
  }

  val groupsV2Authorization: GroupsV2Authorization by lazy {
    val authCache: GroupsV2Authorization.ValueCache = GroupsV2AuthorizationMemoryValueCache(ZonaRosaStore.groupsV2AciAuthorizationCache)
    GroupsV2Authorization(zonarosaServiceAccountManager.groupsV2Api, authCache)
  }

  val groupsV2Operations: GroupsV2Operations by lazy {
    provider.provideGroupsV2Operations(zonarosaServiceNetworkAccess.getConfiguration())
  }

  val clientZkReceiptOperations: ClientZkReceiptOperations by lazy {
    provider.provideClientZkReceiptOperations(zonarosaServiceNetworkAccess.getConfiguration())
  }

  val zonarosaServiceMessageReceiver: ZonaRosaServiceMessageReceiver by lazy {
    provider.provideZonaRosaServiceMessageReceiver(pushServiceSocket)
  }

  val payments: Payments by lazy {
    provider.providePayments(paymentsApi)
  }

  val profileService: ProfileService by lazy {
    provider.provideProfileService(groupsV2Operations.profileOperations, authWebSocket, unauthWebSocket)
  }

  val donationsService: DonationsService by lazy {
    provider.provideDonationsService(donationsApi)
  }

  val archiveApi: ArchiveApi by lazy {
    provider.provideArchiveApi(authWebSocket, unauthWebSocket, pushServiceSocket)
  }

  val keysApi: KeysApi by lazy {
    provider.provideKeysApi(authWebSocket, unauthWebSocket)
  }

  val attachmentApi: AttachmentApi by lazy {
    provider.provideAttachmentApi(authWebSocket, pushServiceSocket)
  }

  val linkDeviceApi: LinkDeviceApi by lazy {
    provider.provideLinkDeviceApi(authWebSocket)
  }

  val registrationApi: RegistrationApi by lazy {
    provider.provideRegistrationApi(pushServiceSocket)
  }

  val storageServiceApi: StorageServiceApi by lazy {
    provider.provideStorageServiceApi(authWebSocket, pushServiceSocket)
  }

  val accountApi: AccountApi by lazy {
    provider.provideAccountApi(authWebSocket)
  }

  val usernameApi: UsernameApi by lazy {
    provider.provideUsernameApi(unauthWebSocket)
  }

  val callingApi: CallingApi by lazy {
    provider.provideCallingApi(authWebSocket, unauthWebSocket, pushServiceSocket)
  }

  val paymentsApi: PaymentsApi by lazy {
    provider.providePaymentsApi(authWebSocket)
  }

  val cdsApi: CdsApi by lazy {
    provider.provideCdsApi(authWebSocket)
  }

  val rateLimitChallengeApi: RateLimitChallengeApi by lazy {
    provider.provideRateLimitChallengeApi(authWebSocket)
  }

  val messageApi: MessageApi by lazy {
    provider.provideMessageApi(authWebSocket, unauthWebSocket)
  }

  val provisioningApi: ProvisioningApi by lazy {
    provider.provideProvisioningApi(authWebSocket, unauthWebSocket)
  }

  val certificateApi: CertificateApi by lazy {
    provider.provideCertificateApi(authWebSocket)
  }

  val profileApi: ProfileApi by lazy {
    provider.provideProfileApi(authWebSocket, unauthWebSocket, pushServiceSocket, groupsV2Operations.profileOperations)
  }

  val remoteConfigApi: RemoteConfigApi by lazy {
    provider.provideRemoteConfigApi(authWebSocket, pushServiceSocket)
  }

  val donationsApi: DonationsApi by lazy {
    provider.provideDonationsApi(authWebSocket, unauthWebSocket)
  }

  val svrBApi: SvrBApi by lazy {
    provider.provideSvrBApi(libzonarosaNetwork)
  }

  val keyTransparencyApi: KeyTransparencyApi by lazy {
    provider.provideKeyTransparencyApi(unauthWebSocket)
  }

  val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .addInterceptor(StandardUserAgentInterceptor())
      .dns(ZonaRosaServiceNetworkAccess.DNS)
      .build()
  }

  val zonarosaOkHttpClient: OkHttpClient by lazy {
    try {
      val baseClient = okHttpClient
      val sslContext = SSLContext.getInstance("TLS")
      val trustStore: TrustStore = ZonaRosaServiceTrustStore(application)
      val trustManagers = BlacklistingTrustManager.createFor(trustStore)

      sslContext.init(null, trustManagers, null)

      val builder = baseClient.newBuilder()
        .sslSocketFactory(Tls12SocketFactory(sslContext.socketFactory), trustManagers[0] as X509TrustManager)
        .connectionSpecs(Util.immutableList(ConnectionSpec.RESTRICTED_TLS))

      val proxy = zonarosaServiceNetworkAccess.getConfiguration().zonarosaProxy.orNull()
      if (proxy != null) {
        builder.socketFactory(TlsProxySocketFactory(proxy.host, proxy.port, zonarosaServiceNetworkAccess.getConfiguration().dns))
      }

      builder.build()
    } catch (e: NoSuchAlgorithmException) {
      throw AssertionError(e)
    } catch (e: KeyManagementException) {
      throw AssertionError(e)
    }
  }

  fun closeConnections() {
    Log.i(TAG, "Closing connections.")
    incomingMessageObserver.terminate()
    if (_zonarosaServiceMessageSender.isInitialized()) {
      zonarosaServiceMessageSender.cancelInFlightRequests()
    }
    unauthWebSocket.disconnect()
    disposables.clear()
  }

  fun openConnections() {
    try {
      authWebSocket.connect()
    } catch (e: WebSocketUnavailableException) {
      Log.w(TAG, "Not allowed to start auth websocket", e)
    }

    try {
      unauthWebSocket.connect()
    } catch (e: WebSocketUnavailableException) {
      Log.w(TAG, "Not allowed to start unauth websocket", e)
    }

    incomingMessageObserver
  }

  fun resetProtocolStores() {
    _protocolStore.reset()
    _zonarosaServiceMessageSender.reset()
  }
}

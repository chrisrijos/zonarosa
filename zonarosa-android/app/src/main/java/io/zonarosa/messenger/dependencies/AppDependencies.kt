package io.zonarosa.messenger.dependencies

import android.annotation.SuppressLint
import android.app.Application
import io.reactivex.rxjava3.subjects.BehaviorSubject
import okhttp3.OkHttpClient
import io.zonarosa.core.ui.CoreUiDependencies
import io.zonarosa.core.util.CoreUtilDependencies
import io.zonarosa.core.util.billing.BillingApi
import io.zonarosa.core.util.concurrent.DeadlockDetector
import io.zonarosa.core.util.concurrent.LatestValueObservable
import io.zonarosa.core.util.orNull
import io.zonarosa.core.util.resettableLazy
import io.zonarosa.glide.ZonaRosaGlideDependencies
import io.zonarosa.libzonarosa.net.Network
import io.zonarosa.libzonarosa.zkgroup.profiles.ClientZkProfileOperations
import io.zonarosa.libzonarosa.zkgroup.receipts.ClientZkReceiptOperations
import io.zonarosa.mediasend.MediaSendDependencies
import io.zonarosa.messenger.BuildConfig
import io.zonarosa.messenger.components.TypingStatusRepository
import io.zonarosa.messenger.components.TypingStatusSender
import io.zonarosa.messenger.crypto.storage.ZonaRosaServiceDataStoreImpl
import io.zonarosa.messenger.database.DatabaseObserver
import io.zonarosa.messenger.database.PendingRetryReceiptCache
import io.zonarosa.messenger.dependencies.AppDependencies.authWebSocket
import io.zonarosa.messenger.groups.GroupsV2Authorization
import io.zonarosa.messenger.jobmanager.JobManager
import io.zonarosa.messenger.megaphone.MegaphoneRepository
import io.zonarosa.messenger.messages.IncomingMessageObserver
import io.zonarosa.messenger.notifications.MessageNotifier
import io.zonarosa.messenger.payments.Payments
import io.zonarosa.messenger.push.ZonaRosaServiceNetworkAccess
import io.zonarosa.messenger.recipients.LiveRecipientCache
import io.zonarosa.messenger.revealable.ViewOnceMessageManager
import io.zonarosa.messenger.service.DeletedCallEventManager
import io.zonarosa.messenger.service.ExpiringMessageManager
import io.zonarosa.messenger.service.ExpiringStoriesManager
import io.zonarosa.messenger.service.PendingRetryReceiptManager
import io.zonarosa.messenger.service.PinnedMessageManager
import io.zonarosa.messenger.service.ScheduledMessageManager
import io.zonarosa.messenger.service.TrimThreadsByDateManager
import io.zonarosa.messenger.service.webrtc.ZonaRosaCallManager
import io.zonarosa.messenger.shakereport.ShakeToReport
import io.zonarosa.messenger.util.EarlyMessageCache
import io.zonarosa.messenger.util.FrameRateTracker
import io.zonarosa.messenger.video.exo.GiphyMp4Cache
import io.zonarosa.messenger.video.exo.SimpleExoPlayerPool
import io.zonarosa.messenger.webrtc.audio.AudioManagerCompat
import io.zonarosa.service.api.ZonaRosaServiceAccountManager
import io.zonarosa.service.api.ZonaRosaServiceDataStore
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
import io.zonarosa.service.api.ratelimit.RateLimitChallengeApi
import io.zonarosa.service.api.registration.RegistrationApi
import io.zonarosa.service.api.remoteconfig.RemoteConfigApi
import io.zonarosa.service.api.services.DonationsService
import io.zonarosa.service.api.services.ProfileService
import io.zonarosa.service.api.storage.StorageServiceApi
import io.zonarosa.service.api.svr.SvrBApi
import io.zonarosa.service.api.username.UsernameApi
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.api.websocket.WebSocketConnectionState
import io.zonarosa.service.internal.configuration.HttpProxy
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import io.zonarosa.service.internal.push.PushServiceSocket
import java.util.function.Supplier

/**
 * Location for storing and retrieving application-scoped singletons. Users must call
 * [.init] before using any of the methods, preferably early on in
 * [Application.onCreate].
 *
 * All future application-scoped singletons should be written as normal objects, then placed here
 * to manage their singleton-ness.
 */
@SuppressLint("StaticFieldLeak")
object AppDependencies {
  private lateinit var _application: Application
  private lateinit var provider: Provider

  @JvmStatic
  @Synchronized
  fun init(application: Application, provider: Provider) {
    if (this::_application.isInitialized || this::provider.isInitialized) {
      return
    }

    _application = application
    AppDependencies.provider = provider

    CoreUtilDependencies.init(
      application,
      CoreUtilDependenciesProvider,
      CoreUtilDependencies.BuildInfo(
        canonicalVersionCode = BuildConfig.CANONICAL_VERSION_CODE,
        buildTimestamp = BuildConfig.BUILD_TIMESTAMP
      )
    )
    CoreUiDependencies.init(application, CoreUiDependenciesProvider)
    ZonaRosaGlideDependencies.init(application, ZonaRosaGlideDependenciesProvider)
    MediaSendDependencies.init(application, MediaSendDependenciesProvider)
  }

  @JvmStatic
  val isInitialized: Boolean
    get() = this::_application.isInitialized

  @JvmStatic
  val application: Application
    get() = _application

  @JvmStatic
  val recipientCache: LiveRecipientCache by lazy {
    provider.provideRecipientCache()
  }

  @JvmStatic
  val jobManager: JobManager by lazy {
    provider.provideJobManager()
  }

  @JvmStatic
  val frameRateTracker: FrameRateTracker by lazy {
    provider.provideFrameRateTracker()
  }

  @JvmStatic
  val megaphoneRepository: MegaphoneRepository by lazy {
    provider.provideMegaphoneRepository()
  }

  @JvmStatic
  val earlyMessageCache: EarlyMessageCache by lazy {
    provider.provideEarlyMessageCache()
  }

  @JvmStatic
  val typingStatusRepository: TypingStatusRepository by lazy {
    provider.provideTypingStatusRepository()
  }

  @JvmStatic
  val typingStatusSender: TypingStatusSender by lazy {
    provider.provideTypingStatusSender()
  }

  @JvmStatic
  val databaseObserver: DatabaseObserver by lazy {
    provider.provideDatabaseObserver()
  }

  @JvmStatic
  val trimThreadsByDateManager: TrimThreadsByDateManager by lazy {
    provider.provideTrimThreadsByDateManager()
  }

  @JvmStatic
  val viewOnceMessageManager: ViewOnceMessageManager by lazy {
    provider.provideViewOnceMessageManager()
  }

  @JvmStatic
  val expiringMessageManager: ExpiringMessageManager by lazy {
    provider.provideExpiringMessageManager()
  }

  @JvmStatic
  val deletedCallEventManager: DeletedCallEventManager by lazy {
    provider.provideDeletedCallEventManager()
  }

  @JvmStatic
  val zonarosaCallManager: ZonaRosaCallManager by lazy {
    provider.provideZonaRosaCallManager()
  }

  @JvmStatic
  val shakeToReport: ShakeToReport by lazy {
    provider.provideShakeToReport()
  }

  @JvmStatic
  val pendingRetryReceiptManager: PendingRetryReceiptManager by lazy {
    provider.providePendingRetryReceiptManager()
  }

  @JvmStatic
  val pendingRetryReceiptCache: PendingRetryReceiptCache by lazy {
    provider.providePendingRetryReceiptCache()
  }

  @JvmStatic
  val messageNotifier: MessageNotifier by lazy {
    provider.provideMessageNotifier()
  }

  @JvmStatic
  val giphyMp4Cache: GiphyMp4Cache by lazy {
    provider.provideGiphyMp4Cache()
  }

  @JvmStatic
  val exoPlayerPool: SimpleExoPlayerPool by lazy {
    provider.provideExoPlayerPool()
  }

  @JvmStatic
  val deadlockDetector: DeadlockDetector by lazy {
    provider.provideDeadlockDetector()
  }

  @JvmStatic
  val expireStoriesManager: ExpiringStoriesManager by lazy {
    provider.provideExpiringStoriesManager()
  }

  @JvmStatic
  val scheduledMessageManager: ScheduledMessageManager by lazy {
    provider.provideScheduledMessageManager()
  }

  @JvmStatic
  val pinnedMessageManager: PinnedMessageManager by lazy {
    provider.providePinnedMessageManager()
  }

  @JvmStatic
  val androidCallAudioManager: AudioManagerCompat by lazy {
    provider.provideAndroidCallAudioManager()
  }

  @JvmStatic
  val billingApi: BillingApi by lazy {
    provider.provideBillingApi()
  }

  private val _webSocketObserver: BehaviorSubject<WebSocketConnectionState> = BehaviorSubject.create()

  /**
   * An observable that emits the current state of the WebSocket connection across the various lifecycles
   * of the [authWebSocket].
   */
  @JvmStatic
  val webSocketObserver: LatestValueObservable<WebSocketConnectionState> = LatestValueObservable(_webSocketObserver)

  private val _networkModule = resettableLazy {
    NetworkDependenciesModule(application, provider, _webSocketObserver)
  }
  private val networkModule by _networkModule

  @JvmStatic
  val zonarosaServiceNetworkAccess: ZonaRosaServiceNetworkAccess
    get() = networkModule.zonarosaServiceNetworkAccess

  @JvmStatic
  val protocolStore: ZonaRosaServiceDataStoreImpl
    get() = networkModule.protocolStore

  @JvmStatic
  val zonarosaServiceMessageSender: ZonaRosaServiceMessageSender
    get() = networkModule.zonarosaServiceMessageSender

  @JvmStatic
  val zonarosaServiceAccountManager: ZonaRosaServiceAccountManager
    get() = networkModule.zonarosaServiceAccountManager

  @JvmStatic
  val zonarosaServiceMessageReceiver: ZonaRosaServiceMessageReceiver
    get() = networkModule.zonarosaServiceMessageReceiver

  @JvmStatic
  val incomingMessageObserver: IncomingMessageObserver
    get() = networkModule.incomingMessageObserver

  @JvmStatic
  val libzonarosaNetwork: Network
    get() = networkModule.libzonarosaNetwork

  @JvmStatic
  val authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket
    get() = networkModule.authWebSocket

  @JvmStatic
  val unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket
    get() = networkModule.unauthWebSocket

  @JvmStatic
  val groupsV2Authorization: GroupsV2Authorization
    get() = networkModule.groupsV2Authorization

  @JvmStatic
  val groupsV2Operations: GroupsV2Operations
    get() = networkModule.groupsV2Operations

  @JvmStatic
  val clientZkReceiptOperations
    get() = networkModule.clientZkReceiptOperations

  @JvmStatic
  val payments: Payments
    get() = networkModule.payments

  @JvmStatic
  val profileService: ProfileService
    get() = networkModule.profileService

  @JvmStatic
  val donationsService: DonationsService
    get() = networkModule.donationsService

  @JvmStatic
  val archiveApi: ArchiveApi
    get() = networkModule.archiveApi

  @JvmStatic
  val keysApi: KeysApi
    get() = networkModule.keysApi

  @JvmStatic
  val attachmentApi: AttachmentApi
    get() = networkModule.attachmentApi

  @JvmStatic
  val linkDeviceApi: LinkDeviceApi
    get() = networkModule.linkDeviceApi

  @JvmStatic
  val registrationApi: RegistrationApi
    get() = networkModule.registrationApi

  val storageServiceApi: StorageServiceApi
    get() = networkModule.storageServiceApi

  val accountApi: AccountApi
    get() = networkModule.accountApi

  val usernameApi: UsernameApi
    get() = networkModule.usernameApi

  val svrBApi: SvrBApi
    get() = networkModule.svrBApi

  val callingApi: CallingApi
    get() = networkModule.callingApi

  val paymentsApi: PaymentsApi
    get() = networkModule.paymentsApi

  val cdsApi: CdsApi
    get() = networkModule.cdsApi

  val rateLimitChallengeApi: RateLimitChallengeApi
    get() = networkModule.rateLimitChallengeApi

  val messageApi: MessageApi
    get() = networkModule.messageApi

  val provisioningApi: ProvisioningApi
    get() = networkModule.provisioningApi

  val certificateApi: CertificateApi
    get() = networkModule.certificateApi

  val profileApi: ProfileApi
    get() = networkModule.profileApi

  val remoteConfigApi: RemoteConfigApi
    get() = networkModule.remoteConfigApi

  val donationsApi: DonationsApi
    get() = networkModule.donationsApi

  val keyTransparencyApi: KeyTransparencyApi
    get() = networkModule.keyTransparencyApi

  @JvmStatic
  val okHttpClient: OkHttpClient
    get() = networkModule.okHttpClient

  @JvmStatic
  val zonarosaOkHttpClient: OkHttpClient
    get() = networkModule.zonarosaOkHttpClient

  @JvmStatic
  fun resetProtocolStores() {
    networkModule.resetProtocolStores()
  }

  @JvmStatic
  fun resetNetwork() {
    networkModule.closeConnections()
    _networkModule.reset()
  }

  @JvmStatic
  fun startNetwork() {
    networkModule.openConnections()
  }

  fun onSystemHttpProxyChange(systemHttpProxy: HttpProxy?): Boolean {
    val currentSystemProxy = zonarosaServiceNetworkAccess.getConfiguration().systemHttpProxy.orNull()
    return if (currentSystemProxy?.host != systemHttpProxy?.host || currentSystemProxy?.port != systemHttpProxy?.port) {
      resetNetwork()
      true
    } else {
      false
    }
  }

  interface Provider {
    fun providePushServiceSocket(zonarosaServiceConfiguration: ZonaRosaServiceConfiguration, groupsV2Operations: GroupsV2Operations): PushServiceSocket
    fun provideGroupsV2Operations(zonarosaServiceConfiguration: ZonaRosaServiceConfiguration): GroupsV2Operations
    fun provideZonaRosaServiceAccountManager(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, accountApi: AccountApi, pushServiceSocket: PushServiceSocket, groupsV2Operations: GroupsV2Operations): ZonaRosaServiceAccountManager
    fun provideZonaRosaServiceMessageSender(protocolStore: ZonaRosaServiceDataStore, pushServiceSocket: PushServiceSocket, attachmentApi: AttachmentApi, messageApi: MessageApi, keysApi: KeysApi): ZonaRosaServiceMessageSender
    fun provideZonaRosaServiceMessageReceiver(pushServiceSocket: PushServiceSocket): ZonaRosaServiceMessageReceiver
    fun provideZonaRosaServiceNetworkAccess(): ZonaRosaServiceNetworkAccess
    fun provideRecipientCache(): LiveRecipientCache
    fun provideJobManager(): JobManager
    fun provideFrameRateTracker(): FrameRateTracker
    fun provideMegaphoneRepository(): MegaphoneRepository
    fun provideEarlyMessageCache(): EarlyMessageCache
    fun provideMessageNotifier(): MessageNotifier
    fun provideIncomingMessageObserver(webSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): IncomingMessageObserver
    fun provideTrimThreadsByDateManager(): TrimThreadsByDateManager
    fun provideViewOnceMessageManager(): ViewOnceMessageManager
    fun provideExpiringStoriesManager(): ExpiringStoriesManager
    fun provideExpiringMessageManager(): ExpiringMessageManager
    fun provideDeletedCallEventManager(): DeletedCallEventManager
    fun provideTypingStatusRepository(): TypingStatusRepository
    fun provideTypingStatusSender(): TypingStatusSender
    fun provideDatabaseObserver(): DatabaseObserver
    fun providePayments(paymentsApi: PaymentsApi): Payments
    fun provideShakeToReport(): ShakeToReport
    fun provideZonaRosaCallManager(): ZonaRosaCallManager
    fun providePendingRetryReceiptManager(): PendingRetryReceiptManager
    fun providePendingRetryReceiptCache(): PendingRetryReceiptCache
    fun provideProtocolStore(): ZonaRosaServiceDataStoreImpl
    fun provideGiphyMp4Cache(): GiphyMp4Cache
    fun provideExoPlayerPool(): SimpleExoPlayerPool
    fun provideAndroidCallAudioManager(): AudioManagerCompat
    fun provideDonationsService(donationsApi: DonationsApi): DonationsService
    fun provideProfileService(profileOperations: ClientZkProfileOperations, authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): ProfileService
    fun provideDeadlockDetector(): DeadlockDetector
    fun provideClientZkReceiptOperations(zonarosaServiceConfiguration: ZonaRosaServiceConfiguration): ClientZkReceiptOperations
    fun provideScheduledMessageManager(): ScheduledMessageManager
    fun providePinnedMessageManager(): PinnedMessageManager
    fun provideLibzonarosaNetwork(config: ZonaRosaServiceConfiguration): Network
    fun provideBillingApi(): BillingApi
    fun provideArchiveApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket, pushServiceSocket: PushServiceSocket): ArchiveApi
    fun provideKeysApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): KeysApi
    fun provideAttachmentApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, pushServiceSocket: PushServiceSocket): AttachmentApi
    fun provideLinkDeviceApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): LinkDeviceApi
    fun provideRegistrationApi(pushServiceSocket: PushServiceSocket): RegistrationApi
    fun provideStorageServiceApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, pushServiceSocket: PushServiceSocket): StorageServiceApi
    fun provideAuthWebSocket(zonarosaServiceConfigurationSupplier: Supplier<ZonaRosaServiceConfiguration>, libZonaRosaNetworkSupplier: Supplier<Network>): ZonaRosaWebSocket.AuthenticatedWebSocket
    fun provideUnauthWebSocket(zonarosaServiceConfigurationSupplier: Supplier<ZonaRosaServiceConfiguration>, libZonaRosaNetworkSupplier: Supplier<Network>): ZonaRosaWebSocket.UnauthenticatedWebSocket
    fun provideAccountApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): AccountApi
    fun provideUsernameApi(unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): UsernameApi
    fun provideCallingApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket, pushServiceSocket: PushServiceSocket): CallingApi
    fun providePaymentsApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): PaymentsApi
    fun provideCdsApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): CdsApi
    fun provideRateLimitChallengeApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): RateLimitChallengeApi
    fun provideMessageApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): MessageApi
    fun provideProvisioningApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): ProvisioningApi
    fun provideCertificateApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): CertificateApi
    fun provideProfileApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket, pushServiceSocket: PushServiceSocket, clientZkProfileOperations: ClientZkProfileOperations): ProfileApi
    fun provideRemoteConfigApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, pushServiceSocket: PushServiceSocket): RemoteConfigApi
    fun provideDonationsApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): DonationsApi
    fun provideSvrBApi(libZonaRosaNetwork: Network): SvrBApi
    fun provideKeyTransparencyApi(unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): KeyTransparencyApi
  }
}

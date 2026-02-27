package io.zonarosa.messenger.dependencies

import io.mockk.mockk
import io.zonarosa.core.util.billing.BillingApi
import io.zonarosa.core.util.concurrent.DeadlockDetector
import io.zonarosa.libzonarosa.net.Network
import io.zonarosa.libzonarosa.zkgroup.profiles.ClientZkProfileOperations
import io.zonarosa.libzonarosa.zkgroup.receipts.ClientZkReceiptOperations
import io.zonarosa.messenger.components.TypingStatusRepository
import io.zonarosa.messenger.components.TypingStatusSender
import io.zonarosa.messenger.crypto.storage.ZonaRosaServiceDataStoreImpl
import io.zonarosa.messenger.database.DatabaseObserver
import io.zonarosa.messenger.database.PendingRetryReceiptCache
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
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import io.zonarosa.service.internal.push.PushServiceSocket
import java.util.function.Supplier

class MockApplicationDependencyProvider : AppDependencies.Provider {
  override fun providePushServiceSocket(zonarosaServiceConfiguration: ZonaRosaServiceConfiguration, groupsV2Operations: GroupsV2Operations): PushServiceSocket {
    return mockk(relaxed = true)
  }

  override fun provideGroupsV2Operations(zonarosaServiceConfiguration: ZonaRosaServiceConfiguration): GroupsV2Operations {
    return mockk(relaxed = true)
  }

  override fun provideZonaRosaServiceAccountManager(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, accountApi: AccountApi, pushServiceSocket: PushServiceSocket, groupsV2Operations: GroupsV2Operations): ZonaRosaServiceAccountManager {
    return mockk(relaxed = true)
  }

  override fun provideZonaRosaServiceMessageSender(
    protocolStore: ZonaRosaServiceDataStore,
    pushServiceSocket: PushServiceSocket,
    attachmentApi: AttachmentApi,
    messageApi: MessageApi,
    keysApi: KeysApi
  ): ZonaRosaServiceMessageSender {
    return mockk(relaxed = true)
  }

  override fun provideZonaRosaServiceMessageReceiver(pushServiceSocket: PushServiceSocket): ZonaRosaServiceMessageReceiver {
    return mockk(relaxed = true)
  }

  override fun provideZonaRosaServiceNetworkAccess(): ZonaRosaServiceNetworkAccess {
    return mockk(relaxed = true)
  }

  override fun provideRecipientCache(): LiveRecipientCache {
    return mockk(relaxed = true)
  }

  override fun provideJobManager(): JobManager {
    return mockk(relaxed = true)
  }

  override fun provideFrameRateTracker(): FrameRateTracker {
    return mockk(relaxed = true)
  }

  override fun provideMegaphoneRepository(): MegaphoneRepository {
    return mockk(relaxed = true)
  }

  override fun provideEarlyMessageCache(): EarlyMessageCache {
    return mockk(relaxed = true)
  }

  override fun provideMessageNotifier(): MessageNotifier {
    return mockk(relaxed = true)
  }

  override fun provideIncomingMessageObserver(webSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): IncomingMessageObserver {
    return mockk(relaxed = true)
  }

  override fun provideTrimThreadsByDateManager(): TrimThreadsByDateManager {
    return mockk(relaxed = true)
  }

  override fun provideViewOnceMessageManager(): ViewOnceMessageManager {
    return mockk(relaxed = true)
  }

  override fun provideExpiringStoriesManager(): ExpiringStoriesManager {
    return mockk(relaxed = true)
  }

  override fun provideExpiringMessageManager(): ExpiringMessageManager {
    return mockk(relaxed = true)
  }

  override fun provideDeletedCallEventManager(): DeletedCallEventManager {
    return mockk(relaxed = true)
  }

  override fun provideTypingStatusRepository(): TypingStatusRepository {
    return mockk(relaxed = true)
  }

  override fun provideTypingStatusSender(): TypingStatusSender {
    return mockk(relaxed = true)
  }

  override fun provideDatabaseObserver(): DatabaseObserver {
    return mockk(relaxed = true)
  }

  override fun providePayments(paymentsApi: PaymentsApi): Payments {
    return mockk(relaxed = true)
  }

  override fun provideShakeToReport(): ShakeToReport {
    return mockk(relaxed = true)
  }

  override fun provideZonaRosaCallManager(): ZonaRosaCallManager {
    return mockk(relaxed = true)
  }

  override fun providePendingRetryReceiptManager(): PendingRetryReceiptManager {
    return mockk(relaxed = true)
  }

  override fun providePendingRetryReceiptCache(): PendingRetryReceiptCache {
    return mockk(relaxed = true)
  }

  override fun provideProtocolStore(): ZonaRosaServiceDataStoreImpl {
    return mockk(relaxed = true)
  }

  override fun provideGiphyMp4Cache(): GiphyMp4Cache {
    return mockk(relaxed = true)
  }

  override fun provideExoPlayerPool(): SimpleExoPlayerPool {
    return mockk(relaxed = true)
  }

  override fun provideAndroidCallAudioManager(): AudioManagerCompat {
    return mockk(relaxed = true)
  }

  override fun provideDonationsService(donationsApi: DonationsApi): DonationsService {
    return mockk(relaxed = true)
  }

  override fun provideProfileService(
    profileOperations: ClientZkProfileOperations,
    authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket,
    unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket
  ): ProfileService {
    return mockk(relaxed = true)
  }

  override fun provideDeadlockDetector(): DeadlockDetector {
    return mockk(relaxed = true)
  }

  override fun provideClientZkReceiptOperations(zonarosaServiceConfiguration: ZonaRosaServiceConfiguration): ClientZkReceiptOperations {
    return mockk(relaxed = true)
  }

  override fun provideScheduledMessageManager(): ScheduledMessageManager {
    return mockk(relaxed = true)
  }

  override fun providePinnedMessageManager(): PinnedMessageManager {
    return mockk(relaxed = true)
  }

  override fun provideLibzonarosaNetwork(config: ZonaRosaServiceConfiguration): Network {
    return mockk(relaxed = true)
  }

  override fun provideBillingApi(): BillingApi {
    return mockk(relaxed = true)
  }

  override fun provideArchiveApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket, pushServiceSocket: PushServiceSocket): ArchiveApi {
    return mockk(relaxed = true)
  }

  override fun provideKeysApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): KeysApi {
    return mockk(relaxed = true)
  }

  override fun provideAttachmentApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, pushServiceSocket: PushServiceSocket): AttachmentApi {
    return mockk(relaxed = true)
  }

  override fun provideLinkDeviceApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): LinkDeviceApi {
    return mockk(relaxed = true)
  }

  override fun provideRegistrationApi(pushServiceSocket: PushServiceSocket): RegistrationApi {
    return mockk(relaxed = true)
  }

  override fun provideStorageServiceApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, pushServiceSocket: PushServiceSocket): StorageServiceApi {
    return mockk(relaxed = true)
  }

  override fun provideAuthWebSocket(zonarosaServiceConfigurationSupplier: Supplier<ZonaRosaServiceConfiguration>, libZonaRosaNetworkSupplier: Supplier<Network>): ZonaRosaWebSocket.AuthenticatedWebSocket {
    return mockk(relaxed = true)
  }

  override fun provideUnauthWebSocket(zonarosaServiceConfigurationSupplier: Supplier<ZonaRosaServiceConfiguration>, libZonaRosaNetworkSupplier: Supplier<Network>): ZonaRosaWebSocket.UnauthenticatedWebSocket {
    return mockk(relaxed = true)
  }

  override fun provideAccountApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): AccountApi {
    return mockk(relaxed = true)
  }

  override fun provideUsernameApi(unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): UsernameApi {
    return mockk(relaxed = true)
  }

  override fun provideCallingApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket, pushServiceSocket: PushServiceSocket): CallingApi {
    return mockk(relaxed = true)
  }

  override fun providePaymentsApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): PaymentsApi {
    return mockk(relaxed = true)
  }

  override fun provideCdsApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): CdsApi {
    return mockk(relaxed = true)
  }

  override fun provideRateLimitChallengeApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): RateLimitChallengeApi {
    return mockk(relaxed = true)
  }

  override fun provideMessageApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): MessageApi {
    return mockk(relaxed = true)
  }

  override fun provideProvisioningApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): ProvisioningApi {
    return mockk(relaxed = true)
  }

  override fun provideCertificateApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket): CertificateApi {
    return mockk(relaxed = true)
  }

  override fun provideProfileApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket, pushServiceSocket: PushServiceSocket, clientZkProfileOperations: ClientZkProfileOperations): ProfileApi {
    return mockk(relaxed = true)
  }

  override fun provideRemoteConfigApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, pushServiceSocket: PushServiceSocket): RemoteConfigApi {
    return mockk(relaxed = true)
  }

  override fun provideDonationsApi(authWebSocket: ZonaRosaWebSocket.AuthenticatedWebSocket, unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): DonationsApi {
    return mockk(relaxed = true)
  }

  override fun provideSvrBApi(libZonaRosaNetwork: Network): SvrBApi {
    return mockk(relaxed = true)
  }

  override fun provideKeyTransparencyApi(unauthWebSocket: ZonaRosaWebSocket.UnauthenticatedWebSocket): KeyTransparencyApi {
    return mockk(relaxed = true)
  }
}

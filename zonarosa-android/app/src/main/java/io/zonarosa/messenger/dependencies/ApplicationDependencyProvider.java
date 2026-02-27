package io.zonarosa.messenger.dependencies;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import org.jetbrains.annotations.NotNull;
import io.zonarosa.billing.BillingFactory;
import io.zonarosa.core.util.ThreadUtil;
import io.zonarosa.core.util.billing.BillingApi;
import io.zonarosa.core.util.concurrent.DeadlockDetector;
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.libzonarosa.net.Network;
import io.zonarosa.libzonarosa.zkgroup.profiles.ClientZkProfileOperations;
import io.zonarosa.libzonarosa.zkgroup.receipts.ClientZkReceiptOperations;
import io.zonarosa.messenger.BuildConfig;
import io.zonarosa.messenger.components.TypingStatusRepository;
import io.zonarosa.messenger.components.TypingStatusSender;
import io.zonarosa.messenger.crypto.ReentrantSessionLock;
import io.zonarosa.messenger.crypto.storage.ZonaRosaBaseIdentityKeyStore;
import io.zonarosa.messenger.crypto.storage.ZonaRosaIdentityKeyStore;
import io.zonarosa.messenger.crypto.storage.ZonaRosaKyberPreKeyStore;
import io.zonarosa.messenger.crypto.storage.ZonaRosaSenderKeyStore;
import io.zonarosa.messenger.crypto.storage.ZonaRosaServiceAccountDataStoreImpl;
import io.zonarosa.messenger.crypto.storage.ZonaRosaServiceDataStoreImpl;
import io.zonarosa.messenger.crypto.storage.ZonaRosaPreKeyStore;
import io.zonarosa.messenger.crypto.storage.ZonaRosaSessionStore;
import io.zonarosa.messenger.database.DatabaseObserver;
import io.zonarosa.messenger.database.JobDatabase;
import io.zonarosa.messenger.database.PendingRetryReceiptCache;
import io.zonarosa.messenger.jobmanager.JobManager;
import io.zonarosa.messenger.jobmanager.JobMigrator;
import io.zonarosa.messenger.jobmanager.impl.FactoryJobPredicate;
import io.zonarosa.messenger.jobs.AttachmentCompressionJob;
import io.zonarosa.messenger.jobs.AttachmentUploadJob;
import io.zonarosa.messenger.jobs.FastJobStorage;
import io.zonarosa.messenger.jobs.GroupCallUpdateSendJob;
import io.zonarosa.messenger.jobs.IndividualSendJob;
import io.zonarosa.messenger.jobs.JobManagerFactories;
import io.zonarosa.messenger.jobs.MarkerJob;
import io.zonarosa.messenger.jobs.PreKeysSyncJob;
import io.zonarosa.messenger.jobs.PushGroupSendJob;
import io.zonarosa.messenger.jobs.PushProcessMessageJob;
import io.zonarosa.messenger.jobs.ReactionSendJob;
import io.zonarosa.messenger.jobs.SendDeliveryReceiptJob;
import io.zonarosa.messenger.jobs.TypingSendJob;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.megaphone.MegaphoneRepository;
import io.zonarosa.messenger.messages.IncomingMessageObserver;
import io.zonarosa.messenger.net.DeviceTransferBlockingInterceptor;
import io.zonarosa.messenger.net.ZonaRosaWebSocketHealthMonitor;
import io.zonarosa.messenger.net.StandardUserAgentInterceptor;
import io.zonarosa.messenger.notifications.MessageNotifier;
import io.zonarosa.messenger.notifications.OptimizedMessageNotifier;
import io.zonarosa.messenger.payments.MobileCoinConfig;
import io.zonarosa.messenger.payments.Payments;
import io.zonarosa.messenger.push.SecurityEventListener;
import io.zonarosa.messenger.push.ZonaRosaServiceNetworkAccess;
import io.zonarosa.messenger.recipients.LiveRecipientCache;
import io.zonarosa.messenger.revealable.ViewOnceMessageManager;
import io.zonarosa.messenger.service.DeletedCallEventManager;
import io.zonarosa.messenger.service.ExpiringMessageManager;
import io.zonarosa.messenger.service.ExpiringStoriesManager;
import io.zonarosa.messenger.service.PendingRetryReceiptManager;
import io.zonarosa.messenger.service.PinnedMessageManager;
import io.zonarosa.messenger.service.ScheduledMessageManager;
import io.zonarosa.messenger.service.TrimThreadsByDateManager;
import io.zonarosa.messenger.service.webrtc.ZonaRosaCallManager;
import io.zonarosa.messenger.shakereport.ShakeToReport;
import io.zonarosa.messenger.stories.Stories;
import io.zonarosa.messenger.util.AlarmSleepTimer;
import io.zonarosa.messenger.util.AppForegroundObserver;
import io.zonarosa.messenger.util.ByteUnit;
import io.zonarosa.messenger.util.EarlyMessageCache;
import io.zonarosa.messenger.util.Environment;
import io.zonarosa.messenger.util.FrameRateTracker;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.messenger.video.exo.GiphyMp4Cache;
import io.zonarosa.messenger.video.exo.SimpleExoPlayerPool;
import io.zonarosa.messenger.webrtc.audio.AudioManagerCompat;
import io.zonarosa.service.api.ZonaRosaServiceAccountManager;
import io.zonarosa.service.api.ZonaRosaServiceDataStore;
import io.zonarosa.service.api.ZonaRosaServiceMessageReceiver;
import io.zonarosa.service.api.ZonaRosaServiceMessageSender;
import io.zonarosa.service.api.account.AccountApi;
import io.zonarosa.service.api.archive.ArchiveApi;
import io.zonarosa.service.api.attachment.AttachmentApi;
import io.zonarosa.service.api.calling.CallingApi;
import io.zonarosa.service.api.cds.CdsApi;
import io.zonarosa.service.api.certificate.CertificateApi;
import io.zonarosa.service.api.donations.DonationsApi;
import io.zonarosa.service.api.groupsv2.ClientZkOperations;
import io.zonarosa.service.api.groupsv2.GroupsV2Operations;
import io.zonarosa.service.api.keys.KeysApi;
import io.zonarosa.service.api.link.LinkDeviceApi;
import io.zonarosa.service.api.message.MessageApi;
import io.zonarosa.service.api.payments.PaymentsApi;
import io.zonarosa.service.api.profiles.ProfileApi;
import io.zonarosa.service.api.provisioning.ProvisioningApi;
import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.core.models.ServiceId.PNI;
import io.zonarosa.service.api.ratelimit.RateLimitChallengeApi;
import io.zonarosa.service.api.registration.RegistrationApi;
import io.zonarosa.service.api.remoteconfig.RemoteConfigApi;
import io.zonarosa.service.api.services.DonationsService;
import io.zonarosa.service.api.services.ProfileService;
import io.zonarosa.service.api.storage.StorageServiceApi;
import io.zonarosa.service.api.svr.SvrBApi;
import io.zonarosa.service.api.username.UsernameApi;
import io.zonarosa.service.api.util.CredentialsProvider;
import io.zonarosa.service.api.util.SleepTimer;
import io.zonarosa.service.api.util.UptimeSleepTimer;
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket;
import io.zonarosa.service.api.websocket.WebSocketFactory;
import io.zonarosa.service.api.websocket.WebSocketUnavailableException;
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration;
import io.zonarosa.service.internal.push.PushServiceSocket;
import io.zonarosa.service.internal.websocket.LibZonaRosaChatConnection;
import io.zonarosa.service.internal.websocket.LibZonaRosaNetworkExtensions;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Implementation of {@link AppDependencies.Provider} that provides real app dependencies.
 */
public class ApplicationDependencyProvider implements AppDependencies.Provider {

  private final Application context;

  public ApplicationDependencyProvider(@NonNull Application context) {
    this.context = context;
  }

  private @NonNull ClientZkOperations provideClientZkOperations(@NonNull ZonaRosaServiceConfiguration zonarosaServiceConfiguration) {
    return ClientZkOperations.create(zonarosaServiceConfiguration);
  }

  @Override
  public @NonNull PushServiceSocket providePushServiceSocket(@NonNull ZonaRosaServiceConfiguration zonarosaServiceConfiguration, @NonNull GroupsV2Operations groupsV2Operations) {
    return new PushServiceSocket(zonarosaServiceConfiguration,
                                 new DynamicCredentialsProvider(),
                                 BuildConfig.ZONAROSA_AGENT,
                                 RemoteConfig.okHttpAutomaticRetry());
  }

  @Override
  public @NonNull GroupsV2Operations provideGroupsV2Operations(@NonNull ZonaRosaServiceConfiguration zonarosaServiceConfiguration) {
    return new GroupsV2Operations(provideClientZkOperations(zonarosaServiceConfiguration), RemoteConfig.groupLimits().getHardLimit());
  }

  @Override
  public @NonNull ZonaRosaServiceAccountManager provideZonaRosaServiceAccountManager(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull AccountApi accountApi, @NonNull PushServiceSocket pushServiceSocket, @NonNull GroupsV2Operations groupsV2Operations) {
    return new ZonaRosaServiceAccountManager(authWebSocket, accountApi, pushServiceSocket, groupsV2Operations);
  }

  @Override
  public @NonNull ZonaRosaServiceMessageSender provideZonaRosaServiceMessageSender(@NonNull ZonaRosaServiceDataStore protocolStore,
                                                                               @NonNull PushServiceSocket pushServiceSocket,
                                                                               @NonNull AttachmentApi attachmentApi,
                                                                               @NonNull MessageApi messageApi,
                                                                               @NonNull KeysApi keysApi) {
      return new ZonaRosaServiceMessageSender(pushServiceSocket,
                                            protocolStore,
                                            ReentrantSessionLock.INSTANCE,
                                            attachmentApi,
                                            messageApi,
                                            keysApi,
                                            Optional.of(new SecurityEventListener(context)),
                                            ZonaRosaExecutors.newCachedBoundedExecutor("zonarosa-messages", ThreadUtil.PRIORITY_IMPORTANT_BACKGROUND_THREAD, 1, 16, 30),
                                            RemoteConfig.maxEnvelopeSizeBytes(),
                                            RemoteConfig::useMessageSendRestFallback,
                                            RemoteConfig.useBinaryId(),
                                            BuildConfig.USE_STRING_ID);
  }

  @Override
  public @NonNull ZonaRosaServiceMessageReceiver provideZonaRosaServiceMessageReceiver(@NonNull PushServiceSocket pushServiceSocket) {
    return new ZonaRosaServiceMessageReceiver(pushServiceSocket);
  }

  @Override
  public @NonNull ZonaRosaServiceNetworkAccess provideZonaRosaServiceNetworkAccess() {
    return new ZonaRosaServiceNetworkAccess(context);
  }

  @Override
  public @NonNull LiveRecipientCache provideRecipientCache() {
    return new LiveRecipientCache(context);
  }

  @Override
  public @NonNull JobManager provideJobManager() {
    JobManager.Configuration config = new JobManager.Configuration.Builder()
                                                                  .setJobFactories(JobManagerFactories.getJobFactories(context))
                                                                  .setConstraintFactories(JobManagerFactories.getConstraintFactories(context))
                                                                  .setConstraintObservers(JobManagerFactories.getConstraintObservers(context))
                                                                  .setJobStorage(new FastJobStorage(JobDatabase.getInstance(context)))
                                                                  .setJobMigrator(new JobMigrator(ZonaRosaPreferences.getJobManagerVersion(context), JobManager.CURRENT_VERSION, JobManagerFactories.getJobMigrations(context)))
                                                                  .addReservedJobRunner(new FactoryJobPredicate(PushProcessMessageJob.KEY, MarkerJob.KEY))
                                                                  .addReservedJobRunner(new FactoryJobPredicate(AttachmentUploadJob.KEY, AttachmentCompressionJob.KEY))
                                                                  .addReservedJobRunner(new FactoryJobPredicate(
                                                                      IndividualSendJob.KEY,
                                                                      PushGroupSendJob.KEY,
                                                                      ReactionSendJob.KEY,
                                                                      TypingSendJob.KEY,
                                                                      GroupCallUpdateSendJob.KEY,
                                                                      SendDeliveryReceiptJob.KEY
                                                                  ))
                                                                  .build();
    return new JobManager(context, config);
  }

  @Override
  public @NonNull FrameRateTracker provideFrameRateTracker() {
    return new FrameRateTracker(context);
  }

  @SuppressLint("DiscouragedApi")
  public @NonNull MegaphoneRepository provideMegaphoneRepository() {
    return new MegaphoneRepository(context);
  }

  @Override
  public @NonNull EarlyMessageCache provideEarlyMessageCache() {
    return new EarlyMessageCache();
  }

  @Override
  public @NonNull MessageNotifier provideMessageNotifier() {
    return new OptimizedMessageNotifier(context);
  }

  @Override
  public @NonNull IncomingMessageObserver provideIncomingMessageObserver(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket webSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket) {
    return new IncomingMessageObserver(context, webSocket, unauthWebSocket);
  }

  @Override
  public @NonNull TrimThreadsByDateManager provideTrimThreadsByDateManager() {
    return new TrimThreadsByDateManager(context);
  }

  @Override
  public @NonNull ViewOnceMessageManager provideViewOnceMessageManager() {
    return new ViewOnceMessageManager(context);
  }

  @Override
  public @NonNull ExpiringStoriesManager provideExpiringStoriesManager() {
    return new ExpiringStoriesManager(context);
  }

  @Override
  public @NonNull ExpiringMessageManager provideExpiringMessageManager() {
    return new ExpiringMessageManager(context);
  }

  @Override
  public @NonNull DeletedCallEventManager provideDeletedCallEventManager() {
    return new DeletedCallEventManager(context);
  }

  @Override
  public @NonNull ScheduledMessageManager provideScheduledMessageManager() {
    return new ScheduledMessageManager(context);
  }

  @Override
  public @NonNull PinnedMessageManager providePinnedMessageManager() {
    return new PinnedMessageManager(context);
  }

  @Override
  public @NonNull Network provideLibzonarosaNetwork(@NonNull ZonaRosaServiceConfiguration config) {
    Network network = new Network(BuildConfig.LIBZONAROSA_NET_ENV, StandardUserAgentInterceptor.USER_AGENT);
    LibZonaRosaNetworkExtensions.applyConfiguration(network, config);
    network.setRemoteConfig(RemoteConfig.getLibzonarosaConfigs());

    return network;
  }

  @Override
  public @NonNull TypingStatusRepository provideTypingStatusRepository() {
    return new TypingStatusRepository();
  }

  @Override
  public @NonNull TypingStatusSender provideTypingStatusSender() {
    return new TypingStatusSender();
  }

  @Override
  public @NonNull DatabaseObserver provideDatabaseObserver() {
    return new DatabaseObserver();
  }

  @SuppressWarnings("ConstantConditions")
  @Override
  public @NonNull Payments providePayments(@NonNull PaymentsApi paymentsApi) {
    MobileCoinConfig network;

    if      (BuildConfig.MOBILE_COIN_ENVIRONMENT.equals("mainnet")) network = MobileCoinConfig.getMainNet(paymentsApi);
    else if (BuildConfig.MOBILE_COIN_ENVIRONMENT.equals("testnet")) network = MobileCoinConfig.getTestNet(paymentsApi);
    else throw new AssertionError("Unknown network " + BuildConfig.MOBILE_COIN_ENVIRONMENT);

    return new Payments(network);
  }

  @Override
  public @NonNull ShakeToReport provideShakeToReport() {
    return new ShakeToReport(context);
  }

  @Override
  public @NonNull ZonaRosaCallManager provideZonaRosaCallManager() {
    return new ZonaRosaCallManager(context);
  }

  @Override
  public @NonNull PendingRetryReceiptManager providePendingRetryReceiptManager() {
    return new PendingRetryReceiptManager(context);
  }

  @Override
  public @NonNull PendingRetryReceiptCache providePendingRetryReceiptCache() {
    return new PendingRetryReceiptCache();
  }

  @Override
  public @NonNull ZonaRosaWebSocket.AuthenticatedWebSocket provideAuthWebSocket(@NonNull Supplier<ZonaRosaServiceConfiguration> zonarosaServiceConfigurationSupplier, @NonNull Supplier<Network> libZonaRosaNetworkSupplier) {
    SleepTimer                   sleepTimer    = !ZonaRosaStore.account().isFcmEnabled() || ZonaRosaStore.internal().isWebsocketModeForced() ? new AlarmSleepTimer(context) : new UptimeSleepTimer();
    ZonaRosaWebSocketHealthMonitor healthMonitor = new ZonaRosaWebSocketHealthMonitor(sleepTimer);

    WebSocketFactory authFactory = () -> {
      DynamicCredentialsProvider credentialsProvider = new DynamicCredentialsProvider();

      if (credentialsProvider.isInvalid()) {
        throw new WebSocketUnavailableException("Invalid auth credentials");
      }

      Network network = libZonaRosaNetworkSupplier.get();
      return new LibZonaRosaChatConnection("libzonarosa-auth",
                                         network,
                                         credentialsProvider,
                                         Stories.isFeatureEnabled(),
                                         healthMonitor);
    };

    ZonaRosaWebSocket.AuthenticatedWebSocket webSocket = new ZonaRosaWebSocket.AuthenticatedWebSocket(authFactory,
                                                                                                  () -> !ZonaRosaStore.misc().isClientDeprecated() && !DeviceTransferBlockingInterceptor.getInstance().isBlockingNetwork() && !Environment.IS_INSTRUMENTATION,
                                                                                                  sleepTimer,
                                                                                                  TimeUnit.SECONDS.toMillis(30));
    if (AppForegroundObserver.isForegrounded()) {
      webSocket.registerKeepAliveToken(ZonaRosaWebSocket.FOREGROUND_KEEPALIVE);
    }

    healthMonitor.monitor(webSocket);

    return webSocket;
  }

  @Override
  public @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket provideUnauthWebSocket(@NonNull Supplier<ZonaRosaServiceConfiguration> zonarosaServiceConfigurationSupplier, @NonNull Supplier<Network> libZonaRosaNetworkSupplier) {
    SleepTimer                   sleepTimer    = !ZonaRosaStore.account().isFcmEnabled() || ZonaRosaStore.internal().isWebsocketModeForced() ? new AlarmSleepTimer(context) : new UptimeSleepTimer();
    ZonaRosaWebSocketHealthMonitor healthMonitor = new ZonaRosaWebSocketHealthMonitor(sleepTimer);

    WebSocketFactory unauthFactory = () -> {
      Network network = libZonaRosaNetworkSupplier.get();
      return new LibZonaRosaChatConnection("libzonarosa-unauth",
                                         network,
                                         null,
                                         Stories.isFeatureEnabled(),
                                         healthMonitor);
    };

    ZonaRosaWebSocket.UnauthenticatedWebSocket webSocket = new ZonaRosaWebSocket.UnauthenticatedWebSocket(unauthFactory,
                                                                                                      () -> !ZonaRosaStore.misc().isClientDeprecated() && !DeviceTransferBlockingInterceptor.getInstance().isBlockingNetwork() && !Environment.IS_INSTRUMENTATION,
                                                                                                      sleepTimer,
                                                                                                      TimeUnit.SECONDS.toMillis(30));
    if (AppForegroundObserver.isForegrounded()) {
      webSocket.registerKeepAliveToken(ZonaRosaWebSocket.FOREGROUND_KEEPALIVE);
    }

    healthMonitor.monitor(webSocket);
    return webSocket;
  }

  @Override
  public @NonNull ZonaRosaServiceDataStoreImpl provideProtocolStore() {
    ACI localAci = ZonaRosaStore.account().getAci();
    PNI localPni = ZonaRosaStore.account().getPni();

    if (localAci == null) {
      throw new IllegalStateException("No ACI set!");
    }

    if (localPni == null) {
      throw new IllegalStateException("No PNI set!");
    }

    boolean needsPreKeyJob = false;

    if (!ZonaRosaStore.account().hasAciIdentityKey()) {
      ZonaRosaStore.account().generateAciIdentityKeyIfNecessary();
      needsPreKeyJob = true;
    }

    if (!ZonaRosaStore.account().hasPniIdentityKey()) {
      ZonaRosaStore.account().generatePniIdentityKeyIfNecessary();
      needsPreKeyJob = true;
    }

    if (needsPreKeyJob) {
      PreKeysSyncJob.enqueueIfNeeded();
    }

    ZonaRosaBaseIdentityKeyStore baseIdentityStore = new ZonaRosaBaseIdentityKeyStore(context);

    ZonaRosaServiceAccountDataStoreImpl aciStore = new ZonaRosaServiceAccountDataStoreImpl(context,
                                                                                       new ZonaRosaPreKeyStore(localAci),
                                                                                       new ZonaRosaKyberPreKeyStore(localAci),
                                                                                       new ZonaRosaIdentityKeyStore(baseIdentityStore, () -> ZonaRosaStore.account().getAciIdentityKey()),
                                                                                       new ZonaRosaSessionStore(localAci),
                                                                                       new ZonaRosaSenderKeyStore(context));

    ZonaRosaServiceAccountDataStoreImpl pniStore = new ZonaRosaServiceAccountDataStoreImpl(context,
                                                                                       new ZonaRosaPreKeyStore(localPni),
                                                                                       new ZonaRosaKyberPreKeyStore(localPni),
                                                                                       new ZonaRosaIdentityKeyStore(baseIdentityStore, () -> ZonaRosaStore.account().getPniIdentityKey()),
                                                                                       new ZonaRosaSessionStore(localPni),
                                                                                       new ZonaRosaSenderKeyStore(context));
    return new ZonaRosaServiceDataStoreImpl(context, aciStore, pniStore);
  }

  @Override
  public @NonNull GiphyMp4Cache provideGiphyMp4Cache() {
    return new GiphyMp4Cache(ByteUnit.MEGABYTES.toBytes(16));
  }

  @Override
  public @NonNull SimpleExoPlayerPool provideExoPlayerPool() {
    return new SimpleExoPlayerPool(context);
  }

  @Override
  public @NonNull AudioManagerCompat provideAndroidCallAudioManager() {
    return AudioManagerCompat.create(context);
  }

  @Override
  public @NonNull DonationsService provideDonationsService(@NonNull DonationsApi donationsApi) {
    return new DonationsService(donationsApi);
  }

  @Override
  public @NonNull ProfileService provideProfileService(@NonNull ClientZkProfileOperations clientZkProfileOperations,
                                                       @NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket,
                                                       @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket)
  {
    return new ProfileService(clientZkProfileOperations, authWebSocket, unauthWebSocket);
  }

  @Override
  public @NonNull DeadlockDetector provideDeadlockDetector() {
    HandlerThread handlerThread = new HandlerThread("zonarosa-DeadlockDetector", ThreadUtil.PRIORITY_BACKGROUND_THREAD);
    handlerThread.start();
    return new DeadlockDetector(new Handler(handlerThread.getLooper()), TimeUnit.SECONDS.toMillis(5));
  }

  @Override
  public @NonNull ClientZkReceiptOperations provideClientZkReceiptOperations(@NonNull ZonaRosaServiceConfiguration zonarosaServiceConfiguration) {
    return provideClientZkOperations(zonarosaServiceConfiguration).getReceiptOperations();
  }

  @Override
  public @NonNull BillingApi provideBillingApi() {
    return BillingFactory.create(GooglePlayBillingDependencies.INSTANCE, Environment.Backups.supportsGooglePlayBilling());
  }

  @Override
  public @NonNull ArchiveApi provideArchiveApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket, @NonNull PushServiceSocket pushServiceSocket) {
    return new ArchiveApi(authWebSocket, unauthWebSocket, pushServiceSocket);
  }

  @Override
  public @NonNull KeysApi provideKeysApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket) {
    return new KeysApi(authWebSocket, unauthWebSocket);
  }

  @Override
  public @NonNull AttachmentApi provideAttachmentApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull PushServiceSocket pushServiceSocket) {
    return new AttachmentApi(authWebSocket, pushServiceSocket);
  }

  @Override
  public @NonNull LinkDeviceApi provideLinkDeviceApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket) {
    return new LinkDeviceApi(authWebSocket);
  }

  @Override
  public @NonNull RegistrationApi provideRegistrationApi(@NonNull PushServiceSocket pushServiceSocket) {
    return new RegistrationApi(pushServiceSocket);
  }

  @Override
  public @NonNull StorageServiceApi provideStorageServiceApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull PushServiceSocket pushServiceSocket) {
    return new StorageServiceApi(authWebSocket, pushServiceSocket);
  }

  @Override
  public @NonNull AccountApi provideAccountApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket) {
    return new AccountApi(authWebSocket);
  }

  @Override
  public @NonNull UsernameApi provideUsernameApi(@NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket) {
    return new UsernameApi(unauthWebSocket);
  }

  @Override
  public @NonNull CallingApi provideCallingApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket, @NonNull PushServiceSocket pushServiceSocket) {
    return new CallingApi(authWebSocket, unauthWebSocket, pushServiceSocket);
  }

  @Override
  public @NonNull PaymentsApi providePaymentsApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket) {
    return new PaymentsApi(authWebSocket);
  }

  @Override
  public @NonNull CdsApi provideCdsApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket) {
    return new CdsApi(authWebSocket);
  }

  @Override
  public @NonNull RateLimitChallengeApi provideRateLimitChallengeApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket) {
    return new RateLimitChallengeApi(authWebSocket);
  }

  @Override
  public @NonNull MessageApi provideMessageApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket) {
    return new MessageApi(authWebSocket, unauthWebSocket);
  }

  @Override
  public @NonNull ProvisioningApi provideProvisioningApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket) {
    return new ProvisioningApi(authWebSocket, unauthWebSocket);
  }

  @Override
  public @NonNull CertificateApi provideCertificateApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket) {
    return new CertificateApi(authWebSocket);
  }

  @Override
  public @NonNull ProfileApi provideProfileApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket, @NonNull PushServiceSocket pushServiceSocket, @NonNull ClientZkProfileOperations clientZkProfileOperations) {
    return new ProfileApi(authWebSocket, unauthWebSocket, pushServiceSocket, clientZkProfileOperations);
  }

  @Override
  public @NonNull RemoteConfigApi provideRemoteConfigApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull PushServiceSocket pushServiceSocket) {
    return new RemoteConfigApi(authWebSocket, pushServiceSocket);
  }

  @Override
  public @NonNull DonationsApi provideDonationsApi(@NonNull ZonaRosaWebSocket.AuthenticatedWebSocket authWebSocket, @NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket) {
    return new DonationsApi(authWebSocket, unauthWebSocket);
  }

  @Override
  public @NonNull SvrBApi provideSvrBApi(@NotNull Network libZonaRosaNetwork) {
    return new SvrBApi(libZonaRosaNetwork);
  }

  @Override
  public @NonNull KeyTransparencyApi provideKeyTransparencyApi(@NonNull ZonaRosaWebSocket.UnauthenticatedWebSocket unauthWebSocket) {
    return new KeyTransparencyApi(unauthWebSocket);
  }

  @VisibleForTesting
  static class DynamicCredentialsProvider implements CredentialsProvider {

    @Override
    public ACI getAci() {
      return ZonaRosaStore.account().getAci();
    }

    @Override
    public PNI getPni() {
      return ZonaRosaStore.account().getPni();
    }

    @Override
    public String getE164() {
      return ZonaRosaStore.account().getE164();
    }

    @Override
    public String getPassword() {
      return ZonaRosaStore.account().getServicePassword();
    }

    @Override
    public int getDeviceId() {
      return ZonaRosaStore.account().getDeviceId();
    }
  }
}

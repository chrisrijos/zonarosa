/*
 * Copyright 2013 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.server;

import static java.util.Objects.requireNonNull;
import static io.zonarosa.server.metrics.MetricsUtil.name;

import com.google.common.collect.Lists;
import com.webauthn4j.appattest.DeviceCheckManager;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.basic.BasicCredentialAuthFilter;
import io.dropwizard.auth.basic.BasicCredentials;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.core.Application;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.lettuce.core.metrics.MicrometerCommandLatencyRecorder;
import io.lettuce.core.metrics.MicrometerOptions;
import io.lettuce.core.resource.ClientResources;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.resolver.ResolvedAddressTypes;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletRegistration;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.stream.Stream;
import org.eclipse.jetty.websocket.core.WebSocketExtensionRegistry;
import org.eclipse.jetty.websocket.core.server.WebSocketServerComponents;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.glassfish.jersey.server.ServerProperties;
import io.zonarosa.i18n.HeaderControlledResourceBundleLookup;
import io.zonarosa.libzonarosa.zkgroup.GenericServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.ServerSecretParams;
import io.zonarosa.libzonarosa.zkgroup.auth.ServerZkAuthOperations;
import io.zonarosa.libzonarosa.zkgroup.profiles.ServerZkProfileOperations;
import io.zonarosa.libzonarosa.zkgroup.receipts.ReceiptCredentialPresentation;
import io.zonarosa.libzonarosa.zkgroup.receipts.ServerZkReceiptOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.zonarosa.server.asn.AsnInfoProvider;
import io.zonarosa.server.asn.AsnInfoProviderImpl;
import io.zonarosa.server.attachments.GcsAttachmentGenerator;
import io.zonarosa.server.attachments.TusAttachmentGenerator;
import io.zonarosa.server.auth.AccountAuthenticator;
import io.zonarosa.server.auth.AuthenticatedDevice;
import io.zonarosa.server.auth.CertificateGenerator;
import io.zonarosa.server.auth.CloudflareTurnCredentialsManager;
import io.zonarosa.server.auth.DisconnectionRequestManager;
import io.zonarosa.server.auth.ExternalServiceCredentialsGenerator;
import io.zonarosa.server.auth.IdlePrimaryDeviceAuthenticatedWebSocketUpgradeFilter;
import io.zonarosa.server.auth.PhoneVerificationTokenManager;
import io.zonarosa.server.auth.RegistrationLockVerificationManager;
import io.zonarosa.server.auth.grpc.ProhibitAuthenticationInterceptor;
import io.zonarosa.server.auth.grpc.RequireAuthenticationInterceptor;
import io.zonarosa.server.backup.BackupAuthManager;
import io.zonarosa.server.backup.BackupManager;
import io.zonarosa.server.backup.BackupsDb;
import io.zonarosa.server.backup.Cdn3BackupCredentialGenerator;
import io.zonarosa.server.backup.Cdn3RemoteStorageManager;
import io.zonarosa.server.backup.SecureValueRecoveryBCredentialsGeneratorFactory;
import io.zonarosa.server.badges.ConfiguredProfileBadgeConverter;
import io.zonarosa.server.captcha.CaptchaChecker;
import io.zonarosa.server.captcha.CaptchaClient;
import io.zonarosa.server.captcha.RegistrationCaptchaManager;
import io.zonarosa.server.captcha.ShortCodeExpander;
import io.zonarosa.server.configuration.dynamic.DynamicConfiguration;
import io.zonarosa.server.configuration.secrets.SecretStore;
import io.zonarosa.server.configuration.secrets.SecretsModule;
import io.zonarosa.server.controllers.AccountController;
import io.zonarosa.server.controllers.AccountControllerV2;
import io.zonarosa.server.controllers.ArchiveController;
import io.zonarosa.server.controllers.AttachmentControllerV4;
import io.zonarosa.server.controllers.CallLinkController;
import io.zonarosa.server.controllers.CallQualitySurveyController;
import io.zonarosa.server.controllers.CallRoutingControllerV2;
import io.zonarosa.server.controllers.CertificateController;
import io.zonarosa.server.controllers.ChallengeController;
import io.zonarosa.server.controllers.DeviceCheckController;
import io.zonarosa.server.controllers.DeviceController;
import io.zonarosa.server.controllers.DirectoryV2Controller;
import io.zonarosa.server.controllers.DonationController;
import io.zonarosa.server.controllers.KeepAliveController;
import io.zonarosa.server.controllers.KeyTransparencyController;
import io.zonarosa.server.controllers.KeysController;
import io.zonarosa.server.controllers.MessageController;
import io.zonarosa.server.controllers.OneTimeDonationController;
import io.zonarosa.server.controllers.PaymentsController;
import io.zonarosa.server.controllers.ProfileController;
import io.zonarosa.server.controllers.ProvisioningController;
import io.zonarosa.server.controllers.RegistrationController;
import io.zonarosa.server.controllers.RemoteConfigController;
import io.zonarosa.server.controllers.SecureStorageController;
import io.zonarosa.server.controllers.SecureValueRecovery2Controller;
import io.zonarosa.server.controllers.StickerController;
import io.zonarosa.server.controllers.SubscriptionController;
import io.zonarosa.server.controllers.VerificationController;
import io.zonarosa.server.currency.CoinGeckoClient;
import io.zonarosa.server.currency.CurrencyConversionManager;
import io.zonarosa.server.currency.FixerClient;
import io.zonarosa.server.experiment.ExperimentEnrollmentManager;
import io.zonarosa.server.filters.ExternalRequestFilter;
import io.zonarosa.server.filters.RemoteAddressFilter;
import io.zonarosa.server.filters.RemoteDeprecationFilter;
import io.zonarosa.server.filters.RequestStatisticsFilter;
import io.zonarosa.server.filters.RestDeprecationFilter;
import io.zonarosa.server.filters.TimestampResponseFilter;
import io.zonarosa.server.grpc.AccountsAnonymousGrpcService;
import io.zonarosa.server.grpc.AccountsGrpcService;
import io.zonarosa.server.grpc.CallQualitySurveyGrpcService;
import io.zonarosa.server.grpc.ErrorConformanceInterceptor;
import io.zonarosa.server.grpc.GrpcAllowListInterceptor;
import io.zonarosa.server.grpc.ErrorMappingInterceptor;
import io.zonarosa.server.grpc.ExternalServiceCredentialsAnonymousGrpcService;
import io.zonarosa.server.grpc.ExternalServiceCredentialsGrpcService;
import io.zonarosa.server.grpc.KeysAnonymousGrpcService;
import io.zonarosa.server.grpc.KeysGrpcService;
import io.zonarosa.server.grpc.MetricServerInterceptor;
import io.zonarosa.server.grpc.PaymentsGrpcService;
import io.zonarosa.server.grpc.ProfileAnonymousGrpcService;
import io.zonarosa.server.grpc.ProfileGrpcService;
import io.zonarosa.server.grpc.RequestAttributesInterceptor;
import io.zonarosa.server.grpc.ValidatingInterceptor;
import io.zonarosa.server.grpc.net.ManagedGrpcServer;
import io.zonarosa.server.grpc.net.ManagedNioEventLoopGroup;
import io.zonarosa.server.jetty.JettyHttpConfigurationCustomizer;
import io.zonarosa.server.keytransparency.KeyTransparencyServiceClient;
import io.zonarosa.server.limits.CardinalityEstimator;
import io.zonarosa.server.limits.MessageDeliveryLoopMonitor;
import io.zonarosa.server.limits.NoopMessageDeliveryLoopMonitor;
import io.zonarosa.server.limits.PushChallengeManager;
import io.zonarosa.server.limits.RateLimitByIpFilter;
import io.zonarosa.server.limits.RateLimitChallengeManager;
import io.zonarosa.server.limits.RateLimiters;
import io.zonarosa.server.limits.RedisMessageDeliveryLoopMonitor;
import io.zonarosa.server.mappers.BackupExceptionMapper;
import io.zonarosa.server.mappers.CompletionExceptionMapper;
import io.zonarosa.server.mappers.DeviceLimitExceededExceptionMapper;
import io.zonarosa.server.mappers.GrpcStatusRuntimeExceptionMapper;
import io.zonarosa.server.mappers.IOExceptionMapper;
import io.zonarosa.server.mappers.ImpossiblePhoneNumberExceptionMapper;
import io.zonarosa.server.mappers.InvalidWebsocketAddressExceptionMapper;
import io.zonarosa.server.mappers.JsonMappingExceptionMapper;
import io.zonarosa.server.mappers.NonNormalizedPhoneNumberExceptionMapper;
import io.zonarosa.server.mappers.ObsoletePhoneNumberFormatExceptionMapper;
import io.zonarosa.server.mappers.RateLimitExceededExceptionMapper;
import io.zonarosa.server.mappers.RegistrationServiceSenderExceptionMapper;
import io.zonarosa.server.mappers.ServerRejectedExceptionMapper;
import io.zonarosa.server.mappers.SubscriptionExceptionMapper;
import io.zonarosa.server.metrics.BackupMetrics;
import io.zonarosa.server.metrics.CallQualitySurveyManager;
import io.zonarosa.server.metrics.MessageMetrics;
import io.zonarosa.server.metrics.MetricsApplicationEventListener;
import io.zonarosa.server.metrics.MetricsHttpChannelListener;
import io.zonarosa.server.metrics.MetricsUtil;
import io.zonarosa.server.metrics.MicrometerAwsSdkMetricPublisher;
import io.zonarosa.server.metrics.ReportedMessageMetricsListener;
import io.zonarosa.server.metrics.TlsCertificateExpirationUtil;
import io.zonarosa.server.metrics.TrafficSource;
import io.zonarosa.server.providers.MultiRecipientMessageProvider;
import io.zonarosa.server.push.APNSender;
import io.zonarosa.server.push.FcmSender;
import io.zonarosa.server.push.MessageSender;
import io.zonarosa.server.push.ProvisioningManager;
import io.zonarosa.server.push.PushNotificationManager;
import io.zonarosa.server.push.PushNotificationScheduler;
import io.zonarosa.server.push.ReceiptSender;
import io.zonarosa.server.push.RedisMessageAvailabilityManager;
import io.zonarosa.server.redis.ConnectionEventLogger;
import io.zonarosa.server.redis.FaultTolerantRedisClient;
import io.zonarosa.server.redis.FaultTolerantRedisClusterClient;
import io.zonarosa.server.registration.RegistrationServiceClient;
import io.zonarosa.server.s3.PolicySigner;
import io.zonarosa.server.s3.PostPolicyGenerator;
import io.zonarosa.server.s3.S3MonitoringSupplier;
import io.zonarosa.server.securestorage.SecureStorageClient;
import io.zonarosa.server.securevaluerecovery.SecureValueRecoveryClient;
import io.zonarosa.server.spam.ChallengeConstraintChecker;
import io.zonarosa.server.spam.RegistrationFraudChecker;
import io.zonarosa.server.spam.RegistrationRecoveryChecker;
import io.zonarosa.server.spam.SpamChecker;
import io.zonarosa.server.spam.SpamFilter;
import io.zonarosa.server.storage.AccountLockManager;
import io.zonarosa.server.storage.Accounts;
import io.zonarosa.server.storage.AccountsManager;
import io.zonarosa.server.storage.ChangeNumberManager;
import io.zonarosa.server.storage.ClientPublicKeys;
import io.zonarosa.server.storage.ClientPublicKeysManager;
import io.zonarosa.server.storage.ClientReleaseManager;
import io.zonarosa.server.storage.ClientReleases;
import io.zonarosa.server.storage.DynamicConfigurationManager;
import io.zonarosa.server.storage.IssuedReceiptsManager;
import io.zonarosa.server.storage.KeysManager;
import io.zonarosa.server.storage.MessagesCache;
import io.zonarosa.server.storage.MessagesDynamoDb;
import io.zonarosa.server.storage.MessagesManager;
import io.zonarosa.server.storage.OneTimeDonationsManager;
import io.zonarosa.server.storage.PagedSingleUseKEMPreKeyStore;
import io.zonarosa.server.storage.PersistentTimer;
import io.zonarosa.server.storage.PhoneNumberIdentifiers;
import io.zonarosa.server.storage.Profiles;
import io.zonarosa.server.storage.ProfilesManager;
import io.zonarosa.server.storage.PushChallengeDynamoDb;
import io.zonarosa.server.storage.RedeemedReceiptsManager;
import io.zonarosa.server.storage.RegistrationRecoveryPasswords;
import io.zonarosa.server.storage.RegistrationRecoveryPasswordsManager;
import io.zonarosa.server.storage.RemoteConfigs;
import io.zonarosa.server.storage.RemoteConfigsManager;
import io.zonarosa.server.storage.RepeatedUseECSignedPreKeyStore;
import io.zonarosa.server.storage.RepeatedUseKEMSignedPreKeyStore;
import io.zonarosa.server.storage.ReportMessageDynamoDb;
import io.zonarosa.server.storage.ReportMessageManager;
import io.zonarosa.server.storage.SingleUseECPreKeyStore;
import io.zonarosa.server.storage.SubscriptionManager;
import io.zonarosa.server.storage.Subscriptions;
import io.zonarosa.server.storage.VerificationSessionManager;
import io.zonarosa.server.storage.VerificationSessions;
import io.zonarosa.server.storage.devicecheck.AppleDeviceCheckManager;
import io.zonarosa.server.storage.devicecheck.AppleDeviceCheckTrustAnchor;
import io.zonarosa.server.storage.devicecheck.AppleDeviceChecks;
import io.zonarosa.server.subscriptions.AppleAppStoreClient;
import io.zonarosa.server.subscriptions.AppleAppStoreManager;
import io.zonarosa.server.subscriptions.BankMandateTranslator;
import io.zonarosa.server.subscriptions.BraintreeManager;
import io.zonarosa.server.subscriptions.PayPalDonationsTranslator;
import io.zonarosa.server.subscriptions.GooglePlayBillingManager;
import io.zonarosa.server.subscriptions.StripeManager;
import io.zonarosa.server.telephony.CarrierDataProvider;
import io.zonarosa.server.telephony.hlrlookup.HlrLookupCarrierDataProvider;
import io.zonarosa.server.util.BufferingInterceptor;
import io.zonarosa.server.util.ManagedAwsCrt;
import io.zonarosa.server.util.ManagedExecutors;
import io.zonarosa.server.util.ResilienceUtil;
import io.zonarosa.server.util.SystemMapper;
import io.zonarosa.server.util.UsernameHashZkProofVerifier;
import io.zonarosa.server.util.VirtualExecutorServiceProvider;
import io.zonarosa.server.util.VirtualThreadPinEventMonitor;
import io.zonarosa.server.util.logging.LoggingUnhandledExceptionMapper;
import io.zonarosa.server.util.logging.UncaughtExceptionHandler;
import io.zonarosa.server.websocket.AuthenticatedConnectListener;
import io.zonarosa.server.websocket.NoContextTakeoverPerMessageDeflateExtension;
import io.zonarosa.server.websocket.ProvisioningConnectListener;
import io.zonarosa.server.websocket.WebSocketAccountAuthenticator;
import io.zonarosa.server.workers.BackupMetricsCommand;
import io.zonarosa.server.workers.BackupUsageRecalculationCommand;
import io.zonarosa.server.workers.CertificateCommand;
import io.zonarosa.server.workers.CheckDynamicConfigurationCommand;
import io.zonarosa.server.workers.ClearIssuedReceiptRedemptionsCommand;
import io.zonarosa.server.workers.DeleteUserCommand;
import io.zonarosa.server.workers.IdleDeviceNotificationSchedulerFactory;
import io.zonarosa.server.workers.MessagePersisterServiceCommand;
import io.zonarosa.server.workers.NotifyIdleDevicesCommand;
import io.zonarosa.server.workers.ProcessScheduledJobsServiceCommand;
import io.zonarosa.server.workers.RegenerateSecondaryDynamoDbTableDataCommand;
import io.zonarosa.server.workers.RemoveExpiredAccountsCommand;
import io.zonarosa.server.workers.RemoveExpiredBackupsCommand;
import io.zonarosa.server.workers.RemoveExpiredLinkedDevicesCommand;
import io.zonarosa.server.workers.RemoveExpiredUsernameHoldsCommand;
import io.zonarosa.server.workers.RemoveOrphanedPreKeyPagesCommand;
import io.zonarosa.server.workers.ScheduledApnPushNotificationSenderServiceCommand;
import io.zonarosa.server.workers.ServerVersionCommand;
import io.zonarosa.server.workers.SetRequestLoggingEnabledTask;
import io.zonarosa.server.workers.SetUserDiscoverabilityCommand;
import io.zonarosa.server.workers.UnlinkDeviceCommand;
import io.zonarosa.server.workers.UnlinkDevicesWithIdlePrimaryCommand;
import io.zonarosa.server.workers.ZkParamsCommand;
import io.zonarosa.websocket.WebSocketResourceProviderFactory;
import io.zonarosa.websocket.setup.WebSocketEnvironment;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;

public class WhisperServerService extends Application<WhisperServerConfiguration> {

  private static final Logger log = LoggerFactory.getLogger(WhisperServerService.class);

  public static final String SECRETS_BUNDLE_FILE_NAME_PROPERTY = "secrets.bundle.filename";

  @Override
  public void initialize(final Bootstrap<WhisperServerConfiguration> bootstrap) {
    // `SecretStore` needs to be initialized before Dropwizard reads the main application config file.
    final String secretsBundleFileName = requireNonNull(
        System.getProperty(SECRETS_BUNDLE_FILE_NAME_PROPERTY),
        "Application requires property [%s] to be provided".formatted(SECRETS_BUNDLE_FILE_NAME_PROPERTY));
    final SecretStore secretStore = SecretStore.fromYamlFileSecretsBundle(secretsBundleFileName);
    SecretsModule.INSTANCE.setSecretStore(secretStore);

    // Initializing SystemMapper here because parsing of the main application config happens before `run()` method is called.
    SystemMapper.configureMapper(bootstrap.getObjectMapper());

    // Enable variable substitution with environment variables
    // https://www.dropwizard.io/en/stable/manual/core.html#environment-variables
    final EnvironmentVariableSubstitutor substitutor = new EnvironmentVariableSubstitutor(true);
    final SubstitutingSourceProvider provider =
        new SubstitutingSourceProvider(bootstrap.getConfigurationSourceProvider(), substitutor);
    bootstrap.setConfigurationSourceProvider(provider);

    bootstrap.addCommand(new DeleteUserCommand());
    bootstrap.addCommand(new CertificateCommand());
    bootstrap.addCommand(new ZkParamsCommand());
    bootstrap.addCommand(new ServerVersionCommand());
    bootstrap.addCommand(new CheckDynamicConfigurationCommand());
    bootstrap.addCommand(new SetUserDiscoverabilityCommand());
    bootstrap.addCommand(new UnlinkDeviceCommand());
    bootstrap.addCommand(new ScheduledApnPushNotificationSenderServiceCommand());
    bootstrap.addCommand(new MessagePersisterServiceCommand());
    bootstrap.addCommand(new RemoveExpiredAccountsCommand(Clock.systemUTC()));
    bootstrap.addCommand(new RemoveExpiredUsernameHoldsCommand(Clock.systemUTC()));
    bootstrap.addCommand(new RemoveExpiredBackupsCommand(Clock.systemUTC()));
    bootstrap.addCommand(new RemoveOrphanedPreKeyPagesCommand(Clock.systemUTC()));
    bootstrap.addCommand(new BackupMetricsCommand(Clock.systemUTC()));
    bootstrap.addCommand(new BackupUsageRecalculationCommand());
    bootstrap.addCommand(new RemoveExpiredLinkedDevicesCommand());
    bootstrap.addCommand(new UnlinkDevicesWithIdlePrimaryCommand(Clock.systemUTC()));
    bootstrap.addCommand(new NotifyIdleDevicesCommand());
    bootstrap.addCommand(new ClearIssuedReceiptRedemptionsCommand());

    bootstrap.addCommand(new ProcessScheduledJobsServiceCommand("process-idle-device-notification-jobs",
        "Processes scheduled jobs to send notifications to idle devices",
        new IdleDeviceNotificationSchedulerFactory()));

    bootstrap.addCommand(new RegenerateSecondaryDynamoDbTableDataCommand());

    ServiceLoader.load(SpamFilter.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .flatMap(spamFilter -> spamFilter.getCommands().stream())
        .forEach(bootstrap::addCommand);
  }

  @Override
  public String getName() {
    return "zonarosa-server";
  }

  @Override
  public void run(WhisperServerConfiguration config, Environment environment) throws Exception {
    final Clock clock = Clock.systemUTC();
    final int availableProcessors = Runtime.getRuntime().availableProcessors();

    final AwsCredentialsProvider awsCredentialsProvider = config.getAwsCredentialsConfiguration().build();

    UncaughtExceptionHandler.register();

    config.getCircuitBreakerConfigurations().forEach((name, configuration) ->
        ResilienceUtil.getCircuitBreakerRegistry().addConfiguration(name, configuration.toCircuitBreakerConfig()));

    config.getRetryConfigurations().forEach((name, configuration) ->
        ResilienceUtil.getRetryRegistry().addConfiguration(name, configuration.toRetryConfigBuilder().build()));

    ResilienceUtil.setGeneralRedisRetryConfiguration(config.getGeneralRedisRetryConfiguration());

    ScheduledExecutorService dynamicConfigurationExecutor = ScheduledExecutorServiceBuilder.of(environment, "dynamicConfiguration")
        .threads(1).build();

    DynamicConfigurationManager<DynamicConfiguration> dynamicConfigurationManager =
        new DynamicConfigurationManager<>(
            config.getDynamicConfig().build(awsCredentialsProvider, dynamicConfigurationExecutor), DynamicConfiguration.class);
    dynamicConfigurationManager.start();

    MetricsUtil.configureRegistries(config, environment, dynamicConfigurationManager);
    MetricsUtil.configureLogging(config, environment);

    ExperimentEnrollmentManager experimentEnrollmentManager = new ExperimentEnrollmentManager(dynamicConfigurationManager);

    if (config.getServerFactory() instanceof DefaultServerFactory defaultServerFactory) {
      defaultServerFactory.getApplicationConnectors()
          .forEach(connectorFactory -> {
            if (connectorFactory instanceof HttpsConnectorFactory h) {
              h.setKeyStorePassword(config.getTlsKeyStoreConfiguration().password().value());

              TlsCertificateExpirationUtil.configureMetrics(h.getKeyStorePath(), h.getKeyStorePassword(), h.getKeyStoreType(), h.getKeyStoreProvider());
            }
          });
    }

    environment.lifecycle().addEventListener(new JettyHttpConfigurationCustomizer());

    HeaderControlledResourceBundleLookup headerControlledResourceBundleLookup =
        new HeaderControlledResourceBundleLookup();
    ConfiguredProfileBadgeConverter profileBadgeConverter = new ConfiguredProfileBadgeConverter(
        clock, config.getBadges(), headerControlledResourceBundleLookup);
    BankMandateTranslator bankMandateTranslator = new BankMandateTranslator(headerControlledResourceBundleLookup);
    PayPalDonationsTranslator payPalDonationsTranslator =
        new PayPalDonationsTranslator(headerControlledResourceBundleLookup);

    environment.lifecycle().manage(new ManagedAwsCrt());

    final ExecutorService awsSdkMetricsExecutor = ManagedExecutors.newVirtualThreadPerTaskExecutor(
        "awsSdkMetrics",
        config.getVirtualThreadConfiguration().maxConcurrentThreadsPerExecutor(),
        environment);

    final DynamoDbAsyncClient dynamoDbAsyncClient = config.getDynamoDbClientConfiguration()
        .buildAsyncClient(awsCredentialsProvider, new MicrometerAwsSdkMetricPublisher(awsSdkMetricsExecutor, "dynamoDbAsync"));

    final DynamoDbClient dynamoDbClient = config.getDynamoDbClientConfiguration()
        .buildSyncClient(awsCredentialsProvider, new MicrometerAwsSdkMetricPublisher(awsSdkMetricsExecutor, "dynamoDbSync"));

    final AwsCredentialsProvider cdnCredentialsProvider = config.getCdnConfiguration().credentials().build();
    final S3AsyncClient asyncCdnS3Client = S3AsyncClient.builder()
        .credentialsProvider(cdnCredentialsProvider)
        .region(Region.of(config.getCdnConfiguration().region()))
        .endpointOverride(config.getCdnConfiguration().endpointOverride())
        .build();

    BlockingQueue<Runnable> messageDeletionQueue = new LinkedBlockingQueue<>();
    Metrics.gaugeCollectionSize(name(getClass(), "messageDeletionQueueSize"), Collections.emptyList(),
        messageDeletionQueue);
    ExecutorService messageDeletionAsyncExecutor = ExecutorServiceBuilder.of(environment, "messageDeletionAsyncExecutor")
        .minThreads(2)
        .maxThreads(2)
        .allowCoreThreadTimeOut(true)
        .workQueue(messageDeletionQueue).build();

    Accounts accounts = new Accounts(
        clock,
        dynamoDbClient,
        dynamoDbAsyncClient,
        config.getDynamoDbTables().getAccounts().getTableName(),
        config.getDynamoDbTables().getAccounts().getPhoneNumberTableName(),
        config.getDynamoDbTables().getAccounts().getPhoneNumberIdentifierTableName(),
        config.getDynamoDbTables().getAccounts().getUsernamesTableName(),
        config.getDynamoDbTables().getDeletedAccounts().getTableName(),
        config.getDynamoDbTables().getAccounts().getUsedLinkDeviceTokensTableName());
    ClientReleases clientReleases = new ClientReleases(dynamoDbAsyncClient,
        config.getDynamoDbTables().getClientReleases().getTableName());
    PhoneNumberIdentifiers phoneNumberIdentifiers = new PhoneNumberIdentifiers(dynamoDbAsyncClient,
        config.getDynamoDbTables().getPhoneNumberIdentifiers().getTableName());
    Profiles profiles = new Profiles(dynamoDbClient, dynamoDbAsyncClient,
        config.getDynamoDbTables().getProfiles().getTableName());

    S3AsyncClient asyncKeysS3Client = S3AsyncClient.builder()
        .credentialsProvider(awsCredentialsProvider)
        .region(Region.of(config.getPagedSingleUseKEMPreKeyStore().region()))
        .endpointOverride(config.getPagedSingleUseKEMPreKeyStore().endpointOverride())
        .build();
    KeysManager keysManager = new KeysManager(
        new SingleUseECPreKeyStore(dynamoDbAsyncClient, config.getDynamoDbTables().getEcKeys().getTableName()),
        new PagedSingleUseKEMPreKeyStore(
            dynamoDbAsyncClient,
            asyncKeysS3Client,
            config.getDynamoDbTables().getPagedKemKeys().getTableName(),
            config.getPagedSingleUseKEMPreKeyStore().bucket()),
        new RepeatedUseECSignedPreKeyStore(dynamoDbAsyncClient, config.getDynamoDbTables().getEcSignedPreKeys().getTableName()),
        new RepeatedUseKEMSignedPreKeyStore(dynamoDbAsyncClient, config.getDynamoDbTables().getKemLastResortKeys().getTableName()));
    MessagesDynamoDb messagesDynamoDb = new MessagesDynamoDb(dynamoDbClient, dynamoDbAsyncClient,
        config.getDynamoDbTables().getMessages().getTableName(),
        config.getDynamoDbTables().getMessages().getExpiration(),
        messageDeletionAsyncExecutor, experimentEnrollmentManager);
    RemoteConfigs remoteConfigs = new RemoteConfigs(dynamoDbClient,
        config.getDynamoDbTables().getRemoteConfig().getTableName());
    PushChallengeDynamoDb pushChallengeDynamoDb = new PushChallengeDynamoDb(dynamoDbClient,
        config.getDynamoDbTables().getPushChallenge().getTableName());
    ReportMessageDynamoDb reportMessageDynamoDb = new ReportMessageDynamoDb(dynamoDbClient, dynamoDbAsyncClient,
        config.getDynamoDbTables().getReportMessage().getTableName(),
        config.getReportMessageConfiguration().getReportTtl());
    RegistrationRecoveryPasswords registrationRecoveryPasswords = new RegistrationRecoveryPasswords(
        config.getDynamoDbTables().getRegistrationRecovery().getTableName(),
        config.getDynamoDbTables().getRegistrationRecovery().getExpiration(),
        dynamoDbAsyncClient,
        clock);
    ClientPublicKeys clientPublicKeys =
        new ClientPublicKeys(dynamoDbAsyncClient, config.getDynamoDbTables().getClientPublicKeys().getTableName());

    final VerificationSessions verificationSessions = new VerificationSessions(dynamoDbAsyncClient,
        config.getDynamoDbTables().getVerificationSessions().getTableName(), clock);

    final ClientResources sharedClientResources = ClientResources.builder()
        .commandLatencyRecorder(
            new MicrometerCommandLatencyRecorder(Metrics.globalRegistry, MicrometerOptions.builder().build()))
        .build();
    ConnectionEventLogger.logConnectionEvents(sharedClientResources);

    FaultTolerantRedisClusterClient cacheCluster = config.getCacheClusterConfiguration()
        .build("main_cache", sharedClientResources.mutate());
    FaultTolerantRedisClusterClient messagesCluster =
        config.getMessageCacheConfiguration().getRedisClusterConfiguration()
            .build("messages", sharedClientResources.mutate());
    FaultTolerantRedisClusterClient pushSchedulerCluster = config.getPushSchedulerCluster().build("push_scheduler",
        sharedClientResources.mutate());
    FaultTolerantRedisClusterClient rateLimitersCluster = config.getRateLimitersCluster().build("rate_limiters",
        sharedClientResources.mutate());

    FaultTolerantRedisClient pubsubClient =
        config.getRedisPubSubConfiguration().build("pubsub", sharedClientResources);

    final BlockingQueue<Runnable> receiptSenderQueue = new LinkedBlockingQueue<>();
    Metrics.gaugeCollectionSize(name(getClass(), "receiptSenderQueue"), Collections.emptyList(), receiptSenderQueue);
    final BlockingQueue<Runnable> fcmSenderQueue = new LinkedBlockingQueue<>();
    Metrics.gaugeCollectionSize(name(getClass(), "fcmSenderQueue"), Collections.emptyList(), fcmSenderQueue);
    final BlockingQueue<Runnable> messageDeliveryQueue = new LinkedBlockingQueue<>();
    Metrics.gaugeCollectionSize(MetricsUtil.name(getClass(), "messageDeliveryQueue"), Collections.emptyList(),
        messageDeliveryQueue);

    ScheduledExecutorService recurringJobExecutor = ScheduledExecutorServiceBuilder.of(environment, "recurringJob").threads(6).build();
    ExecutorService apnSenderExecutor = ExecutorServiceBuilder.of(environment, "apnSender")
        .maxThreads(1).minThreads(1).build();
    ExecutorService fcmSenderExecutor = ExecutorServiceBuilder.of(environment, "fcmSender")
        .maxThreads(32).minThreads(32).workQueue(fcmSenderQueue).build();
    ExecutorService secureValueRecoveryServiceExecutor = ExecutorServiceBuilder.of(environment, "secureValueRecoveryService")
        .maxThreads(1).minThreads(1).build();
    ExecutorService storageServiceExecutor = ExecutorServiceBuilder.of(environment, "storageService")
        .maxThreads(1).minThreads(1).build();
    ExecutorService virtualThreadEventLoggerExecutor = ExecutorServiceBuilder.of(environment, "virtualThreadEventLogger")
        .minThreads(1).maxThreads(1).build();
    ExecutorService asyncOperationQueueingExecutor = ExecutorServiceBuilder.of(environment, "asyncOperationQueueing")
        .minThreads(1).maxThreads(1).build();

    final ScheduledExecutorService retryExecutor = ScheduledExecutorServiceBuilder.of(environment, "retry")
        .threads(16).build();
    final ScheduledExecutorService registrationIdentityTokenRefreshExecutor =
      ScheduledExecutorServiceBuilder.of(environment, "registrationIdentityTokenRefresh").threads(1).build();

    Scheduler messageDeliveryScheduler = Schedulers.fromExecutorService(
        ExecutorServiceBuilder.of(environment, "messageDelivery")
            .minThreads(20)
            .maxThreads(20)
            .workQueue(messageDeliveryQueue)
            .build(),
        "messageDelivery");

    // TODO: generally speaking this is a DynamoDB I/O executor for the accounts table; we should eventually have a general executor for speaking to the accounts table, but most of the server is still synchronous so this isn't widely useful yet
    ExecutorService batchIdentityCheckExecutor = ExecutorServiceBuilder.of(environment, "batchIdentityCheck").minThreads(32).maxThreads(32).build();

    ExecutorService receiptSenderExecutor = ExecutorServiceBuilder.of(environment, "receiptSender")
        .maxThreads(2)
        .minThreads(2)
        .workQueue(receiptSenderQueue)
        .rejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy())
        .build();
    ExecutorService registrationCallbackExecutor = ExecutorServiceBuilder.of(environment, "registration")
        .maxThreads(2)
        .minThreads(2)
        .build();
    ExecutorService accountLockExecutor = ExecutorServiceBuilder.of(environment, "accountLock")
        .minThreads(8)
        .maxThreads(8)
        .build();
    // unbounded executor (same as cachedThreadPool)
    ExecutorService remoteStorageHttpExecutor = ExecutorServiceBuilder.of(environment, "remoteStorage")
        .minThreads(0)
        .maxThreads(Integer.MAX_VALUE)
        .workQueue(new SynchronousQueue<>())
        .keepAliveTime(io.dropwizard.util.Duration.seconds(60L))
        .build();
    ExecutorService cloudflareTurnHttpExecutor = ExecutorServiceBuilder.of(environment, "cloudflareTurn")
        .maxThreads(2)
        .minThreads(2)
        .build();
    ExecutorService hlrLookupHttpExecutor = ExecutorServiceBuilder.of(environment, "hlrLookup")
        .maxThreads(2)
        .minThreads(2)
        .build();

    ExecutorService subscriptionProcessorExecutor = ManagedExecutors.newVirtualThreadPerTaskExecutor(
        "subscriptionProcessor",
        config.getVirtualThreadConfiguration().maxConcurrentThreadsPerExecutor(),
        environment);
    ExecutorService clientEventExecutor = ManagedExecutors.newVirtualThreadPerTaskExecutor(
        "clientEvent",
        config.getVirtualThreadConfiguration().maxConcurrentThreadsPerExecutor(),
        environment);
    ExecutorService disconnectionRequestListenerExecutor = ManagedExecutors.newVirtualThreadPerTaskExecutor(
        "disconnectionRequest",
        config.getVirtualThreadConfiguration().maxConcurrentThreadsPerExecutor(),
        environment);
    ExecutorService callQualitySurveyPubSubExecutor = ManagedExecutors.newVirtualThreadPerTaskExecutor(
        "callQualitySurvey",
        config.getVirtualThreadConfiguration().maxConcurrentThreadsPerExecutor(),
        environment);

    ScheduledExecutorService cloudflareTurnRetryExecutor = ScheduledExecutorServiceBuilder.of(environment, "cloudflareTurnRetry").threads(1).build();
    ScheduledExecutorService messagePollExecutor = ScheduledExecutorServiceBuilder.of(environment, "messagePollExecutor").threads(1).build();
    ScheduledExecutorService provisioningWebsocketTimeoutExecutor = ScheduledExecutorServiceBuilder.of(environment, "provisioningWebsocketTimeout").threads(1).build();
    ScheduledExecutorService jmxDumper = ScheduledExecutorServiceBuilder.of(environment, "jmxDumper").threads(1).build();

    final ManagedNioEventLoopGroup dnsResolutionEventLoopGroup = new ManagedNioEventLoopGroup();
    final DnsNameResolver cloudflareDnsResolver = new DnsNameResolverBuilder(dnsResolutionEventLoopGroup.next())
            .resolvedAddressTypes(ResolvedAddressTypes.IPV6_PREFERRED)
            .completeOncePreferredResolved(false)
            .channelType(NioDatagramChannel.class)
            .socketChannelType(NioSocketChannel.class)
            .build();

    ExternalServiceCredentialsGenerator directoryV2CredentialsGenerator = DirectoryV2Controller.credentialsGenerator(
        config.getDirectoryV2Configuration().getDirectoryV2ClientConfiguration());
    ExternalServiceCredentialsGenerator storageCredentialsGenerator = SecureStorageController.credentialsGenerator(
        config.getSecureStorageServiceConfiguration());
    ExternalServiceCredentialsGenerator paymentsCredentialsGenerator = PaymentsController.credentialsGenerator(
        config.getPaymentsServiceConfiguration());
    ExternalServiceCredentialsGenerator svr2CredentialsGenerator = SecureValueRecovery2Controller.credentialsGenerator(
        config.getSvr2Configuration());
    ExternalServiceCredentialsGenerator svrbCredentialsGenerator =
        SecureValueRecoveryBCredentialsGeneratorFactory.svrbCredentialsGenerator(config.getSvrbConfiguration());

    final S3MonitoringSupplier<AsnInfoProvider> asnInfoProviderSupplier = new S3MonitoringSupplier<>(
        recurringJobExecutor,
        awsCredentialsProvider,
        config.getAsnTableConfiguration(),
        AsnInfoProviderImpl::fromTsvGz,
        AsnInfoProvider.EMPTY,
        "AsnManager");

    RegistrationRecoveryPasswordsManager registrationRecoveryPasswordsManager =
        new RegistrationRecoveryPasswordsManager(registrationRecoveryPasswords);
    UsernameHashZkProofVerifier usernameHashZkProofVerifier = new UsernameHashZkProofVerifier();

    final CarrierDataProvider carrierDataProvider =
        new HlrLookupCarrierDataProvider(config.getHlrLookupConfiguration().apiKey().value(),
            config.getHlrLookupConfiguration().apiSecret().value(),
            hlrLookupHttpExecutor,
            config.getHlrLookupConfiguration().circuitBreakerConfigurationName(),
            config.getHlrLookupConfiguration().retryConfigurationName(),
            retryExecutor);

    RegistrationServiceClient registrationServiceClient = config.getRegistrationServiceConfiguration()
        .build(environment, registrationCallbackExecutor, registrationIdentityTokenRefreshExecutor);
    KeyTransparencyServiceClient keyTransparencyServiceClient = new KeyTransparencyServiceClient(
        config.getKeyTransparencyServiceConfiguration().host(),
        config.getKeyTransparencyServiceConfiguration().port(),
        config.getKeyTransparencyServiceConfiguration().tlsCertificate(),
        config.getKeyTransparencyServiceConfiguration().clientCertificate(),
        config.getKeyTransparencyServiceConfiguration().clientPrivateKey().value());
    SecureValueRecoveryClient secureValueRecovery2Client = new SecureValueRecoveryClient(
        svr2CredentialsGenerator,
        secureValueRecoveryServiceExecutor,
        retryExecutor,
        config.getSvr2Configuration(),
        () -> dynamicConfigurationManager.getConfiguration().getSvr2StatusCodesToIgnoreForAccountDeletion());
    SecureValueRecoveryClient secureValueRecoveryBClient = new SecureValueRecoveryClient(
        svrbCredentialsGenerator,
        secureValueRecoveryServiceExecutor,
        retryExecutor,
        config.getSvrbConfiguration(),
        () -> dynamicConfigurationManager.getConfiguration().getSvrbStatusCodesToIgnoreForAccountDeletion());
    SecureStorageClient secureStorageClient = new SecureStorageClient(storageCredentialsGenerator,
        storageServiceExecutor, retryExecutor, config.getSecureStorageServiceConfiguration());
    DisconnectionRequestManager disconnectionRequestManager = new DisconnectionRequestManager(pubsubClient,
        disconnectionRequestListenerExecutor, retryExecutor);
    ProfilesManager profilesManager = new ProfilesManager(profiles, cacheCluster, retryExecutor, asyncCdnS3Client,
        config.getCdnConfiguration().bucket());
    MessagesCache messagesCache = new MessagesCache(messagesCluster, messageDeliveryScheduler,
        messageDeletionAsyncExecutor, retryExecutor, clock, experimentEnrollmentManager);
    ClientReleaseManager clientReleaseManager = new ClientReleaseManager(clientReleases,
        recurringJobExecutor,
        config.getClientReleaseConfiguration().refreshInterval(),
        Clock.systemUTC());
    ReportMessageManager reportMessageManager = new ReportMessageManager(reportMessageDynamoDb, rateLimitersCluster,
        config.getReportMessageConfiguration().getCounterTtl());
    RedisMessageAvailabilityManager redisMessageAvailabilityManager =
        new RedisMessageAvailabilityManager(messagesCluster, clientEventExecutor, asyncOperationQueueingExecutor);
    MessagesManager messagesManager = new MessagesManager(messagesDynamoDb, messagesCache, redisMessageAvailabilityManager,
        reportMessageManager, messageDeletionAsyncExecutor, Clock.systemUTC());
    AccountLockManager accountLockManager = new AccountLockManager(dynamoDbClient,
        config.getDynamoDbTables().getDeletedAccountsLock().getTableName());
    ClientPublicKeysManager clientPublicKeysManager =
        new ClientPublicKeysManager(clientPublicKeys, accountLockManager, accountLockExecutor);
    AccountsManager accountsManager = new AccountsManager(accounts, phoneNumberIdentifiers, cacheCluster,
        pubsubClient, accountLockManager, keysManager, messagesManager, profilesManager,
        secureStorageClient, secureValueRecovery2Client, disconnectionRequestManager,
        registrationRecoveryPasswordsManager, clientPublicKeysManager, accountLockExecutor, messagePollExecutor,
        retryExecutor, clock, config.getLinkDeviceSecretConfiguration().secret().value(), dynamicConfigurationManager);
    RemoteConfigsManager remoteConfigsManager = new RemoteConfigsManager(remoteConfigs);
    APNSender apnSender = new APNSender(apnSenderExecutor, config.getApnConfiguration());
    FcmSender fcmSender = new FcmSender(fcmSenderExecutor, config.getFcmConfiguration().credentials().value());
    PushNotificationScheduler pushNotificationScheduler = new PushNotificationScheduler(pushSchedulerCluster,
        apnSender, fcmSender, accountsManager, 0, 0, retryExecutor);
    PushNotificationManager pushNotificationManager =
        new PushNotificationManager(accountsManager, apnSender, fcmSender, pushNotificationScheduler);
    RateLimiters rateLimiters = RateLimiters.create(dynamicConfigurationManager, rateLimitersCluster, retryExecutor);
    ProvisioningManager provisioningManager = new ProvisioningManager(pubsubClient);
    IssuedReceiptsManager issuedReceiptsManager = new IssuedReceiptsManager(
        config.getDynamoDbTables().getIssuedReceipts().getTableName(),
        config.getDynamoDbTables().getIssuedReceipts().getExpiration(),
        dynamoDbAsyncClient,
        config.getDynamoDbTables().getIssuedReceipts().getGenerator(),
        config.getDynamoDbTables().getIssuedReceipts().getmaxIssuedReceiptsPerPaymentId());
    OneTimeDonationsManager oneTimeDonationsManager = new OneTimeDonationsManager(
        config.getDynamoDbTables().getOnetimeDonations().getTableName(), config.getDynamoDbTables().getOnetimeDonations().getExpiration(), dynamoDbAsyncClient);
    RedeemedReceiptsManager redeemedReceiptsManager = new RedeemedReceiptsManager(clock,
        config.getDynamoDbTables().getRedeemedReceipts().getTableName(),
        dynamoDbAsyncClient,
        config.getDynamoDbTables().getRedeemedReceipts().getExpiration());
    Subscriptions subscriptions = new Subscriptions(
        config.getDynamoDbTables().getSubscriptions().getTableName(), dynamoDbAsyncClient);
    MessageDeliveryLoopMonitor messageDeliveryLoopMonitor =
        config.logMessageDeliveryLoops() ? new RedisMessageDeliveryLoopMonitor(rateLimitersCluster) : new NoopMessageDeliveryLoopMonitor();
    CallQualitySurveyManager callQualitySurveyManager = new CallQualitySurveyManager(asnInfoProviderSupplier,
        config.getCallQualitySurveyConfiguration().pubSubPublisher().build(),
        Clock.systemUTC(),
        callQualitySurveyPubSubExecutor);

    final RegistrationLockVerificationManager registrationLockVerificationManager = new RegistrationLockVerificationManager(
        accountsManager, disconnectionRequestManager, svr2CredentialsGenerator, registrationRecoveryPasswordsManager,
        pushNotificationManager, rateLimiters);

    final ReportedMessageMetricsListener reportedMessageMetricsListener = new ReportedMessageMetricsListener(
        accountsManager);
    reportMessageManager.addListener(reportedMessageMetricsListener);

    final AccountAuthenticator accountAuthenticator = new AccountAuthenticator(accountsManager);

    final MessageSender messageSender = new MessageSender(messagesManager, pushNotificationManager);
    final ReceiptSender receiptSender = new ReceiptSender(accountsManager, messageSender, receiptSenderExecutor);
    final CloudflareTurnCredentialsManager cloudflareTurnCredentialsManager = new CloudflareTurnCredentialsManager(
        config.getTurnConfiguration().cloudflare().apiToken().value(),
        config.getTurnConfiguration().cloudflare().endpoint(),
        config.getTurnConfiguration().cloudflare().requestedCredentialTtl(),
        config.getTurnConfiguration().cloudflare().clientCredentialTtl(),
        config.getTurnConfiguration().cloudflare().urls(),
        config.getTurnConfiguration().cloudflare().urlsWithIps(),
        config.getTurnConfiguration().cloudflare().hostname(),
        config.getTurnConfiguration().cloudflare().numHttpClients(),
        config.getTurnConfiguration().cloudflare().circuitBreakerConfigurationName(),
        cloudflareTurnHttpExecutor,
        config.getTurnConfiguration().cloudflare().retryConfigurationName(),
        cloudflareTurnRetryExecutor,
        cloudflareDnsResolver
        );

    final CardinalityEstimator messageByteLimitCardinalityEstimator = new CardinalityEstimator(
        rateLimitersCluster,
        "message_byte_limit",
        config.getMessageByteLimitCardinalityEstimator().period());

    PushChallengeManager pushChallengeManager = new PushChallengeManager(pushNotificationManager,
        pushChallengeDynamoDb);

    ChangeNumberManager changeNumberManager = new ChangeNumberManager(messageSender, accountsManager, Clock.systemUTC());

    HttpClient currencyClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2).connectTimeout(Duration.ofSeconds(10)).build();
    FixerClient fixerClient = config.getPaymentsServiceConfiguration().externalClients()
        .buildFixerClient(currencyClient);
    CoinGeckoClient coinGeckoClient = config.getPaymentsServiceConfiguration().externalClients()
        .buildCoinGeckoClient(currencyClient);
    CurrencyConversionManager currencyManager = new CurrencyConversionManager(fixerClient, coinGeckoClient,
        cacheCluster, config.getPaymentsServiceConfiguration().paymentCurrencies(), recurringJobExecutor, Clock.systemUTC());
    VirtualThreadPinEventMonitor virtualThreadPinEventMonitor = new VirtualThreadPinEventMonitor(
        virtualThreadEventLoggerExecutor,
        config.getVirtualThreadConfiguration().pinEventThreshold());

    StripeManager stripeManager = new StripeManager(config.getStripe().apiKey().value(), subscriptionProcessorExecutor,
        config.getStripe().idempotencyKeyGenerator().value(), config.getStripe().boostDescription(), config.getStripe().supportedCurrenciesByPaymentMethod());
    BraintreeManager braintreeManager = new BraintreeManager(config.getBraintree().merchantId(),
        config.getBraintree().publicKey().value(), config.getBraintree().privateKey().value(),
        config.getBraintree().environment(),
        config.getBraintree().supportedCurrenciesByPaymentMethod(), config.getBraintree().merchantAccounts(),
        config.getBraintree().graphqlUrl(), currencyManager, config.getBraintree().pubSubPublisher().build(),
        config.getBraintree().circuitBreakerConfigurationName(), subscriptionProcessorExecutor);
    GooglePlayBillingManager googlePlayBillingManager = new GooglePlayBillingManager(
        new ByteArrayInputStream(config.getGooglePlayBilling().credentialsJson().getBytes(StandardCharsets.UTF_8)),
        config.getGooglePlayBilling().packageName(),
        config.getGooglePlayBilling().applicationName(),
        config.getGooglePlayBilling().productIdToLevel());
    AppleAppStoreManager appleAppStoreManager = new AppleAppStoreManager(
        new AppleAppStoreClient(
            config.getAppleAppStore().env(),
            config.getAppleAppStore().bundleId(),
            config.getAppleAppStore().appAppleId(),
            config.getAppleAppStore().issuerId(),
            config.getAppleAppStore().keyId(),
            config.getAppleAppStore().encodedKey().value(),
            config.getAppleAppStore().appleRootCerts(),
            config.getAppleAppStore().retryConfigurationName()),
        config.getAppleAppStore().subscriptionGroupId(),
        config.getAppleAppStore().productIdToLevel());

    environment.lifecycle().manage(asnInfoProviderSupplier);

    environment.lifecycle().manage(apnSender);
    environment.lifecycle().manage(pushNotificationScheduler);
    environment.lifecycle().manage(provisioningManager);
    environment.lifecycle().manage(disconnectionRequestManager);
    environment.lifecycle().manage(redisMessageAvailabilityManager);
    environment.lifecycle().manage(currencyManager);
    environment.lifecycle().manage(registrationServiceClient);
    environment.lifecycle().manage(keyTransparencyServiceClient);
    environment.lifecycle().manage(clientReleaseManager);
    environment.lifecycle().manage(virtualThreadPinEventMonitor);
    environment.lifecycle().manage(accountsManager);

    final GcsAttachmentGenerator gcsAttachmentGenerator = new GcsAttachmentGenerator(
        config.getGcpAttachmentsConfiguration().domain(),
        config.getGcpAttachmentsConfiguration().email(),
        config.getGcpAttachmentsConfiguration().maxSizeInBytes(),
        config.getGcpAttachmentsConfiguration().pathPrefix(),
        config.getGcpAttachmentsConfiguration().rsaSigningKey().value());

    PostPolicyGenerator profileCdnPolicyGenerator = new PostPolicyGenerator(config.getCdnConfiguration().region(),
        config.getCdnConfiguration().bucket(), config.getCdnConfiguration().credentials().accessKeyId().value());
    PolicySigner profileCdnPolicySigner = new PolicySigner(
        config.getCdnConfiguration().credentials().secretAccessKey().value(),
        config.getCdnConfiguration().region());

    ServerSecretParams zkSecretParams = new ServerSecretParams(config.getZkConfig().serverSecret().value());
    GenericServerSecretParams callingGenericZkSecretParams = new GenericServerSecretParams(config.getCallingZkConfig().serverSecret().value());
    GenericServerSecretParams backupsGenericZkSecretParams = new GenericServerSecretParams(config.getBackupsZkConfig().serverSecret().value());
    ServerZkProfileOperations zkProfileOperations = new ServerZkProfileOperations(zkSecretParams);
    ServerZkAuthOperations zkAuthOperations = new ServerZkAuthOperations(zkSecretParams);
    ServerZkReceiptOperations zkReceiptOperations = new ServerZkReceiptOperations(zkSecretParams);

    TusAttachmentGenerator tusAttachmentGenerator = new TusAttachmentGenerator(config.getTus());
    Cdn3BackupCredentialGenerator cdn3BackupCredentialGenerator = new Cdn3BackupCredentialGenerator(config.getTus());
    BackupAuthManager backupAuthManager = new BackupAuthManager(experimentEnrollmentManager, rateLimiters,
        accountsManager, zkReceiptOperations, redeemedReceiptsManager, backupsGenericZkSecretParams, clock);
    BackupsDb backupsDb = new BackupsDb(
        dynamoDbAsyncClient,
        config.getDynamoDbTables().getBackups().getTableName(),
        clock);
    final Cdn3RemoteStorageManager cdn3RemoteStorageManager = new Cdn3RemoteStorageManager(
        remoteStorageHttpExecutor,
        retryExecutor,
        config.getCdn3StorageManagerConfiguration());
    BackupManager backupManager = new BackupManager(
        backupsDb,
        backupsGenericZkSecretParams,
        rateLimiters,
        tusAttachmentGenerator,
        cdn3BackupCredentialGenerator,
        cdn3RemoteStorageManager,
        svrbCredentialsGenerator,
        secureValueRecoveryBClient,
        clock,
        dynamicConfigurationManager);

    final AppleDeviceChecks appleDeviceChecks = new AppleDeviceChecks(
        dynamoDbClient,
        DeviceCheckManager.createObjectConverter(),
        config.getDynamoDbTables().getAppleDeviceChecks().getTableName(),
        config.getDynamoDbTables().getAppleDeviceCheckPublicKeys().getTableName());
    final DeviceCheckManager deviceCheckManager = new DeviceCheckManager(new AppleDeviceCheckTrustAnchor());
    deviceCheckManager.getAttestationDataValidator().setProduction(config.getAppleDeviceCheck().production());
    final AppleDeviceCheckManager appleDeviceCheckManager = new AppleDeviceCheckManager(
        appleDeviceChecks,
        cacheCluster,
        deviceCheckManager,
        config.getAppleDeviceCheck().teamId(),
        config.getAppleDeviceCheck().bundleId());

    final RemoteDeprecationFilter remoteDeprecationFilter = new RemoteDeprecationFilter(dynamicConfigurationManager);
    final MetricServerInterceptor metricServerInterceptor = new MetricServerInterceptor(Metrics.globalRegistry, clientReleaseManager);

    final ErrorMappingInterceptor errorMappingInterceptor = new ErrorMappingInterceptor();
    final ErrorConformanceInterceptor errorConformanceInterceptor = new ErrorConformanceInterceptor();
    final GrpcAllowListInterceptor grpcAllowListInterceptor = new GrpcAllowListInterceptor(dynamicConfigurationManager);
    final RequestAttributesInterceptor requestAttributesInterceptor = new RequestAttributesInterceptor();

    final ValidatingInterceptor validatingInterceptor = new ValidatingInterceptor();

    final ExternalRequestFilter grpcExternalRequestFilter = new ExternalRequestFilter(
        config.getExternalRequestFilterConfiguration().permittedInternalRanges(),
        config.getExternalRequestFilterConfiguration().grpcMethods());
    final RequireAuthenticationInterceptor requireAuthenticationInterceptor = new RequireAuthenticationInterceptor(accountAuthenticator);
    final ProhibitAuthenticationInterceptor prohibitAuthenticationInterceptor = new ProhibitAuthenticationInterceptor();

    final List<ServerServiceDefinition> authenticatedServices = Stream.of(
        new AccountsGrpcService(accountsManager, rateLimiters, usernameHashZkProofVerifier, registrationRecoveryPasswordsManager),
        ExternalServiceCredentialsGrpcService.createForAllExternalServices(config, rateLimiters),
        new KeysGrpcService(accountsManager, keysManager, rateLimiters))
        .map(bindableService -> ServerInterceptors.intercept(bindableService,
            // Note: interceptors run in the reverse order they are added; the remote deprecation filter
            // depends on the user-agent context so it has to come first here!
            validatingInterceptor,
            errorMappingInterceptor,
            errorConformanceInterceptor,
            grpcAllowListInterceptor,
            remoteDeprecationFilter,
            metricServerInterceptor,
            requestAttributesInterceptor,
            requireAuthenticationInterceptor))
        .toList();

    final List<ServerServiceDefinition> unauthenticatedServices = Stream.of(
            new AccountsAnonymousGrpcService(accountsManager, rateLimiters),
            new CallQualitySurveyGrpcService(callQualitySurveyManager, rateLimiters),
            new KeysAnonymousGrpcService(accountsManager, keysManager, zkSecretParams, Clock.systemUTC()),
            new PaymentsGrpcService(currencyManager),
            ExternalServiceCredentialsAnonymousGrpcService.create(accountsManager, config))
        .map(bindableService -> ServerInterceptors.intercept(bindableService,
            // Note: interceptors run in the reverse order they are added; the remote deprecation filter
            // depends on the user-agent context so it has to come first here!
            grpcExternalRequestFilter,
            validatingInterceptor,
            errorMappingInterceptor,
            errorConformanceInterceptor,
            grpcAllowListInterceptor,
            remoteDeprecationFilter,
            metricServerInterceptor,
            requestAttributesInterceptor,
            prohibitAuthenticationInterceptor))
        .toList();

    final ServerBuilder<?> serverBuilder =
        NettyServerBuilder.forAddress(new InetSocketAddress(config.getGrpc().bindAddress(), config.getGrpc().port()));
    authenticatedServices.forEach(serverBuilder::addService);
    unauthenticatedServices.forEach(serverBuilder::addService);
    final ManagedGrpcServer exposedGrpcServer = new ManagedGrpcServer(serverBuilder.build());

    environment.lifecycle().manage(dnsResolutionEventLoopGroup);
    environment.lifecycle().manage(exposedGrpcServer);

    final List<Filter> filters = new ArrayList<>();
    filters.add(remoteDeprecationFilter);
    filters.add(new RemoteAddressFilter());
    filters.add(new TimestampResponseFilter());

    for (Filter filter : filters) {
      environment.servlets()
          .addFilter(filter.getClass().getSimpleName(), filter)
          .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
    }

    if (!config.getExternalRequestFilterConfiguration().paths().isEmpty()) {
      environment.servlets().addFilter(ExternalRequestFilter.class.getSimpleName(),
              new ExternalRequestFilter(config.getExternalRequestFilterConfiguration().permittedInternalRanges(),
                  config.getExternalRequestFilterConfiguration().grpcMethods()))
          .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,
              config.getExternalRequestFilterConfiguration().paths().toArray(new String[]{}));
    }

    final AuthFilter<BasicCredentials, AuthenticatedDevice> accountAuthFilter =
        new BasicCredentialAuthFilter.Builder<AuthenticatedDevice>()
            .setAuthenticator(accountAuthenticator)
            .buildAuthFilter();

    final String websocketServletPath = "/v1/websocket/";
    final String provisioningWebsocketServletPath = "/v1/websocket/provisioning/";

    final MetricsHttpChannelListener metricsHttpChannelListener = new MetricsHttpChannelListener(clientReleaseManager,
        Set.of(websocketServletPath, provisioningWebsocketServletPath, "/health-check"));
    metricsHttpChannelListener.configure(environment);
    final MessageMetrics messageMetrics = new MessageMetrics();
    final BackupMetrics backupMetrics = new BackupMetrics();

    // BufferingInterceptor is needed on the base environment but not the WebSocketEnvironment,
    // because we handle serialization of http responses on the websocket on our own and can
    // compute content lengths without it
    environment.jersey().register(new BufferingInterceptor());
    environment.jersey().register(new RestDeprecationFilter(dynamicConfigurationManager, experimentEnrollmentManager));

    environment.jersey().register(new VirtualExecutorServiceProvider(
        "managed-async-virtual-thread",
        config.getVirtualThreadConfiguration().maxConcurrentThreadsPerExecutor()));
    environment.jersey().register(new RateLimitByIpFilter(rateLimiters));
    environment.jersey().register(new RequestStatisticsFilter(TrafficSource.HTTP));
    environment.jersey().register(MultiRecipientMessageProvider.class);
    environment.jersey().register(new AuthDynamicFeature(accountAuthFilter));
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(AuthenticatedDevice.class));
    environment.jersey().register(new TimestampResponseFilter());

    ///
    WebSocketEnvironment<AuthenticatedDevice> webSocketEnvironment = new WebSocketEnvironment<>(environment,
        config.getWebSocketConfiguration(), Duration.ofMillis(90000));
    webSocketEnvironment.jersey().register(new VirtualExecutorServiceProvider(
        "managed-async-websocket-virtual-thread",
        config.getVirtualThreadConfiguration().maxConcurrentThreadsPerExecutor()));
    webSocketEnvironment.setAuthenticator(new WebSocketAccountAuthenticator(accountAuthenticator));
    webSocketEnvironment.setAuthenticatedWebSocketUpgradeFilter(new IdlePrimaryDeviceAuthenticatedWebSocketUpgradeFilter(
        config.idlePrimaryDeviceReminderConfiguration().minIdleDuration(), Clock.systemUTC()));
    webSocketEnvironment.setConnectListener(
        new AuthenticatedConnectListener(accountsManager, receiptSender, messagesManager, messageMetrics, pushNotificationManager,
            pushNotificationScheduler, disconnectionRequestManager,
            messageDeliveryScheduler, clientReleaseManager, messageDeliveryLoopMonitor, experimentEnrollmentManager
        ));
    webSocketEnvironment.jersey().register(new RateLimitByIpFilter(rateLimiters));
    webSocketEnvironment.jersey().register(new RequestStatisticsFilter(TrafficSource.WEBSOCKET));
    webSocketEnvironment.jersey().register(MultiRecipientMessageProvider.class);
    webSocketEnvironment.jersey().register(new MetricsApplicationEventListener(TrafficSource.WEBSOCKET, clientReleaseManager));
    webSocketEnvironment.jersey().register(new KeepAliveController(redisMessageAvailabilityManager));
    webSocketEnvironment.jersey().register(new TimestampResponseFilter());

    final List<SpamFilter> spamFilters = ServiceLoader.load(SpamFilter.class)
        .stream()
        .map(ServiceLoader.Provider::get)
        .flatMap(filter -> {
          try {
            filter.configure(config.getSpamFilterConfiguration().getEnvironment(), environment.getValidator());
            return Stream.of(filter);
          } catch (Exception e) {
            log.warn("Failed to register spam filter: {}", filter.getClass().getName(), e);
            return Stream.empty();
          }
        })
        .toList();
    if (spamFilters.size() > 1) {
      log.warn("Multiple spam report token providers found. Using the first.");
    }
    final Optional<SpamFilter> spamFilter = spamFilters.stream().findFirst();
    if (spamFilter.isEmpty()) {
      log.warn("No spam filters installed");
    }
    final SpamChecker spamChecker = spamFilter
        .map(SpamFilter::getSpamChecker)
        .orElseGet(() -> {
          log.warn("No spam-checkers found; using default (no-op) provider as a default");
          return SpamChecker.noop();
        });
    final ChallengeConstraintChecker challengeConstraintChecker = spamFilter
        .map(SpamFilter::getChallengeConstraintChecker)
        .orElseGet(() -> {
          log.warn("No challenge-constraint-checkers found; using default (no-op) provider as a default");
          return ChallengeConstraintChecker.noop();
        });
    final RegistrationFraudChecker registrationFraudChecker = spamFilter
        .map(SpamFilter::getRegistrationFraudChecker)
        .orElseGet(() -> {
          log.warn("No registration-fraud-checkers found; using default (no-op) provider as a default");
          return RegistrationFraudChecker.noop();
        });
    final RegistrationRecoveryChecker registrationRecoveryChecker = spamFilter
        .map(SpamFilter::getRegistrationRecoveryChecker)
        .orElseGet(() -> {
          log.warn("No registration-recovery-checkers found; using default (no-op) provider as a default");
          return RegistrationRecoveryChecker.noop();
        });
    final Function<String, CaptchaClient> captchaClientSupplier = spamFilter
        .map(SpamFilter::getCaptchaClientSupplier)
        .orElseGet(() -> {
          log.warn("No captcha clients found; using default (no-op) client as default");
          return ignored -> CaptchaClient.noop();
        });

    spamFilter.map(SpamFilter::getReportedMessageListener).ifPresent(reportMessageManager::addListener);
    spamFilter.map(SpamFilter::getMessageDeliveryListener).ifPresent(messageSender::addMessageDeliveryListener);

    final HttpClient shortCodeRetrieverHttpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
        .connectTimeout(Duration.ofSeconds(10)).build();
    final ShortCodeExpander shortCodeRetriever = new ShortCodeExpander(shortCodeRetrieverHttpClient, config.getShortCodeRetrieverConfiguration().baseUrl());
    final CaptchaChecker captchaChecker = new CaptchaChecker(shortCodeRetriever, captchaClientSupplier);

    final RegistrationCaptchaManager registrationCaptchaManager = new RegistrationCaptchaManager(captchaChecker);

    final RateLimitChallengeManager rateLimitChallengeManager = new RateLimitChallengeManager(pushChallengeManager,
        captchaChecker, rateLimiters, spamFilter.map(SpamFilter::getRateLimitChallengeListener).stream().toList());

    spamFilter.ifPresent(filter -> {
      environment.lifecycle().manage(filter);
      log.info("Registered spam filter: {}", filter.getClass().getName());
    });

    final PersistentTimer persistentTimer = new PersistentTimer(rateLimitersCluster, clock);

    final PhoneVerificationTokenManager phoneVerificationTokenManager = new PhoneVerificationTokenManager(
        phoneNumberIdentifiers, registrationServiceClient, registrationRecoveryPasswordsManager, registrationRecoveryChecker);
    final List<Object> commonControllers = Lists.newArrayList(
        new AccountController(accountsManager, rateLimiters, registrationRecoveryPasswordsManager,
            usernameHashZkProofVerifier),
        new AccountControllerV2(accountsManager, changeNumberManager, phoneVerificationTokenManager,
            registrationLockVerificationManager, rateLimiters),
        new AttachmentControllerV4(rateLimiters, gcsAttachmentGenerator, tusAttachmentGenerator,
            experimentEnrollmentManager),
        new ArchiveController(accountsManager, backupAuthManager, backupManager, backupMetrics),
        new CallRoutingControllerV2(rateLimiters, cloudflareTurnCredentialsManager),
        new CallLinkController(rateLimiters, callingGenericZkSecretParams),
        new CallQualitySurveyController(callQualitySurveyManager),
        new CertificateController(accountsManager, new CertificateGenerator(config.getDeliveryCertificate().certificate(),
            config.getDeliveryCertificate().ecPrivateKey(), config.getDeliveryCertificate().expiresDays(), config.getDeliveryCertificate().embedSigner()),
            zkAuthOperations, callingGenericZkSecretParams, clock),
        new ChallengeController(accountsManager, rateLimitChallengeManager, challengeConstraintChecker),
        new DeviceController(accountsManager, clientPublicKeysManager, rateLimiters, persistentTimer, config.getMaxDevices()),
        new DeviceCheckController(clock, accountsManager, backupAuthManager, appleDeviceCheckManager, rateLimiters,
            config.getDeviceCheck().backupRedemptionLevel(),
            config.getDeviceCheck().backupRedemptionDuration()),
        new DirectoryV2Controller(directoryV2CredentialsGenerator),
        new DonationController(clock, zkReceiptOperations, redeemedReceiptsManager, accountsManager, config.getBadges(),
            ReceiptCredentialPresentation::new),
        new KeysController(rateLimiters, keysManager, accountsManager, zkSecretParams, Clock.systemUTC()),
        new KeyTransparencyController(keyTransparencyServiceClient),
        new MessageController(rateLimiters, messageByteLimitCardinalityEstimator, messageSender,
            accountsManager, messagesManager, phoneNumberIdentifiers, pushNotificationManager, pushNotificationScheduler,
            reportMessageManager, messageDeliveryScheduler, clientReleaseManager,
            zkSecretParams, spamChecker, messageMetrics, messageDeliveryLoopMonitor,
            Clock.systemUTC()),
        new PaymentsController(currencyManager, paymentsCredentialsGenerator),
        new ProfileController(clock, rateLimiters, accountsManager, profilesManager, dynamicConfigurationManager,
            profileBadgeConverter, config.getBadges(), profileCdnPolicyGenerator, profileCdnPolicySigner,
            zkSecretParams, zkProfileOperations, batchIdentityCheckExecutor),
        new ProvisioningController(rateLimiters, provisioningManager),
        new RegistrationController(accountsManager, phoneVerificationTokenManager, registrationLockVerificationManager,
            rateLimiters),
        new RemoteConfigController(remoteConfigsManager, config.getRemoteConfigConfiguration().globalConfig(), clock),
        new SecureStorageController(storageCredentialsGenerator),
        new SecureValueRecovery2Controller(svr2CredentialsGenerator, accountsManager),
        new StickerController(rateLimiters, config.getCdnConfiguration().credentials().accessKeyId().value(),
            config.getCdnConfiguration().credentials().secretAccessKey().value(), config.getCdnConfiguration().region(),
            config.getCdnConfiguration().bucket()),
        new VerificationController(registrationServiceClient, new VerificationSessionManager(verificationSessions),
            pushNotificationManager, registrationCaptchaManager, registrationRecoveryPasswordsManager,
            phoneNumberIdentifiers, rateLimiters, accountsManager, carrierDataProvider, registrationFraudChecker,
            dynamicConfigurationManager, clock)
    );
    if (config.getSubscription() != null && config.getOneTimeDonations() != null) {
      SubscriptionManager subscriptionManager = new SubscriptionManager(subscriptions,
          List.of(stripeManager, braintreeManager, googlePlayBillingManager, appleAppStoreManager),
          zkReceiptOperations, issuedReceiptsManager);
      commonControllers.add(new SubscriptionController(clock, config.getSubscription(), config.getOneTimeDonations(),
          subscriptionManager, stripeManager, braintreeManager, googlePlayBillingManager, appleAppStoreManager,
          profileBadgeConverter, bankMandateTranslator, dynamicConfigurationManager));
      commonControllers.add(new OneTimeDonationController(clock, config.getOneTimeDonations(), stripeManager, braintreeManager,
          payPalDonationsTranslator, zkReceiptOperations, issuedReceiptsManager, oneTimeDonationsManager));
    }

    for (Object controller : commonControllers) {
      environment.jersey().register(controller);
      webSocketEnvironment.jersey().register(controller);
    }

    WebSocketEnvironment<AuthenticatedDevice> provisioningEnvironment = new WebSocketEnvironment<>(environment,
        webSocketEnvironment.getRequestLog(), Duration.ofMillis(60000));
    provisioningEnvironment.setConnectListener(new ProvisioningConnectListener(provisioningManager, clientReleaseManager, provisioningWebsocketTimeoutExecutor, Duration.ofSeconds(90)));
    provisioningEnvironment.jersey().register(new MetricsApplicationEventListener(TrafficSource.WEBSOCKET, clientReleaseManager));
    provisioningEnvironment.jersey().register(new KeepAliveController(redisMessageAvailabilityManager));
    provisioningEnvironment.jersey().register(new TimestampResponseFilter());

    registerExceptionMappers(environment, webSocketEnvironment, provisioningEnvironment);

    environment.jersey().property(ServerProperties.UNWRAP_COMPLETION_STAGE_IN_WRITER_ENABLE, Boolean.TRUE);
    webSocketEnvironment.jersey().property(ServerProperties.UNWRAP_COMPLETION_STAGE_IN_WRITER_ENABLE, Boolean.TRUE);
    provisioningEnvironment.jersey().property(ServerProperties.UNWRAP_COMPLETION_STAGE_IN_WRITER_ENABLE, Boolean.TRUE);

    JettyWebSocketServletContainerInitializer.configure(environment.getApplicationContext(), (context, container) -> {
      final WebSocketExtensionRegistry extensionRegistry = WebSocketServerComponents
          .getWebSocketComponents(environment.getApplicationContext().getServletContext())
          .getExtensionRegistry();
      if (config.getWebSocketConfiguration().isDisablePerMessageDeflate()) {
        extensionRegistry.unregister("permessage-deflate");
      } else if (config.getWebSocketConfiguration().isDisableCrossMessageOutgoingCompression()) {
        extensionRegistry.unregister("permessage-deflate");
        extensionRegistry.register("permessage-deflate", NoContextTakeoverPerMessageDeflateExtension.class);
      }
    });

    WebSocketResourceProviderFactory<AuthenticatedDevice> webSocketServlet = new WebSocketResourceProviderFactory<>(
        webSocketEnvironment, AuthenticatedDevice.class, config.getWebSocketConfiguration(),
        RemoteAddressFilter.REMOTE_ADDRESS_ATTRIBUTE_NAME);
    WebSocketResourceProviderFactory<AuthenticatedDevice> provisioningServlet = new WebSocketResourceProviderFactory<>(
        provisioningEnvironment, AuthenticatedDevice.class, config.getWebSocketConfiguration(),
        RemoteAddressFilter.REMOTE_ADDRESS_ATTRIBUTE_NAME);

    ServletRegistration.Dynamic websocket = environment.servlets().addServlet("WebSocket", webSocketServlet);
    ServletRegistration.Dynamic provisioning = environment.servlets().addServlet("Provisioning", provisioningServlet);

    websocket.addMapping(websocketServletPath);
    websocket.setAsyncSupported(true);

    provisioning.addMapping(provisioningWebsocketServletPath);
    provisioning.setAsyncSupported(true);

    environment.admin().addTask(new SetRequestLoggingEnabledTask());

  }

  private void registerExceptionMappers(Environment environment,
      WebSocketEnvironment<AuthenticatedDevice> webSocketEnvironment,
      WebSocketEnvironment<AuthenticatedDevice> provisioningEnvironment) {

    List.of(
        new LoggingUnhandledExceptionMapper(),
        new CompletionExceptionMapper(),
        new GrpcStatusRuntimeExceptionMapper(),
        new IOExceptionMapper(),
        new RateLimitExceededExceptionMapper(),
        new InvalidWebsocketAddressExceptionMapper(),
        new DeviceLimitExceededExceptionMapper(),
        new ServerRejectedExceptionMapper(),
        new ImpossiblePhoneNumberExceptionMapper(),
        new NonNormalizedPhoneNumberExceptionMapper(),
        new ObsoletePhoneNumberFormatExceptionMapper(),
        new RegistrationServiceSenderExceptionMapper(),
        new SubscriptionExceptionMapper(),
        new BackupExceptionMapper(),
        new JsonMappingExceptionMapper()
    ).forEach(exceptionMapper -> {
      environment.jersey().register(exceptionMapper);
      webSocketEnvironment.jersey().register(exceptionMapper);
      provisioningEnvironment.jersey().register(exceptionMapper);
    });
  }

  public static class ExecutorServiceBuilder extends io.dropwizard.lifecycle.setup.ExecutorServiceBuilder {
    private final String baseName;

    public ExecutorServiceBuilder(final LifecycleEnvironment environment, final String baseName) {
      super(environment, name(WhisperServerService.class, baseName) + "-%d");
      this.baseName = baseName;
    }

    @Override
    public ExecutorService build() {
      return ExecutorServiceMetrics.monitor(Metrics.globalRegistry, super.build(), baseName, MetricsUtil.PREFIX);
    }

    public static ExecutorServiceBuilder of(final Environment environment, final String name) {
      return new ExecutorServiceBuilder(environment.lifecycle(), name);
    }
  }

  public static class ScheduledExecutorServiceBuilder extends io.dropwizard.lifecycle.setup.ScheduledExecutorServiceBuilder {
    private final String baseName;

    public ScheduledExecutorServiceBuilder(final LifecycleEnvironment environment, final String baseName) {
      super(environment, name(WhisperServerService.class, baseName) + "-%d", false);
      this.baseName = baseName;
    }

    @Override
    public ScheduledExecutorService build() {
      return ExecutorServiceMetrics.monitor(Metrics.globalRegistry, super.build(), baseName, MetricsUtil.PREFIX);
    }

    public static ScheduledExecutorServiceBuilder of(final Environment environment, final String name) {
      return new ScheduledExecutorServiceBuilder(environment.lifecycle(), name);
    }
  }

  public static void main(String[] args) throws Exception {
    new WhisperServerService().run(args);
  }
}

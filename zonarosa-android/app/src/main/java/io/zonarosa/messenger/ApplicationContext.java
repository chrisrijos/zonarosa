/*
 * Copyright 2026 ZonaRosa Platform
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zonarosa.messenger;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.bumptech.glide.Glide;
import com.google.android.gms.security.ProviderInstaller;

import org.conscrypt.ConscryptZonaRosa;
import org.greenrobot.eventbus.EventBus;
import io.zonarosa.aesgcmprovider.AesGcmProvider;
import io.zonarosa.core.util.DiskUtil;
import io.zonarosa.core.util.MemoryTracker;
import io.zonarosa.core.util.concurrent.AnrDetector;
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors;
import io.zonarosa.core.util.logging.AndroidLogger;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.core.util.logging.Scrubber;
import io.zonarosa.core.util.tracing.Tracer;
import io.zonarosa.glide.ZonaRosaGlideCodecs;
import io.zonarosa.libzonarosa.net.ChatServiceException;
import io.zonarosa.libzonarosa.protocol.logging.ZonaRosaProtocolLoggerProvider;
import io.zonarosa.ringrtc.CallManager;
import io.zonarosa.messenger.apkupdate.ApkUpdateRefreshListener;
import io.zonarosa.messenger.avatar.AvatarPickerStorage;
import io.zonarosa.messenger.backup.v2.BackupRepository;
import io.zonarosa.messenger.crypto.AttachmentSecretProvider;
import io.zonarosa.messenger.crypto.DatabaseSecretProvider;
import io.zonarosa.messenger.database.LogDatabase;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.database.SqlCipherLibraryLoader;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.dependencies.ApplicationDependencyProvider;
import io.zonarosa.messenger.emoji.EmojiSource;
import io.zonarosa.messenger.emoji.JumboEmoji;
import io.zonarosa.messenger.gcm.FcmFetchManager;
import io.zonarosa.messenger.glide.ZonaRosaGlideComponents;
import io.zonarosa.messenger.jobs.AccountConsistencyWorkerJob;
import io.zonarosa.messenger.jobs.BackupRefreshJob;
import io.zonarosa.messenger.jobs.BackupSubscriptionCheckJob;
import io.zonarosa.messenger.jobs.BuildExpirationConfirmationJob;
import io.zonarosa.messenger.jobs.CheckKeyTransparencyJob;
import io.zonarosa.messenger.jobs.CheckServiceReachabilityJob;
import io.zonarosa.messenger.jobs.DownloadLatestEmojiDataJob;
import io.zonarosa.messenger.jobs.EmojiSearchIndexDownloadJob;
import io.zonarosa.messenger.jobs.FcmRefreshJob;
import io.zonarosa.messenger.jobs.FontDownloaderJob;
import io.zonarosa.messenger.jobs.GroupRingCleanupJob;
import io.zonarosa.messenger.jobs.GroupV2UpdateSelfProfileKeyJob;
import io.zonarosa.messenger.jobs.InAppPaymentAuthCheckJob;
import io.zonarosa.messenger.jobs.InAppPaymentKeepAliveJob;
import io.zonarosa.messenger.jobs.LinkedDeviceInactiveCheckJob;
import io.zonarosa.messenger.jobs.MultiDeviceContactUpdateJob;
import io.zonarosa.messenger.jobs.PreKeysSyncJob;
import io.zonarosa.messenger.jobs.ProfileUploadJob;
import io.zonarosa.messenger.jobs.RefreshSvrCredentialsJob;
import io.zonarosa.messenger.jobs.RestoreOptimizedMediaJob;
import io.zonarosa.messenger.jobs.RetrieveProfileJob;
import io.zonarosa.messenger.jobs.RetrieveRemoteAnnouncementsJob;
import io.zonarosa.messenger.jobs.RetryPendingSendsJob;
import io.zonarosa.messenger.jobs.StoryOnboardingDownloadJob;
import io.zonarosa.messenger.keyvalue.KeepMessagesDuration;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.logging.CustomZonaRosaProtocolLogger;
import io.zonarosa.messenger.logging.PersistentLogger;
import io.zonarosa.messenger.messageprocessingalarm.RoutineMessageFetchReceiver;
import io.zonarosa.messenger.migrations.ApplicationMigrations;
import io.zonarosa.messenger.mms.ZonaRosaGlideModule;
import io.zonarosa.messenger.providers.BlobProvider;
import io.zonarosa.messenger.ratelimit.RateLimitUtil;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.registration.util.RegistrationUtil;
import io.zonarosa.messenger.ringrtc.RingRtcLogger;
import io.zonarosa.messenger.service.AnalyzeDatabaseAlarmListener;
import io.zonarosa.messenger.service.DirectoryRefreshListener;
import io.zonarosa.messenger.service.KeyCachingService;
import io.zonarosa.messenger.service.LocalBackupListener;
import io.zonarosa.messenger.service.MessageBackupListener;
import io.zonarosa.messenger.service.RotateSenderCertificateListener;
import io.zonarosa.messenger.service.RotateSignedPreKeyListener;
import io.zonarosa.messenger.service.webrtc.ActiveCallManager;
import io.zonarosa.messenger.service.webrtc.AndroidTelecomUtil;
import io.zonarosa.messenger.storage.StorageSyncHelper;
import io.zonarosa.messenger.util.AppForegroundObserver;
import io.zonarosa.messenger.util.AppStartup;
import io.zonarosa.messenger.util.DeviceProperties;
import io.zonarosa.messenger.util.DynamicTheme;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.messenger.util.ZonaRosaLocalMetrics;
import io.zonarosa.messenger.util.ZonaRosaUncaughtExceptionHandler;
import io.zonarosa.messenger.util.ZonaRosaPreferences;
import io.zonarosa.core.util.Util;
import io.zonarosa.messenger.util.VersionTracker;
import io.zonarosa.messenger.util.dynamiclanguage.DynamicLanguageContextWrapper;
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket;

import java.io.InterruptedIOException;
import java.net.SocketException;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import io.reactivex.rxjava3.exceptions.UndeliverableException;
import io.reactivex.rxjava3.plugins.RxJavaPlugins;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import rxdogtag2.RxDogTag;

/**
 * Will be called once when the ZonaRosa process is created.
 * <p>
 * We're using this as an insertion point to patch up the Android PRNG disaster,
 * to initialize the job manager, and to check for GCM registration freshness.
 *
 * @author Moxie Marlinspike
 */
public class ApplicationContext extends Application implements AppForegroundObserver.Listener {

  private static final String TAG = Log.tag(ApplicationContext.class);

  public static ApplicationContext getInstance(Context context) {
    return (ApplicationContext) context.getApplicationContext();
  }

  @Override
  public void onCreate() {
    Tracer.getInstance().start("Application#onCreate()");
    AppStartup.getInstance().onApplicationCreate();
    ZonaRosaLocalMetrics.ColdStart.start();

    long startTime = System.currentTimeMillis();

    super.onCreate();

    AppStartup.getInstance().addBlocking("sqlcipher-init", () -> {
                SqlCipherLibraryLoader.load();
                ZonaRosaDatabase.init(this,
                                    DatabaseSecretProvider.getOrCreateDatabaseSecret(this),
                                    AttachmentSecretProvider.getInstance(this).getOrCreateAttachmentSecret());
              })
              .addBlocking("zonarosa-store", () -> ZonaRosaStore.init(this))
              .addBlocking("logging", () -> {
                initializeLogging();
                Log.i(TAG, "onCreate()");
              })
              .addBlocking("app-dependencies", this::initializeAppDependencies)
              .addBlocking("anr-detector", this::startAnrDetector)
              .addBlocking("security-provider", this::initializeSecurityProvider)
              .addBlocking("crash-handling", this::initializeCrashHandling)
              .addBlocking("rx-init", this::initializeRx)
              .addBlocking("event-bus", () -> EventBus.builder().logNoSubscriberMessages(false).installDefaultEventBus())
              .addBlocking("scrubber", () -> Scrubber.setIdentifierHmacKeyProvider(() -> ZonaRosaStore.svr().getMasterKey().deriveLoggingKey()))
              .addBlocking("first-launch", this::initializeFirstEverAppLaunch)
              .addBlocking("app-migrations", this::initializeApplicationMigrations)
              .addBlocking("lifecycle-observer", () -> AppForegroundObserver.addListener(this))
              .addBlocking("message-retriever", this::initializeMessageRetrieval)
              .addBlocking("dynamic-theme", () -> DynamicTheme.setDefaultDayNightMode(this))
              .addBlocking("proxy-init", () -> {
                if (ZonaRosaStore.proxy().isProxyEnabled()) {
                  Log.w(TAG, "Proxy detected. Enabling Conscrypt.setUseEngineSocketByDefault()");
                  ConscryptZonaRosa.setUseEngineSocketByDefault(true);
                }
              })
              .addBlocking("blob-provider", this::initializeBlobProvider)
              .addBlocking("remote-config", RemoteConfig::init)
              .addBlocking("ring-rtc", this::initializeRingRtc)
              .addBlocking("glide", () -> ZonaRosaGlideModule.setRegisterGlideComponents(new ZonaRosaGlideComponents()))
              .addBlocking("tracer", this::initializeTracer)
              .addNonBlocking(() -> RegistrationUtil.maybeMarkRegistrationComplete())
              .addNonBlocking(() -> Glide.get(this))
              .addNonBlocking(this::cleanAvatarStorage)
              .addNonBlocking(this::initializeRevealableMessageManager)
              .addNonBlocking(this::initializePendingRetryReceiptManager)
              .addNonBlocking(this::initializeScheduledMessageManager)
              .addNonBlocking(this::initializeFcmCheck)
              .addNonBlocking(PreKeysSyncJob::enqueueIfNeeded)
              .addNonBlocking(this::initializePeriodicTasks)
              .addNonBlocking(this::initializeCircumvention)
              .addNonBlocking(this::initializeCleanup)
              .addNonBlocking(this::initializeGlideCodecs)
              .addNonBlocking(StorageSyncHelper::scheduleRoutineSync)
              .addNonBlocking(this::beginJobLoop)
              .addNonBlocking(EmojiSource::refresh)
              .addNonBlocking(() -> AppDependencies.getGiphyMp4Cache().onAppStart(this))
              .addNonBlocking(AppDependencies::getBillingApi)
              .addNonBlocking(this::ensureProfileUploaded)
              .addNonBlocking(() -> AppDependencies.getExpireStoriesManager().scheduleIfNecessary())
              .addNonBlocking(BackupRepository::maybeFixAnyDanglingUploadProgress)
              .addPostRender(() -> AppDependencies.getDeletedCallEventManager().scheduleIfNecessary())
              .addPostRender(() -> RateLimitUtil.retryAllRateLimitedMessages(this))
              .addPostRender(this::initializeExpiringMessageManager)
              .addPostRender(this::initializeTrimThreadsByDateManager)
              .addPostRender(RefreshSvrCredentialsJob::enqueueIfNecessary)
              .addPostRender(() -> DownloadLatestEmojiDataJob.scheduleIfNecessary(this))
              .addPostRender(EmojiSearchIndexDownloadJob::scheduleIfNecessary)
              .addPostRender(() -> ZonaRosaDatabase.messageLog().trimOldMessages(System.currentTimeMillis(), RemoteConfig.retryRespondMaxAge()))
              .addPostRender(() -> JumboEmoji.updateCurrentVersion(this))
              .addPostRender(RetrieveRemoteAnnouncementsJob::enqueue)
              .addPostRender(() -> AndroidTelecomUtil.registerPhoneAccount())
              .addPostRender(() -> AppDependencies.getJobManager().add(new FontDownloaderJob()))
              .addPostRender(CheckServiceReachabilityJob::enqueueIfNecessary)
              .addPostRender(GroupV2UpdateSelfProfileKeyJob::enqueueForGroupsIfNecessary)
              .addPostRender(StoryOnboardingDownloadJob.Companion::enqueueIfNeeded)
              .addPostRender(() -> AppDependencies.getExoPlayerPool().getPoolStats().getMaxUnreserved())
              .addPostRender(() -> AppDependencies.getRecipientCache().warmUp())
              .addPostRender(AccountConsistencyWorkerJob::enqueueIfNecessary)
              .addPostRender(GroupRingCleanupJob::enqueue)
              .addPostRender(LinkedDeviceInactiveCheckJob::enqueueIfNecessary)
              .addPostRender(() -> ActiveCallManager.clearNotifications(this))
              .addPostRender(RestoreOptimizedMediaJob::enqueueIfNecessary)
              .addPostRender(() -> AppDependencies.getPinnedMessageManager().scheduleIfNecessary())
              .execute();

    Log.d(TAG, "onCreate() took " + (System.currentTimeMillis() - startTime) + " ms");
    ZonaRosaLocalMetrics.ColdStart.onApplicationCreateFinished();
    Tracer.getInstance().end("Application#onCreate()");
  }

  @Override
  public void onForeground() {
    long startTime = System.currentTimeMillis();
    Log.i(TAG, "App is now visible. Battery: " + DeviceProperties.getBatteryLevel(this) + "% (charging: " + DeviceProperties.isCharging(this) + ")");

    AppDependencies.getFrameRateTracker().start();
    AppDependencies.getMegaphoneRepository().onAppForegrounded();
    AppDependencies.getDeadlockDetector().start();
    InAppPaymentKeepAliveJob.enqueueAndTrackTimeIfNecessary();
    FcmFetchManager.onForeground(this);
    startAnrDetector();

    ZonaRosaExecutors.BOUNDED.execute(() -> {
      BackupRefreshJob.enqueueIfNecessary();
      InAppPaymentAuthCheckJob.enqueueIfNeeded();
      RemoteConfig.refreshIfNecessary();
      RetrieveProfileJob.enqueueRoutineFetchIfNecessary();
      executePendingContactSync();
      KeyCachingService.onAppForegrounded(this);
      AppDependencies.getShakeToReport().enable();
      checkBuildExpiration();
      checkFreeDiskSpace();
      MemoryTracker.start();
      BackupSubscriptionCheckJob.enqueueIfAble();
      CheckKeyTransparencyJob.enqueueIfNecessary(true);
      AppDependencies.getAuthWebSocket().registerKeepAliveToken(ZonaRosaWebSocket.FOREGROUND_KEEPALIVE);
      AppDependencies.getUnauthWebSocket().registerKeepAliveToken(ZonaRosaWebSocket.FOREGROUND_KEEPALIVE);

      long lastForegroundTime = ZonaRosaStore.misc().getLastForegroundTime();
      long currentTime        = System.currentTimeMillis();
      long timeDiff           = currentTime - lastForegroundTime;

      if (timeDiff < 0) {
        Log.w(TAG, "Time travel! The system clock has moved backwards. (currentTime: " + currentTime + " ms, lastForegroundTime: " + lastForegroundTime + " ms, diff: " + timeDiff + " ms)", true);
      }

      ZonaRosaStore.misc().setLastForegroundTime(currentTime);
    });

    Log.d(TAG, "onStart() took " + (System.currentTimeMillis() - startTime) + " ms");
  }

  @Override
  public void onBackground() {
    Log.i(TAG, "App is no longer visible.");
    KeyCachingService.onAppBackgrounded(this);
    AppDependencies.getMessageNotifier().clearVisibleThread();
    AppDependencies.getFrameRateTracker().stop();
    AppDependencies.getShakeToReport().disable();
    AppDependencies.getDeadlockDetector().stop();
    AppDependencies.getAuthWebSocket().removeKeepAliveToken(ZonaRosaWebSocket.FOREGROUND_KEEPALIVE);
    AppDependencies.getUnauthWebSocket().removeKeepAliveToken(ZonaRosaWebSocket.FOREGROUND_KEEPALIVE);
    MemoryTracker.stop();
    AnrDetector.stop();
  }

  public void checkBuildExpiration() {
    if (Util.getTimeUntilBuildExpiry(ZonaRosaStore.misc().getEstimatedServerTime()) <= 0 && !ZonaRosaStore.misc().isClientDeprecated()) {
      Log.w(TAG, "Build potentially expired! Enqueing job to check.", true);
      AppDependencies.getJobManager().add(new BuildExpirationConfirmationJob());
    }
  }

  public void checkFreeDiskSpace() {
    long availableBytes = DiskUtil.getAvailableSpace(getApplicationContext()).getBytes();
    ZonaRosaStore.backup().setSpaceAvailableOnDiskBytes(availableBytes);
  }

  /**
   * Note: this is purposefully "started" twice -- once during application create, and once during foreground.
   * This is so we can capture ANR's that happen on boot before the foreground event.
   */
  private void startAnrDetector() {
    AnrDetector.start(TimeUnit.SECONDS.toMillis(5), RemoteConfig::internalUser, (dumps) -> {
      LogDatabase.getInstance(this).anrs().save(System.currentTimeMillis(), dumps);
      return Unit.INSTANCE;
    });
  }

  private void initializeSecurityProvider() {
    int aesPosition = Security.insertProviderAt(new AesGcmProvider(), 1);
    Log.i(TAG, "Installed AesGcmProvider: " + aesPosition);

    if (aesPosition < 0) {
      Log.e(TAG, "Failed to install AesGcmProvider()");
      throw new ProviderInitializationException();
    }

    int conscryptPosition = Security.insertProviderAt(ConscryptZonaRosa.newProvider(), 2);
    Log.i(TAG, "Installed Conscrypt provider: " + conscryptPosition);

    if (conscryptPosition < 0) {
      Log.w(TAG, "Did not install Conscrypt provider. May already be present.");
    }
  }

  @VisibleForTesting
  protected void initializeLogging() {
    Log.initialize(RemoteConfig::internalUser, AndroidLogger.INSTANCE, PersistentLogger.getInstance(this));

    ZonaRosaProtocolLoggerProvider.setProvider(new CustomZonaRosaProtocolLogger());
    ZonaRosaProtocolLoggerProvider.initializeLogging(BuildConfig.LIBZONAROSA_LOG_LEVEL);

    ZonaRosaExecutors.UNBOUNDED.execute(() -> {
      Log.blockUntilAllWritesFinished();
      LogDatabase.getInstance(this).logs().trimToSize();
      LogDatabase.getInstance(this).crashes().trimToSize();
    });
  }

  private void initializeCrashHandling() {
    final Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
    Thread.setDefaultUncaughtExceptionHandler(new ZonaRosaUncaughtExceptionHandler(originalHandler));
  }

  private void initializeRx() {
    RxDogTag.install();
    RxJavaPlugins.setInitIoSchedulerHandler(schedulerSupplier -> Schedulers.from(ZonaRosaExecutors.UNBOUNDED, true, false));
    RxJavaPlugins.setInitComputationSchedulerHandler(schedulerSupplier -> Schedulers.from(ZonaRosaExecutors.BOUNDED, true, false));
    RxJavaPlugins.setErrorHandler(e -> {
      boolean wasWrapped = false;
      while ((e instanceof UndeliverableException || e instanceof AssertionError || e instanceof OnErrorNotImplementedException) && e.getCause() != null) {
        wasWrapped = true;
        e = e.getCause();
      }

      if (wasWrapped && (e instanceof SocketException || e instanceof InterruptedException || e instanceof InterruptedIOException || e instanceof ChatServiceException)) {
        return;
      }

      Log.e(TAG, "RxJava error handler invoked", e);

      Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.currentThread().getUncaughtExceptionHandler();
      if (uncaughtExceptionHandler == null) {
        uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
      }

      uncaughtExceptionHandler.uncaughtException(Thread.currentThread(), e);
    });
  }

  private void initializeApplicationMigrations() {
    ApplicationMigrations.onApplicationCreate(this, AppDependencies.getJobManager());
  }

  public void initializeMessageRetrieval() {
    ZonaRosaExecutors.UNBOUNDED.execute(AppDependencies::startNetwork);
  }

  @VisibleForTesting
  void initializeAppDependencies() {
    if (!AppDependencies.isInitialized()) {
      Log.i(TAG, "Initializing AppDependencies.");
      AppDependencies.init(this, new ApplicationDependencyProvider(this));
    }
    AppForegroundObserver.begin();
  }

  private void initializeFirstEverAppLaunch() {
    if (ZonaRosaPreferences.getFirstInstallVersion(this) == -1) {
      if (!ZonaRosaDatabase.databaseFileExists(this) || VersionTracker.getDaysSinceFirstInstalled(this) < 365) {
        Log.i(TAG, "First ever app launch!");
        AppInitialization.onFirstEverAppLaunch(this);
      }

      Log.i(TAG, "Setting first install version to " + BuildConfig.CANONICAL_VERSION_CODE);
      ZonaRosaPreferences.setFirstInstallVersion(this, BuildConfig.CANONICAL_VERSION_CODE);
    } else if (!ZonaRosaStore.settings().getPassphraseDisabled() && VersionTracker.getDaysSinceFirstInstalled(this) < 90) {
      Log.i(TAG, "Detected a new install that doesn't have passphrases disabled -- assuming bad initialization.");
      AppInitialization.onRepairFirstEverAppLaunch(this);
    } else if (!ZonaRosaStore.settings().getPassphraseDisabled() && VersionTracker.getDaysSinceFirstInstalled(this) < 912) {
      Log.i(TAG, "Detected a not-recent install that doesn't have passphrases disabled -- disabling now.");
      ZonaRosaStore.settings().setPassphraseDisabled(true);
    }
  }

  private void initializeFcmCheck() {
    if (ZonaRosaStore.account().isRegistered()) {
      long lastSetTime = ZonaRosaStore.account().getFcmTokenLastSetTime();
      long nextSetTime = lastSetTime + TimeUnit.HOURS.toMillis(6);
      long now         = System.currentTimeMillis();

      if (ZonaRosaStore.account().getFcmToken() == null || nextSetTime <= now || lastSetTime > now) {
        AppDependencies.getJobManager().add(new FcmRefreshJob());
      }
    }
  }

  private void initializeExpiringMessageManager() {
    AppDependencies.getExpiringMessageManager().checkSchedule();
  }

  private void initializeRevealableMessageManager() {
    AppDependencies.getViewOnceMessageManager().scheduleIfNecessary();
  }

  private void initializePendingRetryReceiptManager() {
    AppDependencies.getPendingRetryReceiptManager().scheduleIfNecessary();
  }

  private void initializeScheduledMessageManager() {
    AppDependencies.getScheduledMessageManager().scheduleIfNecessary();
  }

  private void initializeTrimThreadsByDateManager() {
    KeepMessagesDuration keepMessagesDuration = ZonaRosaStore.settings().getKeepMessagesDuration();
    if (keepMessagesDuration != KeepMessagesDuration.FOREVER) {
      AppDependencies.getTrimThreadsByDateManager().scheduleIfNecessary();
    }
  }

  private void initializeTracer() {
    if (RemoteConfig.internalUser()) {
      Tracer.getInstance().setMaxBufferSize(35_000);
    }
  }

  private void initializePeriodicTasks() {
    RotateSignedPreKeyListener.schedule(this);
    DirectoryRefreshListener.schedule(this);
    LocalBackupListener.schedule(this);
    MessageBackupListener.schedule(this);
    RotateSenderCertificateListener.schedule(this);
    RoutineMessageFetchReceiver.startOrUpdateAlarm(this);
    AnalyzeDatabaseAlarmListener.schedule(this);

    if (BuildConfig.MANAGES_APP_UPDATES) {
      ApkUpdateRefreshListener.schedule(this);
    }
  }

  private void initializeRingRtc() {
    try {
      Map<String, String> fieldTrials = new HashMap<>();
      if (RemoteConfig.callingFieldTrialAnyAddressPortsKillSwitch()) {
        fieldTrials.put("RingRTC-AnyAddressPortsKillSwitch", "Enabled");
      }
      CallManager.initialize(this, new RingRtcLogger(), fieldTrials);
    } catch (UnsatisfiedLinkError e) {
      throw new AssertionError("Unable to load ringrtc library", e);
    }
  }

  @WorkerThread
  private void initializeCircumvention() {
    if (AppDependencies.getZonaRosaServiceNetworkAccess().isCensored()) {
      try {
        ProviderInstaller.installIfNeeded(ApplicationContext.this);
      } catch (Throwable t) {
        Log.w(TAG, t);
      }
    }
  }

  private void ensureProfileUploaded() {
    if (ZonaRosaStore.account().isRegistered() && !ZonaRosaStore.registration().hasUploadedProfile() && !Recipient.self().getProfileName().isEmpty() && ZonaRosaStore.account().isPrimaryDevice()) {
      Log.w(TAG, "User has a profile, but has not uploaded one. Uploading now.");
      AppDependencies.getJobManager().add(new ProfileUploadJob());
    }
  }

  private void executePendingContactSync() {
    if (ZonaRosaPreferences.needsFullContactSync(this)) {
      AppDependencies.getJobManager().add(new MultiDeviceContactUpdateJob(true));
    }
  }

  @VisibleForTesting
  protected void beginJobLoop() {
    AppDependencies.getJobManager().beginJobLoop();
  }

  @WorkerThread
  private void initializeBlobProvider() {
    BlobProvider.getInstance().initialize(this);
  }

  @WorkerThread
  private void cleanAvatarStorage() {
    AvatarPickerStorage.cleanOrphans(this);
  }

  @WorkerThread
  private void initializeCleanup() {
    int deleted = ZonaRosaDatabase.attachments().deleteAbandonedPreuploadedAttachments();
    Log.i(TAG, "Deleted " + deleted + " abandoned attachments.");
  }

  private void initializeGlideCodecs() {
    ZonaRosaGlideCodecs.setLogProvider(new io.zonarosa.glide.Log.Provider() {
      @Override
      public void v(@NonNull String tag, @NonNull String message) {
        Log.v(tag, message);
      }

      @Override
      public void d(@NonNull String tag, @NonNull String message) {
        Log.d(tag, message);
      }

      @Override
      public void i(@NonNull String tag, @NonNull String message) {
        Log.i(tag, message);
      }

      @Override
      public void w(@NonNull String tag, @NonNull String message) {
        Log.w(tag, message);
      }

      @Override
      public void e(@NonNull String tag, @NonNull String message, @Nullable Throwable throwable) {
        Log.e(tag, message, throwable);
      }
    });
  }

  @Override
  protected void attachBaseContext(Context base) {
    DynamicLanguageContextWrapper.updateContext(base);
    super.attachBaseContext(base);
  }

  private static class ProviderInitializationException extends RuntimeException {
  }
}

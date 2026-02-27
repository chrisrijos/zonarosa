/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger

import android.app.Application
import io.zonarosa.libzonarosa.net.Network
import io.zonarosa.messenger.database.JobDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.dependencies.ApplicationDependencyProvider
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobmanager.JobManager
import io.zonarosa.messenger.jobmanager.JobMigrator
import io.zonarosa.messenger.jobmanager.impl.FactoryJobPredicate
import io.zonarosa.messenger.jobs.AccountConsistencyWorkerJob
import io.zonarosa.messenger.jobs.ArchiveBackupIdReservationJob
import io.zonarosa.messenger.jobs.AttachmentCompressionJob
import io.zonarosa.messenger.jobs.AttachmentUploadJob
import io.zonarosa.messenger.jobs.CreateReleaseChannelJob
import io.zonarosa.messenger.jobs.DirectoryRefreshJob
import io.zonarosa.messenger.jobs.DownloadLatestEmojiDataJob
import io.zonarosa.messenger.jobs.EmojiSearchIndexDownloadJob
import io.zonarosa.messenger.jobs.FastJobStorage
import io.zonarosa.messenger.jobs.FontDownloaderJob
import io.zonarosa.messenger.jobs.GroupCallUpdateSendJob
import io.zonarosa.messenger.jobs.GroupRingCleanupJob
import io.zonarosa.messenger.jobs.GroupV2UpdateSelfProfileKeyJob
import io.zonarosa.messenger.jobs.IndividualSendJob
import io.zonarosa.messenger.jobs.JobManagerFactories
import io.zonarosa.messenger.jobs.LinkedDeviceInactiveCheckJob
import io.zonarosa.messenger.jobs.MarkerJob
import io.zonarosa.messenger.jobs.MultiDeviceProfileKeyUpdateJob
import io.zonarosa.messenger.jobs.PostRegistrationBackupRedemptionJob
import io.zonarosa.messenger.jobs.PreKeysSyncJob
import io.zonarosa.messenger.jobs.ProfileUploadJob
import io.zonarosa.messenger.jobs.PushGroupSendJob
import io.zonarosa.messenger.jobs.PushProcessMessageJob
import io.zonarosa.messenger.jobs.ReactionSendJob
import io.zonarosa.messenger.jobs.RefreshAttributesJob
import io.zonarosa.messenger.jobs.RetrieveRemoteAnnouncementsJob
import io.zonarosa.messenger.jobs.RotateCertificateJob
import io.zonarosa.messenger.jobs.SendDeliveryReceiptJob
import io.zonarosa.messenger.jobs.StickerPackDownloadJob
import io.zonarosa.messenger.jobs.StorageSyncJob
import io.zonarosa.messenger.jobs.StoryOnboardingDownloadJob
import io.zonarosa.messenger.jobs.TypingSendJob
import io.zonarosa.messenger.net.DeviceTransferBlockingInterceptor
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.service.api.util.UptimeSleepTimer
import io.zonarosa.service.api.websocket.ZonaRosaWebSocket
import io.zonarosa.service.internal.configuration.ZonaRosaServiceConfiguration
import io.zonarosa.service.internal.websocket.BenchmarkWebSocketConnection
import java.util.function.Supplier
import kotlin.time.Duration.Companion.seconds

class BenchmarkApplicationContext : ApplicationContext() {

  override fun initializeAppDependencies() {
    AppDependencies.init(this, BenchmarkDependencyProvider(this, ApplicationDependencyProvider(this)))

    DeviceTransferBlockingInterceptor.getInstance().blockNetwork()
  }

  override fun onForeground() = Unit

  class BenchmarkDependencyProvider(val application: Application, private val default: ApplicationDependencyProvider) : AppDependencies.Provider by default {
    override fun provideAuthWebSocket(
      zonarosaServiceConfigurationSupplier: Supplier<ZonaRosaServiceConfiguration>,
      libZonaRosaNetworkSupplier: Supplier<Network>
    ): ZonaRosaWebSocket.AuthenticatedWebSocket {
      return ZonaRosaWebSocket.AuthenticatedWebSocket(
        connectionFactory = { BenchmarkWebSocketConnection.createAuthInstance() },
        canConnect = { true },
        sleepTimer = UptimeSleepTimer(),
        disconnectTimeoutMs = 15.seconds.inWholeMilliseconds
      )
    }

    override fun provideUnauthWebSocket(
      zonarosaServiceConfigurationSupplier: Supplier<ZonaRosaServiceConfiguration>,
      libZonaRosaNetworkSupplier: Supplier<Network>
    ): ZonaRosaWebSocket.UnauthenticatedWebSocket {
      return ZonaRosaWebSocket.UnauthenticatedWebSocket(
        connectionFactory = { BenchmarkWebSocketConnection.createUnauthInstance() },
        canConnect = { true },
        sleepTimer = UptimeSleepTimer(),
        disconnectTimeoutMs = 15.seconds.inWholeMilliseconds
      )
    }

    override fun provideJobManager(): JobManager {
      val config = JobManager.Configuration.Builder()
        .setJobFactories(filterJobFactories(JobManagerFactories.getJobFactories(application)))
        .setConstraintFactories(JobManagerFactories.getConstraintFactories(application))
        .setConstraintObservers(JobManagerFactories.getConstraintObservers(application))
        .setJobStorage(FastJobStorage(JobDatabase.getInstance(application)))
        .setJobMigrator(JobMigrator(ZonaRosaPreferences.getJobManagerVersion(application), JobManager.CURRENT_VERSION, JobManagerFactories.getJobMigrations(application)))
        .addReservedJobRunner(FactoryJobPredicate(PushProcessMessageJob.KEY, MarkerJob.KEY))
        .addReservedJobRunner(FactoryJobPredicate(AttachmentUploadJob.KEY, AttachmentCompressionJob.KEY))
        .addReservedJobRunner(
          FactoryJobPredicate(
            IndividualSendJob.KEY,
            PushGroupSendJob.KEY,
            ReactionSendJob.KEY,
            TypingSendJob.KEY,
            GroupCallUpdateSendJob.KEY,
            SendDeliveryReceiptJob.KEY
          )
        )
        .build()
      return JobManager(application, config)
    }

    private fun filterJobFactories(jobFactories: Map<String, Job.Factory<*>>): Map<String, Job.Factory<*>> {
      val blockedJobs = setOf(
        AccountConsistencyWorkerJob.KEY,
        ArchiveBackupIdReservationJob.KEY,
        CreateReleaseChannelJob.KEY,
        DirectoryRefreshJob.KEY,
        DownloadLatestEmojiDataJob.KEY,
        EmojiSearchIndexDownloadJob.KEY,
        FontDownloaderJob.KEY,
        GroupRingCleanupJob.KEY,
        GroupV2UpdateSelfProfileKeyJob.KEY,
        LinkedDeviceInactiveCheckJob.KEY,
        MultiDeviceProfileKeyUpdateJob.KEY,
        PostRegistrationBackupRedemptionJob.KEY,
        PreKeysSyncJob.KEY,
        ProfileUploadJob.KEY,
        RefreshAttributesJob.KEY,
        RetrieveRemoteAnnouncementsJob.KEY,
        RotateCertificateJob.KEY,
        StickerPackDownloadJob.KEY,
        StorageSyncJob.KEY,
        StoryOnboardingDownloadJob.KEY
      )

      return jobFactories.mapValues {
        if (it.key in blockedJobs) {
          NoOpJob.Factory()
        } else {
          it.value
        }
      }
    }
  }

  private class NoOpJob(parameters: Parameters) : Job(parameters) {

    companion object {
      const val KEY = "NoOpJob"
    }

    override fun serialize(): ByteArray? = null
    override fun getFactoryKey(): String = KEY
    override fun run(): Result = Result.success()
    override fun onFailure() = Unit

    class Factory : Job.Factory<NoOpJob> {
      override fun create(parameters: Parameters, serializedData: ByteArray?): NoOpJob {
        return NoOpJob(parameters)
      }
    }
  }
}

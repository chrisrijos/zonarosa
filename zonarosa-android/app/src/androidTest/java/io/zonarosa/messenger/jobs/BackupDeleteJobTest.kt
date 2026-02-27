/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import okio.IOException
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.zonarosa.messenger.backup.DeletionState
import io.zonarosa.messenger.backup.v2.BackupRepository
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.jobs.protos.BackupDeleteJobData
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.NetworkResult
import io.zonarosa.service.api.push.exceptions.NonSuccessfulResponseCodeException

class BackupDeleteJobTest {

  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Before
  fun setUp() {
    mockkObject(RemoteConfig)
    every { RemoteConfig.internalUser } returns true
    every { RemoteConfig.defaultMaxBackoff } returns 1000L

    mockkObject(BackupRepository)
    every { BackupRepository.getBackupTier() } returns NetworkResult.Success(MessageBackupTier.PAID)
    every { BackupRepository.deleteBackup() } returns NetworkResult.Success(Unit)
    every { BackupRepository.deleteMediaBackup() } returns NetworkResult.Success(Unit)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun givenUserNotRegistered_whenIRun_thenIExpectFailure() {
    mockkObject(ZonaRosaStore) {
      every { ZonaRosaStore.account.isRegistered } returns false

      val job = BackupDeleteJob()

      val result = job.run()

      assertThat(result.isFailure).isTrue()
    }
  }

  @Test
  fun givenLinkedDevice_whenIRun_thenIExpectFailure() {
    mockkObject(ZonaRosaStore) {
      every { ZonaRosaStore.account.isRegistered } returns true
      every { ZonaRosaStore.account.isLinkedDevice } returns true

      val job = BackupDeleteJob()

      val result = job.run()

      assertThat(result.isFailure).isTrue()
    }
  }

  @Test
  fun givenDeletionStateNone_whenIRun_thenIExpectFailure() {
    ZonaRosaStore.backup.deletionState = DeletionState.NONE

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isFailure).isTrue()
  }

  @Test
  fun givenDeletionStateFailed_whenIRun_thenIExpectFailure() {
    ZonaRosaStore.backup.deletionState = DeletionState.FAILED

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isFailure).isTrue()
  }

  @Test
  fun givenDeletionStateComplete_whenIRun_thenIExpectFailure() {
    ZonaRosaStore.backup.deletionState = DeletionState.NONE

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isFailure).isTrue()
  }

  @Test
  fun givenDeletionStateAwaitingMediaDownload_whenIRun_thenIExpectRetry() {
    ZonaRosaStore.backup.deletionState = DeletionState.AWAITING_MEDIA_DOWNLOAD

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isRetry).isTrue()
  }

  @Test
  fun givenDeletionStateClearLocalState_whenIRun_thenIDeleteLocalState() {
    ZonaRosaStore.backup.deletionState = DeletionState.CLEAR_LOCAL_STATE

    val job = BackupDeleteJob()

    job.run()

    val jobData = BackupDeleteJobData.ADAPTER.decode(job.serialize())

    assertThat(ZonaRosaStore.backup.backupTier).isNull()
    assertThat(jobData.tier).isEqualTo(BackupDeleteJobData.Tier.PAID)
    assertThat(jobData.completed).contains(BackupDeleteJobData.Stage.CLEAR_LOCAL_STATE)
  }

  @Test
  fun givenDeletionStateClearLocalState_whenIRun_thenIUnsubscribe() {
    ZonaRosaStore.backup.deletionState = DeletionState.CLEAR_LOCAL_STATE

    val job = BackupDeleteJob()

    job.run()

    val jobData = BackupDeleteJobData.ADAPTER.decode(job.serialize())

    assertThat(ZonaRosaStore.backup.backupTier).isNull()
    assertThat(jobData.tier).isEqualTo(BackupDeleteJobData.Tier.PAID)
    assertThat(jobData.completed).contains(BackupDeleteJobData.Stage.CANCEL_SUBSCRIBER)
  }

  @Test
  fun givenMediaOffloaded_whenIRun_thenIExpectAwaitingMediaDownload() {
    mockkObject(ZonaRosaDatabase)
    every { ZonaRosaDatabase.attachments.getRemainingRestorableAttachmentSize() } returns 1
    every { ZonaRosaDatabase.attachments.getOptimizedMediaAttachmentSize() } returns 1
    every { ZonaRosaDatabase.attachments.clearAllArchiveData() } returns Unit

    ZonaRosaStore.backup.deletionState = DeletionState.CLEAR_LOCAL_STATE

    val job = BackupDeleteJob()
    val result = job.run()
    val jobData = BackupDeleteJobData.ADAPTER.decode(job.serialize())

    assertThat(ZonaRosaStore.backup.backupTier).isNull()
    assertThat(jobData.tier).isEqualTo(BackupDeleteJobData.Tier.PAID)
    assertThat(jobData.completed).contains(BackupDeleteJobData.Stage.CLEAR_LOCAL_STATE)
    assertThat(jobData.completed).contains(BackupDeleteJobData.Stage.CANCEL_SUBSCRIBER)

    assertThat(ZonaRosaStore.backup.deletionState).isEqualTo(DeletionState.AWAITING_MEDIA_DOWNLOAD)
    assertThat(result.isRetry).isTrue()
  }

  @Test
  fun givenMediaDownloadFinished_whenIRun_thenIExpectDeletion() {
    ZonaRosaStore.backup.deletionState = DeletionState.MEDIA_DOWNLOAD_FINISHED

    val job = BackupDeleteJob(
      backupDeleteJobData = BackupDeleteJobData(
        tier = BackupDeleteJobData.Tier.PAID,
        completed = listOf(
          BackupDeleteJobData.Stage.CLEAR_LOCAL_STATE,
          BackupDeleteJobData.Stage.CANCEL_SUBSCRIBER
        )
      )
    )

    val result = job.run()

    verify {
      BackupRepository.deleteBackup()
      BackupRepository.deleteMediaBackup()
      BackupRepository.resetInitializedStateAndAuthCredentials()
    }

    assertThat(result.isSuccess).isTrue()
    assertThat(ZonaRosaStore.backup.deletionState).isEqualTo(DeletionState.COMPLETE)
  }

  @Test
  fun givenNetworkErrorDuringMessageBackupDeletion_whenIRun_thenIExpectRetry() {
    every { BackupRepository.deleteBackup() } returns NetworkResult.NetworkError(IOException())

    ZonaRosaStore.backup.deletionState = DeletionState.CLEAR_LOCAL_STATE

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isRetry).isTrue()
  }

  @Test
  fun givenNetworkErrorDuringMediaBackupDeletion_whenIRun_thenIExpectRetry() {
    every { BackupRepository.deleteMediaBackup() } returns NetworkResult.NetworkError(IOException())

    ZonaRosaStore.backup.deletionState = DeletionState.CLEAR_LOCAL_STATE

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isRetry).isTrue()
  }

  @Test
  fun givenRateLimitedDuringMessageBackupDeletion_whenIRun_thenIExpectRetry() {
    every { BackupRepository.deleteBackup() } returns NetworkResult.StatusCodeError(NonSuccessfulResponseCodeException(429))

    ZonaRosaStore.backup.deletionState = DeletionState.CLEAR_LOCAL_STATE

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isRetry).isTrue()
  }

  @Test
  fun givenRateLimitedDuringMediaBackupDeletion_whenIRun_thenIExpectRetry() {
    every { BackupRepository.deleteMediaBackup() } returns NetworkResult.StatusCodeError(NonSuccessfulResponseCodeException(429))

    ZonaRosaStore.backup.deletionState = DeletionState.CLEAR_LOCAL_STATE

    val job = BackupDeleteJob()

    val result = job.run()

    assertThat(result.isRetry).isTrue()
  }
}

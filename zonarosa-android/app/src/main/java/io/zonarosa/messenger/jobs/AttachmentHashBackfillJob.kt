/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.core.util.ThreadUtil
import io.zonarosa.core.util.drain
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.attachments.AttachmentId
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.security.DigestInputStream
import java.security.MessageDigest

/**
 * This job backfills hashes for attachments that were sent before we started hashing them.
 * In order to avoid hammering the device with hash calculations and disk I/O, this job will
 * calculate the hash for a single attachment and then reschedule itself to run again if necessary.
 */
class AttachmentHashBackfillJob private constructor(parameters: Parameters) : Job(parameters) {

  companion object {
    val TAG = Log.tag(AttachmentHashBackfillJob::class.java)

    const val KEY = "AttachmentHashBackfillJob"
  }

  private var activeFile: File? = null

  constructor() : this(
    Parameters.Builder()
      .setQueue(KEY)
      .setMaxInstancesForFactory(2)
      .setLifespan(Parameters.IMMORTAL)
      .setMaxAttempts(10)
      .build()
  )

  override fun serialize() = null

  override fun getFactoryKey() = KEY

  override fun run(): Result {
    val (file: File?, attachmentId: AttachmentId?) = ZonaRosaDatabase.attachments.getUnhashedDataFile() ?: (null to null)
    if (file == null || attachmentId == null) {
      Log.i(TAG, "No more unhashed files! Task complete.")
      return Result.success()
    }

    activeFile = file

    if (!file.exists()) {
      Log.w(TAG, "File does not exist! Clearing all usages.", true)
      ZonaRosaDatabase.attachments.clearUsagesOfDataFile(file)
      AppDependencies.jobManager.add(AttachmentHashBackfillJob())
      return Result.success()
    }

    try {
      val inputStream = ZonaRosaDatabase.attachments.getAttachmentStream(attachmentId, 0)
      val messageDigest = MessageDigest.getInstance("SHA-256")

      DigestInputStream(inputStream, messageDigest).use {
        it.drain()
      }

      val hash = messageDigest.digest()

      ZonaRosaDatabase.attachments.setHashForDataFile(file, hash)
    } catch (e: FileNotFoundException) {
      Log.w(TAG, "File could not be found! Clearing all usages.", true)
      ZonaRosaDatabase.attachments.clearUsagesOfDataFile(file)
    } catch (e: IOException) {
      Log.e(TAG, "Error hashing attachment. Retrying.", e)

      if (e.cause is FileNotFoundException) {
        Log.w(TAG, "Underlying cause was a FileNotFoundException. Clearing all usages.", true)
        ZonaRosaDatabase.attachments.clearUsagesOfDataFile(file)
      } else {
        return Result.retry(defaultBackoff())
      }
    }

    // Sleep just so we don't hammer the device with hash calculations and disk I/O
    ThreadUtil.sleep(1000)

    AppDependencies.jobManager.add(AttachmentHashBackfillJob())
    return Result.success()
  }

  override fun onFailure() {
    activeFile?.let { file ->
      Log.w(TAG, "Failed to calculate hash, marking as unhashable: $file", true)
      ZonaRosaDatabase.attachments.markDataFileAsUnhashable(file)
    } ?: Log.w(TAG, "Job failed, but no active file is set!")

    AppDependencies.jobManager.add(AttachmentHashBackfillJob())
  }

  class Factory : Job.Factory<AttachmentHashBackfillJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): AttachmentHashBackfillJob {
      return AttachmentHashBackfillJob(parameters)
    }
  }
}

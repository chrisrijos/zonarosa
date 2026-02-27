/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import io.zonarosa.core.models.media.TransformProperties
import io.zonarosa.core.util.StreamUtil
import io.zonarosa.messenger.attachments.UriAttachment
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.UriAttachmentBuilder
import io.zonarosa.messenger.database.transformPropertiesForSentMediaQuality
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.mms.SentMediaQuality
import io.zonarosa.messenger.providers.BlobProvider
import io.zonarosa.messenger.testing.ZonaRosaActivityRule
import io.zonarosa.messenger.util.MediaUtil
import java.util.Optional
import java.util.concurrent.CountDownLatch

@RunWith(AndroidJUnit4::class)
class AttachmentCompressionJobTest {

  @get:Rule
  val harness = ZonaRosaActivityRule()

  @Test
  fun testCompressionJobsWithDifferentTransformPropertiesCompleteSuccessfully() {
    val imageBytes: ByteArray = InstrumentationRegistry.getInstrumentation().context.resources.assets.open("images/sample_image.png").use {
      StreamUtil.readFully(it)
    }

    val blob = BlobProvider.getInstance().forData(imageBytes).createForSingleSessionOnDisk(AppDependencies.application)

    val firstPreUpload = createAttachment(1, blob, TransformProperties.empty())
    val firstDatabaseAttachment = ZonaRosaDatabase.attachments.insertAttachmentForPreUpload(firstPreUpload)

    val firstCompressionJob: AttachmentCompressionJob = AttachmentCompressionJob.fromAttachment(firstDatabaseAttachment, false, -1)

    var secondCompressionJob: AttachmentCompressionJob? = null
    var firstJobResult: Job.Result? = null
    var secondJobResult: Job.Result? = null

    val secondJobLatch = CountDownLatch(1)
    val jobThread = Thread {
      firstCompressionJob.setContext(AppDependencies.application)
      firstJobResult = firstCompressionJob.run()

      secondJobLatch.await()

      secondCompressionJob!!.setContext(AppDependencies.application)
      secondJobResult = secondCompressionJob!!.run()
    }

    jobThread.start()
    val secondPreUpload = createAttachment(1, blob, transformPropertiesForSentMediaQuality(Optional.empty(), SentMediaQuality.HIGH))
    val secondDatabaseAttachment = ZonaRosaDatabase.attachments.insertAttachmentForPreUpload(secondPreUpload)
    secondCompressionJob = AttachmentCompressionJob.fromAttachment(secondDatabaseAttachment, false, -1)

    secondJobLatch.countDown()

    jobThread.join()

    assertThat(firstJobResult!!.isSuccess).isTrue()
    assertThat(secondJobResult!!.isSuccess).isTrue()
  }

  private fun createAttachment(id: Long, uri: Uri, transformProperties: TransformProperties): UriAttachment {
    return UriAttachmentBuilder.build(
      id,
      uri = uri,
      contentType = MediaUtil.IMAGE_JPEG,
      transformProperties = transformProperties
    )
  }
}

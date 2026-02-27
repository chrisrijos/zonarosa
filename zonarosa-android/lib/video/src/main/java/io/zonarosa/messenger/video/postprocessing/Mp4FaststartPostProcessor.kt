/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.video.postprocessing

import io.zonarosa.core.util.readLength
import io.zonarosa.core.util.stream.LimitedInputStream
import io.zonarosa.libzonarosa.media.Mp4Sanitizer
import io.zonarosa.libzonarosa.media.SanitizedMetadata
import io.zonarosa.messenger.video.exceptions.VideoPostProcessingException
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream

/**
 * A post processor that takes a stream of bytes, and using [Mp4Sanitizer], moves the metadata to the front of the file.
 *
 * @property inputStreamFactory factory for the [InputStream]. Expected to be called multiple times.
 */
class Mp4FaststartPostProcessor(private val inputStreamFactory: InputStreamFactory) {

  /**
   * It is the responsibility of the caller to close the resulting [InputStream].
   */
  fun process(inputLength: Long = calculateStreamLength(inputStreamFactory.create())): SequenceInputStream {
    val metadata = inputStreamFactory.create().use { inputStream ->
      sanitizeMetadata(inputStream, inputLength)
    }
    if (metadata.sanitizedMetadata == null) {
      throw VideoPostProcessingException("Sanitized metadata was null!")
    }
    val inputStream = inputStreamFactory.create()
    inputStream.skip(metadata.dataOffset)
    return SequenceInputStream(ByteArrayInputStream(metadata.sanitizedMetadata), LimitedInputStream(inputStream, metadata.dataLength))
  }

  fun processAndWriteTo(outputStream: OutputStream, inputLength: Long = calculateStreamLength(inputStreamFactory.create())): Long {
    process(inputLength).use { inStream ->
      return inStream.copyTo(outputStream)
    }
  }

  /**
   * It is the responsibility of the caller to close the resulting [InputStream].
   */
  fun processWithMdatLength(inputLength: Long, mdatLength: Int): SequenceInputStream {
    val metadata = inputStreamFactory.create().use { inputStream ->
      inputStream.use {
        Mp4Sanitizer.sanitizeFileWithCompoundedMdatBoxes(it, inputLength, mdatLength)
      }
    }
    if (metadata.sanitizedMetadata == null) {
      throw VideoPostProcessingException("Sanitized metadata was null!")
    }
    val inputStream = inputStreamFactory.create()
    inputStream.skip(metadata.dataOffset)
    return SequenceInputStream(ByteArrayInputStream(metadata.sanitizedMetadata), LimitedInputStream(inputStream, metadata.dataLength))
  }

  fun interface InputStreamFactory {
    fun create(): InputStream
  }

  companion object {
    const val TAG = "Mp4Faststart"

    @JvmStatic
    fun calculateStreamLength(inputStream: InputStream): Long {
      inputStream.use {
        return it.readLength()
      }
    }

    @JvmStatic
    private fun sanitizeMetadata(inputStream: InputStream, inputLength: Long): SanitizedMetadata {
      inputStream.use {
        return Mp4Sanitizer.sanitize(it, inputLength)
      }
    }
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.video.videoconverter.mediadatasource

import android.media.MediaDataSource
import java.io.IOException
import java.io.InputStream

/**
 * Extend this class in order to be able to use the system media framework with any arbitrary [InputStream] of bytes.
 */
abstract class InputStreamMediaDataSource : MediaDataSource() {
  @Throws(IOException::class)
  override fun readAt(position: Long, bytes: ByteArray?, offset: Int, length: Int): Int {
    if (position >= size) {
      return -1
    }

    createInputStream(position).use { inputStream ->
      var totalRead = 0
      while (totalRead < length) {
        val read: Int = inputStream.read(bytes, offset + totalRead, length - totalRead)
        if (read == -1) {
          return if (totalRead == 0) {
            -1
          } else {
            totalRead
          }
        }
        totalRead += read
      }
      return totalRead
    }
  }

  abstract override fun close()

  abstract override fun getSize(): Long

  @Throws(IOException::class)
  abstract fun createInputStream(position: Long): InputStream
}

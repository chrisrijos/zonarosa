/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.stream

import io.zonarosa.core.util.writeVarInt32
import io.zonarosa.messenger.backup.v2.proto.BackupInfo
import io.zonarosa.messenger.backup.v2.proto.Frame
import java.io.IOException
import java.io.OutputStream

/**
 * Writes backup frames to the wrapped stream in plain text. Only for testing!
 */
class PlainTextBackupWriter(private val outputStream: OutputStream) : BackupExportWriter {

  @Throws(IOException::class)
  override fun write(header: BackupInfo) {
    val headerBytes: ByteArray = header.encode()

    outputStream.writeVarInt32(headerBytes.size)
    outputStream.write(headerBytes)
  }

  @Throws(IOException::class)
  override fun write(frame: Frame) {
    val frameBytes: ByteArray = frame.encode()

    outputStream.writeVarInt32(frameBytes.size)
    outputStream.write(frameBytes)
  }

  override fun close() {
    outputStream.close()
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.local

import io.zonarosa.core.util.writeVarInt32
import io.zonarosa.messenger.backup.v2.local.proto.FilesFrame
import java.io.IOException
import java.io.OutputStream

/**
 * Write [FilesFrame] protos encoded with their length.
 */
class ArchivedFilesWriter(private val output: OutputStream) : AutoCloseable {

  @Throws(IOException::class)
  fun write(frame: FilesFrame) {
    val bytes = frame.encode()
    output.writeVarInt32(bytes.size)
    output.write(bytes)
  }

  override fun close() {
    output.close()
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.stream

import org.junit.Assert.assertEquals
import org.junit.Test
import io.zonarosa.core.models.ServiceId.ACI
import io.zonarosa.core.models.backup.MessageBackupKey
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Hex
import io.zonarosa.core.util.Util
import io.zonarosa.libzonarosa.messagebackup.BackupForwardSecrecyToken
import io.zonarosa.messenger.backup.v2.proto.AccountData
import io.zonarosa.messenger.backup.v2.proto.BackupInfo
import io.zonarosa.messenger.backup.v2.proto.Frame
import java.io.ByteArrayOutputStream
import java.util.UUID

class EncryptedBackupReaderWriterTest {

  @Test
  fun `can read back all of the frames we write`() {
    val key = MessageBackupKey(Util.getSecretBytes(32))
    val aci = ACI.from(UUID.randomUUID())

    val outputStream = ByteArrayOutputStream()

    val frameCount = 10_000
    EncryptedBackupWriter.createForLocalOrLinking(key, aci, outputStream, append = { outputStream.write(it) }).use { writer ->
      writer.write(BackupInfo(version = 1, backupTimeMs = 1000L))

      for (i in 0 until frameCount) {
        writer.write(Frame(account = AccountData(username = "username-$i")))
      }
    }

    val ciphertext: ByteArray = outputStream.toByteArray()
    println(ciphertext.size)

    val frames: List<Frame> = EncryptedBackupReader.createForLocalOrLinking(key, aci, ciphertext.size.toLong()) { ciphertext.inputStream() }.use { reader ->
      assertEquals(reader.backupInfo?.version, 1L)
      assertEquals(reader.backupInfo?.backupTimeMs, 1000L)
      reader.asSequence().toList()
    }

    assertEquals(frameCount, frames.size)

    for (i in 0 until frameCount) {
      assertEquals("username-$i", frames[i].account?.username)
    }
  }

  @Test
  fun `padding limits number of sizes`() {
    val key = MessageBackupKey(Util.getSecretBytes(32))
    val aci = ACI.from(UUID.randomUUID())

    val uniqueSizes = (1..10)
      .map { frameCount ->
        val outputStream = ByteArrayOutputStream()

        EncryptedBackupWriter.createForLocalOrLinking(key, aci, outputStream, append = { outputStream.write(it) }).use { writer ->
          writer.write(BackupInfo(version = 1, backupTimeMs = 1000L))

          for (i in 0 until frameCount) {
            writer.write(Frame(account = AccountData(username = Hex.toStringCondensed(Util.getSecretBytes(32)))))
          }
        }

        outputStream.toByteArray().size
      }
      .toSet()

    assertEquals(1, uniqueSizes.size)
  }

  @Test
  fun `using a different IV every time`() {
    val key = MessageBackupKey(Util.getSecretBytes(32))
    val aci = ACI.from(UUID.randomUUID())
    val count = 10

    val uniqueOutputs = (0 until count)
      .map {
        val outputStream = ByteArrayOutputStream()

        EncryptedBackupWriter.createForLocalOrLinking(key, aci, outputStream, append = { outputStream.write(it) }).use { writer ->
          writer.write(BackupInfo(version = 1, backupTimeMs = 1000L))
          writer.write(Frame(account = AccountData(username = "static-data")))
        }

        outputStream.toByteArray()
      }
      .map { Base64.encodeWithPadding(it) }
      .toSet()

    assertEquals(count, uniqueOutputs.size)
  }

  @Test
  fun `can read back all of the frames we write - forward secrecy`() {
    val key = MessageBackupKey(Util.getSecretBytes(32))
    val aci = ACI.from(UUID.randomUUID())

    val outputStream = ByteArrayOutputStream()

    val forwardSecrecyToken = BackupForwardSecrecyToken(Util.getSecretBytes(32))

    val frameCount = 10_000
    EncryptedBackupWriter.createForZonaRosaBackup(
      key = key,
      aci = aci,
      forwardSecrecyToken = forwardSecrecyToken,
      forwardSecrecyMetadata = Util.getSecretBytes(64),
      outputStream = outputStream,
      append = { outputStream.write(it) }
    ).use { writer ->
      writer.write(BackupInfo(version = 1, backupTimeMs = 1000L))

      for (i in 0 until frameCount) {
        writer.write(Frame(account = AccountData(username = "username-$i")))
      }
    }

    val ciphertext: ByteArray = outputStream.toByteArray()
    println(ciphertext.size)

    val frames: List<Frame> = EncryptedBackupReader.createForZonaRosaBackup(key, aci, forwardSecrecyToken, ciphertext.size.toLong()) { ciphertext.inputStream() }.use { reader ->
      assertEquals(reader.backupInfo?.version, 1L)
      assertEquals(reader.backupInfo?.backupTimeMs, 1000L)
      reader.asSequence().toList()
    }

    assertEquals(frameCount, frames.size)

    for (i in 0 until frameCount) {
      assertEquals("username-$i", frames[i].account?.username)
    }
  }
}

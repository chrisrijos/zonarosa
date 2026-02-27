/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.processor

import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.Hex
import io.zonarosa.core.util.insertInto
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.backup.v2.ExportSkips
import io.zonarosa.messenger.backup.v2.proto.Frame
import io.zonarosa.messenger.backup.v2.proto.StickerPack
import io.zonarosa.messenger.backup.v2.stream.BackupFrameEmitter
import io.zonarosa.messenger.database.SQLiteDatabase
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.StickerTable
import io.zonarosa.messenger.database.StickerTable.StickerPackRecordReader
import io.zonarosa.messenger.database.model.StickerPackRecord
import java.io.IOException

private val TAG = Log.tag(StickerArchiveProcessor::class)

/**
 * Handles importing/exporting [StickerPack] frames for an archive.
 */
object StickerArchiveProcessor {
  fun export(db: ZonaRosaDatabase, emitter: BackupFrameEmitter) {
    StickerPackRecordReader(db.stickerTable.getAllStickerPacks()).use { reader ->
      var record: StickerPackRecord? = null
      while (reader.getNext()?.let { record = it } != null) {
        if (record!!.isInstalled) {
          val frame = record!!.toBackupFrame() ?: continue
          emitter.emit(frame)
        }
      }
    }
  }

  fun import(stickerPack: StickerPack) {
    ZonaRosaDatabase.rawDatabase
      .insertInto(StickerTable.TABLE_NAME)
      .values(
        StickerTable.PACK_ID to Hex.toStringCondensed(stickerPack.packId.toByteArray()),
        StickerTable.PACK_KEY to Hex.toStringCondensed(stickerPack.packKey.toByteArray()),
        StickerTable.PACK_TITLE to "",
        StickerTable.PACK_AUTHOR to "",
        StickerTable.INSTALLED to 1,
        StickerTable.COVER to 1,
        StickerTable.EMOJI to "",
        StickerTable.CONTENT_TYPE to "",
        StickerTable.FILE_PATH to ""
      )
      .run(SQLiteDatabase.CONFLICT_IGNORE)
  }
}

private fun StickerPackRecord.toBackupFrame(): Frame? {
  val packIdBytes = try {
    Hex.fromStringCondensed(this.packId)?.takeIf { it.size == 16 } ?: throw IOException("Incorrect length!")
  } catch (e: IOException) {
    Log.w(TAG, ExportSkips.invalidStickerPackId())
    return null
  }

  val packKeyBytes = try {
    Hex.fromStringCondensed(this.packKey)?.takeIf { it.size == 32 } ?: throw IOException("Incorrect length!")
  } catch (e: IOException) {
    Log.w(TAG, ExportSkips.invalidStickerPackKey())
    return null
  }

  val pack = StickerPack(
    packId = packIdBytes.toByteString(),
    packKey = packKeyBytes.toByteString()
  )
  return Frame(stickerPack = pack)
}

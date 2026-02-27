/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import android.content.Context
import android.database.Cursor
import io.zonarosa.core.util.Util
import io.zonarosa.core.util.delete
import io.zonarosa.core.util.insertInto
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.readToList
import io.zonarosa.core.util.readToSingleLongOrNull
import io.zonarosa.core.util.requireBlob
import io.zonarosa.core.util.requireNonNullString
import io.zonarosa.core.util.select
import io.zonarosa.core.util.update
import io.zonarosa.core.util.withinTransaction
import io.zonarosa.messenger.attachments.AttachmentMetadata
import io.zonarosa.messenger.attachments.LocalBackupKey

/**
 * Metadata for various attachments. There is a many-to-one relationship with the Attachment table as this metadata
 * represents data about a specific data file (plaintext hash).
 */
class AttachmentMetadataTable(context: Context, databaseHelper: ZonaRosaDatabase) : DatabaseTable(context, databaseHelper) {
  companion object {
    private val TAG = Log.tag(AttachmentMetadataTable::class)

    const val TABLE_NAME = "attachment_metadata"
    const val ID = "_id"
    const val PLAINTEXT_HASH = "plaintext_hash"
    const val LOCAL_BACKUP_KEY = "local_backup_key"

    const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $ID INTEGER PRIMARY KEY,
        $PLAINTEXT_HASH TEXT NOT NULL,
        $LOCAL_BACKUP_KEY BLOB DEFAULT NULL,
        UNIQUE ($PLAINTEXT_HASH)
      )
    """

    val PROJECTION = arrayOf(LOCAL_BACKUP_KEY)

    /**
     * Attempts to load metadata from the cursor if present. Returns null iff the cursor contained no
     * metadata columns (i.e., no join in the original query). If there are columns, but they are null, the contents of the
     * returned [AttachmentMetadata] will be null.
     */
    fun getMetadata(cursor: Cursor, localBackupKeyColumn: String = LOCAL_BACKUP_KEY): AttachmentMetadata? {
      if (cursor.getColumnIndex(localBackupKeyColumn) >= 0) {
        val localBackupKey = cursor.requireBlob(localBackupKeyColumn)?.let { LocalBackupKey(it) }
        return AttachmentMetadata(localBackupKey)
      }
      return null
    }
  }

  fun insert(plaintextHash: String, localBackupKey: ByteArray): Long {
    val rowId = writableDatabase
      .insertInto(TABLE_NAME)
      .values(PLAINTEXT_HASH to plaintextHash, LOCAL_BACKUP_KEY to localBackupKey)
      .run(conflictStrategy = SQLiteDatabase.CONFLICT_IGNORE)

    if (rowId > 0) {
      return rowId
    }

    return readableDatabase
      .select(ID)
      .from(TABLE_NAME)
      .where("$PLAINTEXT_HASH = ?", plaintextHash)
      .run()
      .readToSingleLongOrNull()!!
  }

  fun cleanup() {
    writableDatabase
      .delete(TABLE_NAME)
      .where("$ID NOT IN (SELECT DISTINCT ${AttachmentTable.METADATA_ID} FROM ${AttachmentTable.TABLE_NAME})")
      .run()
  }

  fun insertNewKeysForExistingAttachments() {
    writableDatabase.withinTransaction {
      do {
        val hashes: List<String> = readableDatabase
          .select("DISTINCT ${AttachmentTable.DATA_HASH_END}")
          .from(AttachmentTable.TABLE_NAME)
          .where("${AttachmentTable.DATA_HASH_END} IS NOT NULL AND ${AttachmentTable.DATA_FILE} IS NOT NULL AND ${AttachmentTable.METADATA_ID} IS NULL")
          .limit(1000)
          .run()
          .readToList { it.requireNonNullString(AttachmentTable.DATA_HASH_END) }

        if (hashes.isNotEmpty()) {
          val newKeys: List<Pair<String, ByteArray>> = hashes.zip(hashes.map { Util.getSecretBytes(64) })

          newKeys.forEach { (hash, key) ->
            var rowId = writableDatabase
              .insertInto(TABLE_NAME)
              .values(PLAINTEXT_HASH to hash, LOCAL_BACKUP_KEY to key)
              .run(conflictStrategy = SQLiteDatabase.CONFLICT_IGNORE)

            if (rowId == -1L) {
              rowId = readableDatabase
                .select(ID)
                .from(TABLE_NAME)
                .where("$PLAINTEXT_HASH = ?", hash)
                .run()
                .readToSingleLongOrNull()!!
            }

            writableDatabase
              .update(AttachmentTable.TABLE_NAME)
              .values(AttachmentTable.METADATA_ID to rowId)
              .where("${AttachmentTable.DATA_HASH_END} = ?", hash)
              .run()
          }
        }
      } while (hashes.isNotEmpty())
    }
  }
}

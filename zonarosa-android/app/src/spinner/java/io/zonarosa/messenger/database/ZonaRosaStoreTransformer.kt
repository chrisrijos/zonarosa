/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import android.database.Cursor
import com.squareup.wire.ProtoAdapter
import io.zonarosa.core.util.requireBlob
import io.zonarosa.core.util.requireString
import io.zonarosa.spinner.ColumnTransformer
import io.zonarosa.spinner.DefaultColumnTransformer
import io.zonarosa.messenger.database.model.databaseprotos.RestoreDecisionState
import io.zonarosa.messenger.keyvalue.BackupValues
import io.zonarosa.messenger.keyvalue.RegistrationValues
import io.zonarosa.messenger.keyvalue.protos.ArchiveUploadProgressState

/**
 * Transform non-user friendly store values into less-non-user friendly representations.
 */
object ZonaRosaStoreTransformer : ColumnTransformer {
  override fun matches(tableName: String?, columnName: String): Boolean {
    return columnName == KeyValueDatabase.VALUE
  }

  override fun transform(tableName: String?, columnName: String, cursor: Cursor): String? {
    return when (cursor.requireString(KeyValueDatabase.KEY)) {
      RegistrationValues.RESTORE_DECISION_STATE -> decodeProto(cursor, RestoreDecisionState.ADAPTER)
      BackupValues.KEY_ARCHIVE_UPLOAD_STATE -> decodeProto(cursor, ArchiveUploadProgressState.ADAPTER)
      else -> DefaultColumnTransformer.transform(tableName, columnName, cursor)
    }
  }

  private fun decodeProto(cursor: Cursor, adapter: ProtoAdapter<*>): String? {
    return cursor.requireBlob(KeyValueDatabase.VALUE)?.let { adapter.decode(it) }?.toString()
  }
}

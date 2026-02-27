/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import android.database.Cursor
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.requireBlob
import io.zonarosa.libzonarosa.protocol.state.KyberPreKeyRecord
import io.zonarosa.spinner.ColumnTransformer

object KyberKeyTransformer : ColumnTransformer {
  override fun matches(tableName: String?, columnName: String): Boolean {
    return tableName == KyberPreKeyTable.TABLE_NAME && columnName == KyberPreKeyTable.SERIALIZED
  }

  override fun transform(tableName: String?, columnName: String, cursor: Cursor): String? {
    val record = KyberPreKeyRecord(cursor.requireBlob(columnName))
    return "ID: ${record.id}\nTimestamp: ${record.timestamp}\nPublicKey: ${Base64.encodeWithoutPadding(record.keyPair.publicKey.serialize())}\nPrivateKey: ${Base64.encodeWithoutPadding(record.keyPair.secretKey.serialize())}\nSignature: ${Base64.encodeWithoutPadding(record.signature)}"
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import okio.IOException

@Throws(IOException::class)
fun ContentResolver.getLength(uri: Uri): Long? {
  return this.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
      cursor.requireLongOrNull(OpenableColumns.SIZE)
    } else {
      null
    }
  } ?: openInputStream(uri)?.use { it.readLength() }
}

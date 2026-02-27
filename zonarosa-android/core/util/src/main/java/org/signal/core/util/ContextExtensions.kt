/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.core.util

import android.app.DownloadManager
import android.content.Context

fun Context.getDownloadManager(): DownloadManager {
  return this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
}

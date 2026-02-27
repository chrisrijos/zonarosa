/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup

object BackupVersions {
  const val CURRENT_VERSION = 1
  const val MINIMUM_VERSION = 0

  @JvmStatic
  fun isCompatible(version: Int): Boolean {
    return version in MINIMUM_VERSION..CURRENT_VERSION
  }

  @JvmStatic
  fun isFrameLengthEncrypted(version: Int): Boolean {
    return version >= 1
  }
}

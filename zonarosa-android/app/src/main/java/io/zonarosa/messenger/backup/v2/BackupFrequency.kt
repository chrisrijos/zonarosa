/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2

import io.zonarosa.core.util.LongSerializer

/**
 * Describes how often a users messages are backed up.
 */
enum class BackupFrequency(val id: Int) {
  DAILY(0),
  WEEKLY(1),
  MONTHLY(2),
  MANUAL(-1);

  companion object Serializer : LongSerializer<BackupFrequency> {
    override fun serialize(data: BackupFrequency): Long {
      return data.id.toLong()
    }

    override fun deserialize(data: Long): BackupFrequency {
      return when (data.toInt()) {
        MANUAL.id -> MANUAL
        DAILY.id -> DAILY
        WEEKLY.id -> WEEKLY
        MONTHLY.id -> MONTHLY
        else -> MANUAL
      }
    }
  }
}

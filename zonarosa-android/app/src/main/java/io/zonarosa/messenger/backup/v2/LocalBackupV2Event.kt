/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2

class LocalBackupV2Event(val type: Type, val count: Long = 0, val estimatedTotalCount: Long = 0) {
  enum class Type {
    PROGRESS_ACCOUNT,
    PROGRESS_RECIPIENT,
    PROGRESS_THREAD,
    PROGRESS_CALL,
    PROGRESS_STICKER,
    NOTIFICATION_PROFILE,
    CHAT_FOLDER,
    PROGRESS_MESSAGE,
    PROGRESS_ATTACHMENT,
    FINISHED
  }
}

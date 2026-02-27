/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui.subscription

import androidx.compose.runtime.Stable
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import kotlin.time.Duration

/**
 * Represents a type of backup a user can select.
 */
@Stable
sealed interface MessageBackupsType {

  val tier: MessageBackupTier

  data class Paid(
    val pricePerMonth: FiatMoney,
    val storageAllowanceBytes: Long,
    val mediaTtl: Duration
  ) : MessageBackupsType {
    override val tier: MessageBackupTier = MessageBackupTier.PAID
  }

  data class Free(
    val mediaRetentionDays: Int
  ) : MessageBackupsType {
    override val tier: MessageBackupTier = MessageBackupTier.FREE
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui.subscription

import androidx.compose.runtime.Immutable
import io.zonarosa.core.models.AccountEntropyPool
import io.zonarosa.core.util.billing.BillingResponseCode
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.components.settings.app.backups.remote.BackupKeySaveState
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

@Immutable
data class MessageBackupsFlowState(
  val selectedMessageBackupTier: MessageBackupTier? = ZonaRosaStore.backup.backupTier,
  val currentMessageBackupTier: MessageBackupTier? = null,
  val allBackupTypes: List<MessageBackupsType> = emptyList(),
  val googlePlayApiAvailability: GooglePlayServicesAvailability = GooglePlayServicesAvailability.SUCCESS,
  val googlePlayBillingAvailability: BillingResponseCode = BillingResponseCode.FEATURE_NOT_SUPPORTED,
  val inAppPayment: InAppPaymentTable.InAppPayment? = null,
  val startScreen: MessageBackupsStage,
  val stage: MessageBackupsStage = startScreen,
  val accountEntropyPool: AccountEntropyPool = ZonaRosaStore.account.accountEntropyPool,
  val failure: Throwable? = null,
  val paymentReadyState: PaymentReadyState = PaymentReadyState.NOT_READY,
  val backupKeySaveState: BackupKeySaveState? = null
) {
  enum class PaymentReadyState {
    NOT_READY,
    READY,
    FAILED
  }

  /**
   * Whether or not the 'next' button on the type selection screen is enabled.
   */
  fun isCheckoutButtonEnabled(): Boolean {
    return selectedMessageBackupTier in allBackupTypes.map { it.tier } &&
      selectedMessageBackupTier != currentMessageBackupTier &&
      paymentReadyState == PaymentReadyState.READY
  }
}

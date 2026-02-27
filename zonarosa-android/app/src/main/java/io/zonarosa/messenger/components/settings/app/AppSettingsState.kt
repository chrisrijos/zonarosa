package io.zonarosa.messenger.components.settings.app

import androidx.compose.runtime.Immutable
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.Environment
import io.zonarosa.messenger.util.RemoteConfig

@Immutable
data class AppSettingsState(
  val isPrimaryDevice: Boolean,
  val unreadPaymentsCount: Int,
  val hasExpiredGiftBadge: Boolean,
  val allowUserToGoToDonationManagementScreen: Boolean,
  val userUnregistered: Boolean,
  val clientDeprecated: Boolean,
  val showInternalPreferences: Boolean = RemoteConfig.internalUser,
  val showPayments: Boolean = ZonaRosaStore.payments.paymentsAvailability.showPaymentsMenu(),
  val showAppUpdates: Boolean = Environment.IS_NIGHTLY,
  val backupFailureState: BackupFailureState = BackupFailureState.NONE
) {
  fun isRegisteredAndUpToDate(): Boolean {
    return !userUnregistered && !clientDeprecated
  }
}

package io.zonarosa.messenger.components.settings.app

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.zonarosa.messenger.components.settings.app.subscription.InAppDonations
import io.zonarosa.messenger.components.settings.app.subscription.RecurringInAppPaymentRepository
import io.zonarosa.messenger.conversationlist.model.UnreadPaymentsLiveData
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.util.ZonaRosaPreferences
import io.zonarosa.messenger.util.livedata.Store

class AppSettingsViewModel : ViewModel() {

  private val store = Store(
    AppSettingsState(
      isPrimaryDevice = ZonaRosaStore.account.isPrimaryDevice,
      unreadPaymentsCount = 0,
      hasExpiredGiftBadge = ZonaRosaStore.inAppPayments.getExpiredGiftBadge() != null,
      allowUserToGoToDonationManagementScreen = ZonaRosaStore.inAppPayments.isLikelyASustainer() || InAppDonations.hasAtLeastOnePaymentMethodAvailable(),
      userUnregistered = ZonaRosaPreferences.isUnauthorizedReceived(AppDependencies.application) || !ZonaRosaStore.account.isRegistered,
      clientDeprecated = ZonaRosaStore.misc.isClientDeprecated
    )
  )

  private val unreadPaymentsLiveData = UnreadPaymentsLiveData()
  private val disposables = CompositeDisposable()

  val state: LiveData<AppSettingsState> = store.stateLiveData
  val self: LiveData<BioRecipientState> = Recipient.self().live().liveData.map { BioRecipientState(it) }

  init {
    store.update(unreadPaymentsLiveData) { payments, state -> state.copy(unreadPaymentsCount = payments.map { it.unreadCount }.orElse(0)) }

    disposables += RecurringInAppPaymentRepository.getActiveSubscription(InAppPaymentSubscriberRecord.Type.DONATION).subscribeBy(
      onSuccess = { activeSubscription ->
        store.update { state ->
          state.copy(allowUserToGoToDonationManagementScreen = ZonaRosaStore.account.isRegistered && (activeSubscription.isActive || InAppDonations.hasAtLeastOnePaymentMethodAvailable()))
        }
      },
      onError = {}
    )
  }

  override fun onCleared() {
    disposables.clear()
  }

  fun refreshDeprecatedOrUnregistered() {
    store.update {
      it.copy(
        clientDeprecated = ZonaRosaStore.misc.isClientDeprecated,
        userUnregistered = ZonaRosaPreferences.isUnauthorizedReceived(AppDependencies.application) || !ZonaRosaStore.account.isRegistered
      )
    }
  }

  fun refresh() {
    store.update {
      it.copy(
        hasExpiredGiftBadge = ZonaRosaStore.inAppPayments.getExpiredGiftBadge() != null,
        backupFailureState = getBackupFailureState()
      )
    }
  }

  private fun getBackupFailureState(): BackupFailureState {
    return when {
      !ZonaRosaStore.account.isRegistered || !ZonaRosaStore.backup.areBackupsEnabled -> BackupFailureState.NONE
      ZonaRosaStore.backup.isNotEnoughRemoteStorageSpace -> BackupFailureState.OUT_OF_STORAGE_SPACE
      ZonaRosaStore.backup.hasBackupCreationError -> BackupFailureState.COULD_NOT_COMPLETE_BACKUP
      ZonaRosaStore.backup.subscriptionStateMismatchDetected -> BackupFailureState.SUBSCRIPTION_STATE_MISMATCH
      ZonaRosaStore.backup.hasBackupAlreadyRedeemedError -> BackupFailureState.ALREADY_REDEEMED
      else -> BackupFailureState.NONE
    }
  }
}

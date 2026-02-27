/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.completed

import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.messenger.backup.v2.ui.BackupAlert
import io.zonarosa.messenger.backup.v2.ui.BackupAlertBottomSheet
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.badges.self.expired.MonthlyDonationCanceledBottomSheetDialogFragment
import io.zonarosa.messenger.components.settings.app.subscription.DonationPendingBottomSheet
import io.zonarosa.messenger.components.settings.app.subscription.DonationPendingBottomSheetArgs
import io.zonarosa.messenger.components.settings.app.subscription.thanks.ThanksForYourSupportBottomSheetDialogFragment
import io.zonarosa.messenger.components.settings.app.subscription.thanks.ThanksForYourSupportBottomSheetDialogFragmentArgs
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.databaseprotos.DonationErrorValue
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

/**
 * Handles displaying bottom sheets for in-app payments. The current policy is to "fire and forget".
 */
class InAppPaymentsBottomSheetDelegate(
  private val fragmentManager: FragmentManager,
  private val lifecycleOwner: LifecycleOwner
) : DefaultLifecycleObserver {

  companion object {

    private val inAppPaymentProcessingErrors = listOf(
      InAppPaymentData.Error.Type.PAYMENT_PROCESSING,
      InAppPaymentData.Error.Type.STRIPE_FAILURE,
      InAppPaymentData.Error.Type.STRIPE_CODED_ERROR,
      InAppPaymentData.Error.Type.STRIPE_DECLINED_ERROR,
      InAppPaymentData.Error.Type.PAYPAL_CODED_ERROR,
      InAppPaymentData.Error.Type.PAYPAL_DECLINED_ERROR
    )
  }

  private val lifecycleDisposable = LifecycleDisposable().apply {
    bindTo(lifecycleOwner)
  }

  private val badgeRepository = TerminalDonationRepository()

  override fun onResume(owner: LifecycleOwner) {
    handleLegacyTerminalDonationSheets()
    handleLegacyVerifiedMonthlyDonationSheets()
    handleInAppPaymentDonationSheets()
    handleInAppPaymentBackupsSheets()
  }

  /**
   * Handles terminal donations consumed from the InAppPayments values. These are only ever set by the legacy jobs,
   * and will be completely removed close to when the jobs are removed. (We might want an additional 90 days?)
   */
  private fun handleLegacyTerminalDonationSheets() {
    val donations = ZonaRosaStore.inAppPayments.consumeTerminalDonations()
    for (donation in donations) {
      if (donation.isLongRunningPaymentMethod && (donation.error == null || donation.error.type != DonationErrorValue.Type.REDEMPTION)) {
        TerminalDonationBottomSheet.show(fragmentManager, donation)
      } else if (donation.error != null) {
        lifecycleDisposable += badgeRepository.getBadge(donation).observeOn(AndroidSchedulers.mainThread()).subscribe { badge ->
          ThanksForYourSupportBottomSheetDialogFragment.create(badge).show(fragmentManager, ThanksForYourSupportBottomSheetDialogFragment.SHEET_TAG)
        }
      }
    }
  }

  /**
   * Handles the 'verified' sheet that appears after a user externally verifies a payment and returns to the application.
   * These are only ever set by the legacy jobs, and will be completely removed close to when the jobs are removed. (We might
   * want an additional 90 days?)
   */
  private fun handleLegacyVerifiedMonthlyDonationSheets() {
    ZonaRosaStore.inAppPayments.consumeVerifiedSubscription3DSData()?.also {
      DonationPendingBottomSheet().apply {
        arguments = DonationPendingBottomSheetArgs.Builder(it.inAppPayment.id).build().toBundle()
      }.show(fragmentManager, null)
    }
  }

  /**
   * Handles the new in-app payment sheets for donations.
   */
  private fun handleInAppPaymentDonationSheets() {
    lifecycleDisposable += Single.fromCallable {
      ZonaRosaDatabase.inAppPayments.consumeDonationPaymentsToNotifyUser()
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribeBy { inAppPayments ->
      for (payment in inAppPayments) {
        if (payment.data.error == null && payment.state == InAppPaymentTable.State.END) {
          ThanksForYourSupportBottomSheetDialogFragment()
            .apply { arguments = ThanksForYourSupportBottomSheetDialogFragmentArgs.Builder(Badges.fromDatabaseBadge(payment.data.badge!!)).build().toBundle() }
            .show(fragmentManager, null)
        } else if (payment.data.error != null && payment.state == InAppPaymentTable.State.PENDING) {
          DonationPendingBottomSheet().apply {
            arguments = DonationPendingBottomSheetArgs.Builder(payment.id).build().toBundle()
          }.show(fragmentManager, null)
        } else if (isUnexpectedCancellation(payment.state, payment.data) && ZonaRosaStore.inAppPayments.showMonthlyDonationCanceledDialog) {
          MonthlyDonationCanceledBottomSheetDialogFragment.show(fragmentManager)
        }
      }
    }
  }

  /**
   * Handles the new in-app payment sheets for backups.
   */
  private fun handleInAppPaymentBackupsSheets() {
    lifecycleDisposable += Single.fromCallable {
      ZonaRosaDatabase.inAppPayments.consumeBackupPaymentsToNotifyUser()
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribeBy { inAppPayments ->
      for (payment in inAppPayments) {
        if (isPaymentProcessingError(payment.state, payment.data)) {
          BackupAlertBottomSheet.create(BackupAlert.FailedToRenew).show(fragmentManager, null)
        } else if (isUnexpectedCancellation(payment.state, payment.data)) {
          BackupAlertBottomSheet.create(BackupAlert.MediaBackupsAreOff(payment.endOfPeriodSeconds)).show(fragmentManager, null)
        }
      }
    }
  }

  private fun isUnexpectedCancellation(inAppPaymentState: InAppPaymentTable.State, inAppPaymentData: InAppPaymentData): Boolean {
    return inAppPaymentState == InAppPaymentTable.State.END && inAppPaymentData.error != null && inAppPaymentData.cancellation != null && inAppPaymentData.cancellation.reason != InAppPaymentData.Cancellation.Reason.MANUAL
  }

  private fun isPaymentProcessingError(inAppPaymentState: InAppPaymentTable.State, inAppPaymentData: InAppPaymentData): Boolean {
    return inAppPaymentState == InAppPaymentTable.State.END && inAppPaymentData.error != null && (inAppPaymentData.error.type in inAppPaymentProcessingErrors)
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.billing.upgrade

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.common.GoogleApiAvailability
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlowable
import io.zonarosa.core.ui.compose.ComposeBottomSheetDialogFragment
import io.zonarosa.core.ui.compose.Dialogs
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.messenger.R
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsFlowViewModel
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsStage
import io.zonarosa.messenger.backup.v2.ui.subscription.MessageBackupsType
import io.zonarosa.messenger.components.settings.app.subscription.donate.InAppPaymentCheckoutDelegate
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.util.viewModel

/**
 * BottomSheet that encapsulates the common logic for updating someone to paid tier.
 */
abstract class UpgradeToPaidTierBottomSheet : ComposeBottomSheetDialogFragment(), InAppPaymentCheckoutDelegate.ErrorHandlerCallback {

  companion object {
    const val RESULT_KEY = "UpgradeToPaidTierBottomSheet.RESULT_KEY"

    fun addResultListener(fragment: Fragment, onUpgradedToPaidTier: () -> Unit) {
      fragment.setFragmentResultListener(RESULT_KEY) { key, bundle ->
        if (RESULT_KEY == key) {
          val didCompleteSignup = bundle.getBoolean(RESULT_KEY, false)

          if (didCompleteSignup) {
            onUpgradedToPaidTier()
          }
        }
      }
    }
  }

  private val viewModel: MessageBackupsFlowViewModel by viewModel {
    MessageBackupsFlowViewModel(
      initialTierSelection = MessageBackupTier.PAID,
      googlePlayApiAvailability = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext()),
      startScreen = MessageBackupsStage.TYPE_SELECTION
    )
  }

  private val errorHandler = InAppPaymentCheckoutDelegate.ErrorHandler()

  override val peekHeightPercentage: Float = 1f

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    errorHandler.attach(
      fragment = this,
      errorHandlerCallback = this,
      inAppPaymentIdSource = viewModel.stateFlow.asFlowable()
        .filter { it.inAppPayment != null }
        .map { it.inAppPayment!!.id }
    )

    viewLifecycleOwner.lifecycleScope.launch(ZonaRosaDispatchers.Main) {
      repeatOnLifecycle(Lifecycle.State.RESUMED) {
        viewModel.deletionState.collectLatest {
          if (it.isInProgress()) {
            Toast.makeText(
              requireContext(),
              R.string.MessageBackupsFlowFragment__a_backup_deletion_is_in_progress,
              Toast.LENGTH_SHORT
            ).show()

            dismissAllowingStateLoss()
          }
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.refreshCurrentTier()
    viewModel.setGooglePlayApiAvailability(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext()))
  }

  @Composable
  override fun SheetContent() {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    val paidBackupType = state.allBackupTypes.firstOrNull { it.tier == MessageBackupTier.PAID } as? MessageBackupsType.Paid
    val freeBackupType = state.allBackupTypes.firstOrNull { it.tier == MessageBackupTier.FREE } as? MessageBackupsType.Free

    if (paidBackupType != null && freeBackupType != null) {
      UpgradeSheetContent(
        paidBackupType = paidBackupType,
        freeBackupType = freeBackupType,
        isSubscribeEnabled = state.stage == MessageBackupsStage.TYPE_SELECTION,
        onSubscribeClick = viewModel::goToNextStage
      )
    }

    when (state.stage) {
      MessageBackupsStage.CREATING_IN_APP_PAYMENT -> Dialogs.IndeterminateProgressDialog()
      MessageBackupsStage.PROCESS_PAYMENT -> Dialogs.IndeterminateProgressDialog()
      MessageBackupsStage.PROCESS_FREE -> Dialogs.IndeterminateProgressDialog()
      else -> Unit
    }

    LaunchedEffect(state.stage) {
      if (state.stage == MessageBackupsStage.CHECKOUT_SHEET) {
        AppDependencies.billingApi.launchBillingFlow(requireActivity())
      }

      if (state.stage == MessageBackupsStage.COMPLETED) {
        dismissAllowingStateLoss()
        setFragmentResult(RESULT_KEY, bundleOf(RESULT_KEY to true))
      }
    }
  }

  /**
   * This is responsible for displaying the normal upgrade sheet content.
   */
  @Composable
  abstract fun UpgradeSheetContent(
    paidBackupType: MessageBackupsType.Paid,
    freeBackupType: MessageBackupsType.Free,
    isSubscribeEnabled: Boolean,
    onSubscribeClick: () -> Unit
  )

  override fun onUserLaunchedAnExternalApplication() = error("Unsupported.")

  override fun navigateToDonationPending(inAppPayment: InAppPaymentTable.InAppPayment) = error("Unsupported.")

  override fun exitCheckoutFlow() {
    dismissAllowingStateLoss()
  }
}

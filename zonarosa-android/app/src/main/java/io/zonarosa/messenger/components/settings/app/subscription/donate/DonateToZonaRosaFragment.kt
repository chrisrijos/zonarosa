package io.zonarosa.messenger.components.settings.app.subscription.donate

import android.text.SpannableStringBuilder
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.core.util.dp
import io.zonarosa.core.util.getParcelableCompat
import io.zonarosa.core.util.getSerializableCompat
import io.zonarosa.core.util.logging.Log
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.messenger.R
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.badges.models.BadgePreview
import io.zonarosa.messenger.components.KeyboardAwareLinearLayout
import io.zonarosa.messenger.components.ViewBinderDelegate
import io.zonarosa.messenger.components.WrapperDialogFragment
import io.zonarosa.messenger.components.settings.DSLConfiguration
import io.zonarosa.messenger.components.settings.DSLSettingsFragment
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.app.subscription.boost.Boost
import io.zonarosa.messenger.components.settings.app.subscription.donate.gateway.GatewaySelectorBottomSheet
import io.zonarosa.messenger.components.settings.app.subscription.models.CurrencySelection
import io.zonarosa.messenger.components.settings.app.subscription.models.NetworkFailure
import io.zonarosa.messenger.components.settings.app.subscription.thanks.ThanksForYourSupportBottomSheetDialogFragment
import io.zonarosa.messenger.components.settings.configure
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.databinding.DonateToZonaRosaFragmentBinding
import io.zonarosa.messenger.payments.FiatMoneyUtil
import io.zonarosa.messenger.payments.currency.CurrencyUtil
import io.zonarosa.messenger.subscription.Subscription
import io.zonarosa.messenger.util.Material3OnScrollHelper
import io.zonarosa.messenger.util.Projection
import io.zonarosa.messenger.util.SpanUtil
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter
import io.zonarosa.messenger.util.navigation.safeNavigate
import io.zonarosa.service.api.subscriptions.ActiveSubscription
import java.math.BigDecimal
import java.util.Currency
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Unified donation fragment which allows users to choose between monthly or one-time donations.
 */
class DonateToZonaRosaFragment :
  DSLSettingsFragment(
    layoutId = R.layout.donate_to_zonarosa_fragment
  ),
  ThanksForYourSupportBottomSheetDialogFragment.Callback,
  InAppPaymentCheckoutDelegate.Callback {

  companion object {
    private val TAG = Log.tag(DonateToZonaRosaFragment::class.java)
  }

  class Dialog : WrapperDialogFragment() {

    override fun getWrappedFragment(): Fragment {
      return CheckoutNavHostFragment.create(
        requireArguments().getSerializableCompat(ARG, InAppPaymentType::class.java)!!
      )
    }

    companion object {

      private const val ARG = "in_app_payment_type"

      @JvmStatic
      fun create(inAppPaymentType: InAppPaymentType): DialogFragment {
        return Dialog().apply {
          arguments = bundleOf(ARG to inAppPaymentType)
        }
      }
    }
  }

  private val args: DonateToZonaRosaFragmentArgs by navArgs()
  private val viewModel: DonateToZonaRosaViewModel by viewModels(factoryProducer = {
    DonateToZonaRosaViewModel.Factory(args.startType)
  })

  private val disposables = LifecycleDisposable()
  private val binding by ViewBinderDelegate(DonateToZonaRosaFragmentBinding::bind)

  private val supportTechSummary: CharSequence by lazy {
    SpannableStringBuilder(SpanUtil.color(ContextCompat.getColor(requireContext(), CoreUiR.color.zonarosa_colorOnSurfaceVariant), requireContext().getString(R.string.DonateToZonaRosaFragment__private_messaging)))
      .append("\n")
      .append(
        SpanUtil.readMore(requireContext(), ContextCompat.getColor(requireContext(), CoreUiR.color.zonarosa_colorPrimary)) {
          findNavController().safeNavigate(DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToSubscribeLearnMoreBottomSheetDialog())
        }
      )
  }

  override fun onToolbarNavigationClicked() {
    requireActivity().onBackPressedDispatcher.onBackPressed()
  }

  override fun getMaterial3OnScrollHelper(toolbar: Toolbar?): Material3OnScrollHelper {
    return object : Material3OnScrollHelper(activity = requireActivity(), views = listOf(toolbar!!), lifecycleOwner = viewLifecycleOwner) {
      override val activeColorSet: ColorSet = ColorSet(R.color.transparent, CoreUiR.color.zonarosa_colorBackground)
      override val inactiveColorSet: ColorSet = ColorSet(R.color.transparent, CoreUiR.color.zonarosa_colorBackground)
    }
  }

  override fun bindAdapter(adapter: MappingAdapter) {
    val checkoutDelegate = InAppPaymentCheckoutDelegate(this, this, viewModel.inAppPaymentId)

    val recyclerView = this.recyclerView!!
    recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS

    KeyboardAwareLinearLayout(requireContext()).apply {
      addOnKeyboardHiddenListener {
        recyclerView.post { recyclerView.requestLayout() }
      }

      addOnKeyboardShownListener {
        recyclerView.post { recyclerView.scrollToPosition(adapter.itemCount - 1) }
      }

      (view as ViewGroup).addView(this)
    }

    Boost.register(adapter)
    Subscription.register(adapter)
    NetworkFailure.register(adapter)
    BadgePreview.register(adapter)
    CurrencySelection.register(adapter)
    DonationPillToggle.register(adapter)

    setFragmentResultListener(GatewaySelectorBottomSheet.REQUEST_KEY) { _, bundle ->
      if (bundle.containsKey(GatewaySelectorBottomSheet.FAILURE_KEY)) {
        showSepaEuroMaximumDialog(FiatMoney(bundle.getSerializable(GatewaySelectorBottomSheet.SEPA_EURO_MAX) as BigDecimal, CurrencyUtil.EURO))
      } else {
        val inAppPayment: InAppPaymentTable.InAppPayment = bundle.getParcelableCompat(GatewaySelectorBottomSheet.REQUEST_KEY, InAppPaymentTable.InAppPayment::class.java)!!
        checkoutDelegate.handleGatewaySelectionResponse(inAppPayment)
      }
    }

    disposables.bindTo(viewLifecycleOwner)
    disposables += viewModel.actions.subscribe { action ->
      when (action) {
        is DonateToZonaRosaAction.DisplayCurrencySelectionDialog -> {
          val navAction = DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToSetCurrencyFragment(
            action.inAppPaymentType,
            action.supportedCurrencies.toTypedArray()
          )

          findNavController().safeNavigate(navAction)
        }

        is DonateToZonaRosaAction.DisplayGatewaySelectorDialog -> {
          Log.d(TAG, "Presenting gateway selector for ${action.inAppPayment.id}")
          val navAction = DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToGatewaySelectorBottomSheetDialog(action.inAppPayment.id)

          findNavController().safeNavigate(navAction)
        }

        is DonateToZonaRosaAction.CancelSubscription -> {
          val navAction = DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToStripePaymentInProgressFragment(
            InAppPaymentProcessorAction.CANCEL_SUBSCRIPTION,
            null
          )

          findNavController().safeNavigate(navAction)
        }

        is DonateToZonaRosaAction.UpdateSubscription -> {
          if (action.inAppPayment.data.paymentMethodType == InAppPaymentData.PaymentMethodType.PAYPAL) {
            val navAction = DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToPaypalPaymentInProgressFragment(
              InAppPaymentProcessorAction.UPDATE_SUBSCRIPTION,
              action.inAppPayment.id
            )

            findNavController().safeNavigate(navAction)
          } else {
            val navAction = DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToStripePaymentInProgressFragment(
              InAppPaymentProcessorAction.UPDATE_SUBSCRIPTION,
              action.inAppPayment.id
            )

            findNavController().safeNavigate(navAction)
          }
        }
      }
    }

    disposables += viewModel.state.subscribe { state ->
      adapter.submitList(getConfiguration(state).toMappingModelList())
    }
  }

  override fun onStop() {
    super.onStop()

    listOf(
      binding.boost1Animation,
      binding.boost2Animation,
      binding.boost3Animation,
      binding.boost4Animation,
      binding.boost5Animation,
      binding.boost6Animation
    ).forEach {
      it.cancelAnimation()
    }
  }

  private fun getConfiguration(state: DonateToZonaRosaState): DSLConfiguration {
    return configure {
      space(36.dp)

      customPref(BadgePreview.BadgeModel.SubscriptionModel(state.badge))

      space(12.dp)

      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.DonateToZonaRosaFragment__privacy_over_profit,
          DSLSettingsText.CenterModifier,
          DSLSettingsText.TitleLargeModifier
        )
      )

      space(8.dp)

      noPadTextPref(
        title = DSLSettingsText.from(supportTechSummary, DSLSettingsText.CenterModifier)
      )

      space(24.dp)

      customPref(
        CurrencySelection.Model(
          selectedCurrency = state.selectedCurrency,
          isEnabled = state.canSetCurrency,
          onClick = {
            viewModel.requestChangeCurrency()
          }
        )
      )

      space(16.dp)

      customPref(
        DonationPillToggle.Model(
          selected = state.inAppPaymentType,
          onClick = {
            viewModel.toggleDonationType()
          }
        )
      )

      space(10.dp)

      when (state.inAppPaymentType) {
        InAppPaymentType.ONE_TIME_DONATION -> displayOneTimeSelection(state.areFieldsEnabled, state.oneTimeDonationState)
        InAppPaymentType.RECURRING_DONATION -> displayMonthlySelection(state.areFieldsEnabled, state.monthlyDonationState)
        else -> error("This fragment does not support ${state.inAppPaymentType}.")
      }

      space(20.dp)

      if (state.inAppPaymentType == InAppPaymentType.RECURRING_DONATION && (state.monthlyDonationState.isSubscriptionActive || state.monthlyDonationState.isSubscriptionInProgress)) {
        primaryButton(
          text = DSLSettingsText.from(R.string.SubscribeFragment__update_subscription),
          isEnabled = state.canUpdate,
          onClick = {
            if (state.monthlyDonationState.transactionState.isTransactionJobPending) {
              showDonationPendingDialog(state)
            } else {
              MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.SubscribeFragment__update_subscription_question)
                .setMessage(
                  getString(
                    R.string.SubscribeFragment__you_will_be_charged_the_full_amount_s_of,
                    FiatMoneyUtil.format(
                      requireContext().resources,
                      viewModel.getSelectedSubscriptionCost(),
                      FiatMoneyUtil.formatOptions().trimZerosAfterDecimal()
                    )
                  )
                )
                .setPositiveButton(R.string.SubscribeFragment__update) { _, _ ->
                  viewModel.updateSubscription()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> }
                .show()
            }
          }
        )

        space(4.dp)

        secondaryButtonNoOutline(
          text = DSLSettingsText.from(R.string.SubscribeFragment__cancel_subscription),
          isEnabled = state.areFieldsEnabled,
          onClick = {
            if (state.monthlyDonationState.transactionState.isTransactionJobPending && !state.monthlyDonationState.transactionState.isKeepAlive) {
              showDonationPendingDialog(state)
            } else {
              MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.SubscribeFragment__confirm_cancellation)
                .setMessage(R.string.SubscribeFragment__you_wont_be_charged_again)
                .setPositiveButton(R.string.SubscribeFragment__confirm) { _, _ ->
                  viewModel.cancelSubscription()
                }
                .setNegativeButton(R.string.SubscribeFragment__not_now) { _, _ -> }
                .show()
            }
          }
        )
      } else {
        primaryButton(
          text = DSLSettingsText.from(R.string.DonateToZonaRosaFragment__continue),
          isEnabled = state.continueEnabled,
          onClick = {
            if (state.canContinue) {
              requireView().clearFocus()
              viewModel.requestSelectGateway()
            } else {
              showDonationPendingDialog(state)
            }
          }
        )
      }

      space(24.dp)
    }
  }

  private fun showDonationPendingDialog(state: DonateToZonaRosaState) {
    val message = if (state.inAppPaymentType == InAppPaymentType.ONE_TIME_DONATION) {
      if (state.oneTimeDonationState.isOneTimeDonationLongRunning) {
        R.string.DonateToZonaRosaFragment__bank_transfers_usually_take_1_business_day_to_process_onetime
      } else if (state.oneTimeDonationState.isNonVerifiedIdeal) {
        R.string.DonateToZonaRosaFragment__your_ideal_payment_is_still_processing
      } else {
        R.string.DonateToZonaRosaFragment__your_payment_is_still_being_processed_onetime
      }
    } else {
      if (state.monthlyDonationState.activeSubscription?.paymentMethod == ActiveSubscription.PaymentMethod.SEPA_DEBIT) {
        R.string.DonateToZonaRosaFragment__bank_transfers_usually_take_1_business_day_to_process_monthly
      } else if (state.monthlyDonationState.nonVerifiedMonthlyDonation != null) {
        R.string.DonateToZonaRosaFragment__your_ideal_payment_is_still_processing
      } else {
        R.string.DonateToZonaRosaFragment__your_payment_is_still_being_processed_monthly
      }
    }

    MaterialAlertDialogBuilder(requireContext())
      .setTitle(R.string.DonateToZonaRosaFragment__you_have_a_donation_pending)
      .setMessage(message)
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  private fun DSLConfiguration.displayOneTimeSelection(areFieldsEnabled: Boolean, state: DonateToZonaRosaState.OneTimeDonationState) {
    when (state.donationStage) {
      DonateToZonaRosaState.DonationStage.INIT -> customPref(Boost.LoadingModel())
      DonateToZonaRosaState.DonationStage.FAILURE -> customPref(NetworkFailure.Model { viewModel.retryOneTimeDonationState() })
      DonateToZonaRosaState.DonationStage.READY -> {
        customPref(
          Boost.SelectionModel(
            boosts = state.boosts,
            selectedBoost = state.selectedBoost,
            currency = state.customAmount.currency,
            isCustomAmountFocused = state.isCustomAmountFocused,
            isCustomAmountTooSmall = state.shouldDisplayCustomAmountTooSmallError,
            minimumAmount = state.minimumDonationAmountOfSelectedCurrency,
            isEnabled = areFieldsEnabled,
            onBoostClick = { view, boost ->
              startAnimationAboveSelectedBoost(view)
              viewModel.setSelectedBoost(boost)
            },
            onCustomAmountChanged = {
              viewModel.setCustomAmount(it)
            },
            onCustomAmountFocusChanged = {
              if (it) {
                viewModel.setCustomAmountFocused()
              }
            }
          )
        )
      }
    }
  }

  private fun DSLConfiguration.displayMonthlySelection(areFieldsEnabled: Boolean, state: DonateToZonaRosaState.MonthlyDonationState) {
    when (state.donationStage) {
      DonateToZonaRosaState.DonationStage.INIT -> customPref(Subscription.LoaderModel())
      DonateToZonaRosaState.DonationStage.FAILURE -> customPref(NetworkFailure.Model { viewModel.retryMonthlyDonationState() })
      else -> {
        state.subscriptions.forEach { subscription ->

          val isActive = state.activeLevel == subscription.level && state.isSubscriptionActive

          val activePrice = state.activeSubscription?.let { sub ->
            FiatMoney.fromZonaRosaNetworkAmount(sub.amount, Currency.getInstance(sub.currency))
          }

          customPref(
            Subscription.Model(
              activePrice = if (isActive) activePrice else null,
              subscription = subscription,
              isSelected = state.selectedSubscription == subscription,
              isEnabled = areFieldsEnabled,
              isActive = isActive,
              willRenew = isActive && !state.isActiveSubscriptionEnding,
              onClick = { viewModel.setSelectedSubscription(it) },
              renewalTimestamp = state.renewalTimestamp,
              selectedCurrency = state.selectedCurrency
            )
          )
        }
      }
    }
  }

  private fun startAnimationAboveSelectedBoost(view: View) {
    val animationView = getAnimationContainer(view)
    val viewProjection = Projection.relativeToViewRoot(view, null)
    val animationProjection = Projection.relativeToViewRoot(animationView, null)
    val viewHorizontalCenter = viewProjection.x + viewProjection.width / 2f
    val animationHorizontalCenter = animationProjection.x + animationProjection.width / 2f
    val animationBottom = animationProjection.y + animationProjection.height

    animationView.translationY = -(animationBottom - viewProjection.y) + (viewProjection.height / 2f)
    animationView.translationX = viewHorizontalCenter - animationHorizontalCenter

    animationView.playAnimation()

    viewProjection.release()
    animationProjection.release()
  }

  private fun getAnimationContainer(view: View): LottieAnimationView {
    return when (view.id) {
      R.id.boost_1 -> binding.boost1Animation
      R.id.boost_2 -> binding.boost2Animation
      R.id.boost_3 -> binding.boost3Animation
      R.id.boost_4 -> binding.boost4Animation
      R.id.boost_5 -> binding.boost5Animation
      R.id.boost_6 -> binding.boost6Animation
      else -> throw AssertionError()
    }
  }

  private fun showSepaEuroMaximumDialog(sepaEuroMaximum: FiatMoney) {
    val max = FiatMoneyUtil.format(resources, sepaEuroMaximum, FiatMoneyUtil.formatOptions().trimZerosAfterDecimal())
    MaterialAlertDialogBuilder(requireContext())
      .setTitle(R.string.DonateToZonaRosa__donation_amount_too_high)
      .setMessage(getString(R.string.DonateToZonaRosaFragment__you_can_send_up_to_s_via_bank_transfer, max))
      .setPositiveButton(android.R.string.ok, null)
      .show()
  }

  override fun onBoostThanksSheetDismissed() {
    requireActivity().onBackPressedDispatcher.onBackPressed()
  }

  override fun navigateToStripePaymentInProgress(inAppPayment: InAppPaymentTable.InAppPayment) {
    findNavController().safeNavigate(
      DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToStripePaymentInProgressFragment(
        InAppPaymentProcessorAction.PROCESS_NEW_IN_APP_PAYMENT,
        inAppPayment.id
      )
    )
  }

  override fun navigateToPayPalPaymentInProgress(inAppPayment: InAppPaymentTable.InAppPayment) {
    findNavController().safeNavigate(
      DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToPaypalPaymentInProgressFragment(
        InAppPaymentProcessorAction.PROCESS_NEW_IN_APP_PAYMENT,
        inAppPayment.id
      )
    )
  }

  override fun navigateToCreditCardForm(inAppPayment: InAppPaymentTable.InAppPayment) {
    findNavController().safeNavigate(DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToCreditCardFragment(inAppPayment.id))
  }

  override fun navigateToIdealDetailsFragment(inAppPayment: InAppPaymentTable.InAppPayment) {
    findNavController().safeNavigate(DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToIdealTransferDetailsFragment(inAppPayment.id))
  }

  override fun navigateToBankTransferMandate(inAppPayment: InAppPaymentTable.InAppPayment) {
    findNavController().safeNavigate(DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToBankTransferMandateFragment(inAppPayment.id))
  }

  override fun onPaymentComplete(inAppPayment: InAppPaymentTable.InAppPayment) {
    findNavController().safeNavigate(DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToThanksForYourSupportBottomSheetDialog(Badges.fromDatabaseBadge(inAppPayment.data.badge!!)))
  }

  override fun onSubscriptionCancelled(inAppPaymentType: InAppPaymentType) {
    viewModel.refreshActiveSubscription()
    Snackbar.make(requireView(), R.string.SubscribeFragment__your_subscription_has_been_cancelled, Snackbar.LENGTH_LONG).show()
  }

  override fun onProcessorActionProcessed() {
    // TODO [alex] - what did this used to do?
  }

  override fun onUserLaunchedAnExternalApplication() {
    // TODO [alex] - what did this used to do?
  }

  override fun navigateToDonationPending(inAppPayment: InAppPaymentTable.InAppPayment) {
    findNavController().safeNavigate(DonateToZonaRosaFragmentDirections.actionDonateToZonaRosaFragmentToDonationPendingBottomSheet(inAppPayment.id))
  }

  override fun exitCheckoutFlow() {
    requireActivity().finish()
  }
}

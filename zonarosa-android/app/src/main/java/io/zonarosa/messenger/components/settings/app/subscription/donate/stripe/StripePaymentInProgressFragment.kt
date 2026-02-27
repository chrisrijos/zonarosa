package io.zonarosa.messenger.components.settings.app.subscription.donate.stripe

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.core.util.getParcelableCompat
import io.zonarosa.core.util.logging.Log
import io.zonarosa.donations.StripeApi
import io.zonarosa.donations.StripeIntentAccessor
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.ViewBinderDelegate
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.requireSubscriberType
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository.toErrorSource
import io.zonarosa.messenger.components.settings.app.subscription.donate.InAppPaymentProcessorAction
import io.zonarosa.messenger.components.settings.app.subscription.donate.InAppPaymentProcessorActionResult
import io.zonarosa.messenger.components.settings.app.subscription.donate.InAppPaymentProcessorStage
import io.zonarosa.messenger.components.settings.app.subscription.errors.DonationError
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.databaseprotos.InAppPaymentData
import io.zonarosa.messenger.databinding.DonationInProgressFragmentBinding
import io.zonarosa.messenger.util.navigation.safeNavigate

class StripePaymentInProgressFragment : DialogFragment(R.layout.donation_in_progress_fragment) {

  companion object {
    private val TAG = Log.tag(StripePaymentInProgressFragment::class.java)

    const val REQUEST_KEY = "REQUEST_KEY"
  }

  private val binding by ViewBinderDelegate(DonationInProgressFragmentBinding::bind)
  private val args: StripePaymentInProgressFragmentArgs by navArgs()
  private val disposables = LifecycleDisposable()

  private val viewModel: StripePaymentInProgressViewModel by navGraphViewModels(
    R.id.checkout_flow
  )

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    isCancelable = false
    return super.onCreateDialog(savedInstanceState).apply {
      window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    if (savedInstanceState == null) {
      viewModel.onBeginNewAction()
      when (args.action) {
        InAppPaymentProcessorAction.PROCESS_NEW_IN_APP_PAYMENT -> {
          viewModel.processNewDonation(args.inAppPaymentId!!, this::handleRequiredAction, this::handleRequiredAction)
        }

        InAppPaymentProcessorAction.UPDATE_SUBSCRIPTION -> {
          viewModel.updateSubscription(args.inAppPaymentId!!)
        }

        InAppPaymentProcessorAction.CANCEL_SUBSCRIPTION -> {
          viewModel.cancelSubscription(InAppPaymentSubscriberRecord.Type.DONATION)
        }
      }
    }

    disposables.bindTo(viewLifecycleOwner)
    disposables += viewModel.state.subscribeBy { stage ->
      presentUiState(stage)
    }
  }

  private fun presentUiState(stage: InAppPaymentProcessorStage) {
    when (stage) {
      InAppPaymentProcessorStage.INIT -> binding.progressCardStatus.text = getProcessingStatus()
      InAppPaymentProcessorStage.PAYMENT_PIPELINE -> binding.progressCardStatus.text = getProcessingStatus()
      InAppPaymentProcessorStage.FAILED -> {
        viewModel.onEndAction()
        findNavController().popBackStack()
        setFragmentResult(
          REQUEST_KEY,
          bundleOf(
            REQUEST_KEY to InAppPaymentProcessorActionResult(
              action = args.action,
              inAppPaymentId = args.inAppPaymentId,
              status = InAppPaymentProcessorActionResult.Status.FAILURE
            )
          )
        )
      }

      InAppPaymentProcessorStage.COMPLETE -> {
        viewModel.onEndAction()
        findNavController().popBackStack()
        setFragmentResult(
          REQUEST_KEY,
          bundleOf(
            REQUEST_KEY to InAppPaymentProcessorActionResult(
              action = args.action,
              inAppPaymentId = args.inAppPaymentId,
              status = InAppPaymentProcessorActionResult.Status.SUCCESS
            )
          )
        )
      }

      InAppPaymentProcessorStage.CANCELLING -> binding.progressCardStatus.setText(R.string.StripePaymentInProgressFragment__cancelling)
    }
  }

  private fun getProcessingStatus(): String {
    return getString(R.string.InAppPaymentInProgressFragment__processing_donation)
  }

  private fun handleRequiredAction(inAppPaymentId: InAppPaymentTable.InAppPaymentId): Completable {
    return Single.fromCallable {
      ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)!!
    }.map { inAppPayment ->
      val requiresAction: InAppPaymentData.StripeRequiresActionState = inAppPayment.data.stripeRequiresAction ?: error("REQUIRES_ACTION without action data")
      inAppPayment to StripeApi.Secure3DSAction.from(
        uri = Uri.parse(requiresAction.uri),
        returnUri = Uri.parse(requiresAction.returnUri),
        stripeIntentAccessor = StripeIntentAccessor(
          objectType = if (inAppPayment.type.recurring) {
            StripeIntentAccessor.ObjectType.SETUP_INTENT
          } else {
            StripeIntentAccessor.ObjectType.PAYMENT_INTENT
          },
          intentId = requiresAction.stripeIntentId,
          intentClientSecret = requiresAction.stripeClientSecret
        )
      )
    }.flatMap { (originalPayment, secure3dsAction) ->
      when (secure3dsAction) {
        is StripeApi.Secure3DSAction.NotNeeded -> {
          Log.d(TAG, "No 3DS action required.")
          Single.just(StripeIntentAccessor.NO_ACTION_REQUIRED)
        }

        is StripeApi.Secure3DSAction.ConfirmRequired -> {
          Log.d(TAG, "3DS action required. Displaying dialog...")

          val waitingForAuthPayment = originalPayment.copy(
            subscriberId = if (originalPayment.type.recurring) {
              InAppPaymentsRepository.requireSubscriber(originalPayment.type.requireSubscriberType()).subscriberId
            } else {
              null
            },
            state = InAppPaymentTable.State.WAITING_FOR_AUTHORIZATION,
            data = originalPayment.data.newBuilder().waitForAuth(
              waitForAuth = InAppPaymentData.WaitingForAuthorizationState(
                stripeIntentId = secure3dsAction.stripeIntentAccessor.intentId,
                stripeClientSecret = secure3dsAction.stripeIntentAccessor.intentClientSecret
              )
            ).build()
          )

          Single.create { emitter ->
            val listener = FragmentResultListener { _, bundle ->
              val result: StripeIntentAccessor? = bundle.getParcelableCompat(Stripe3DSDialogFragment.REQUEST_KEY, StripeIntentAccessor::class.java)
              if (result != null) {
                emitter.onSuccess(result)
              } else {
                disposables += viewModel.getInAppPaymentType(args.inAppPaymentId!!).subscribeBy { inAppPaymentType ->
                  val didLaunchExternal = bundle.getBoolean(Stripe3DSDialogFragment.LAUNCHED_EXTERNAL, false)
                  if (didLaunchExternal) {
                    emitter.onError(DonationError.UserLaunchedExternalApplication(inAppPaymentType.toErrorSource()))
                  } else {
                    emitter.onError(DonationError.UserCancelledPaymentError(inAppPaymentType.toErrorSource()))
                  }
                }
              }
            }

            parentFragmentManager.setFragmentResultListener(Stripe3DSDialogFragment.REQUEST_KEY, this, listener)

            findNavController().safeNavigate(StripePaymentInProgressFragmentDirections.actionStripePaymentInProgressFragmentToStripe3dsDialogFragment(secure3dsAction.uri, secure3dsAction.returnUri, waitingForAuthPayment))

            emitter.setCancellable {
              parentFragmentManager.clearFragmentResultListener(Stripe3DSDialogFragment.REQUEST_KEY)
            }
          }.subscribeOn(AndroidSchedulers.mainThread()).observeOn(Schedulers.io())
        }
      }
    }.flatMapCompletable { stripeIntentAccessor ->
      Completable.fromAction {
        Log.d(TAG, "User confirmed action. Updating intent accessors and resubmitting job.")
        val postNextActionPayment = ZonaRosaDatabase.inAppPayments.getById(inAppPaymentId)!!

        ZonaRosaDatabase.inAppPayments.update(
          postNextActionPayment.copy(
            state = InAppPaymentTable.State.REQUIRED_ACTION_COMPLETED,
            data = postNextActionPayment.data.newBuilder().stripeActionComplete(
              stripeActionComplete = InAppPaymentData.StripeActionCompleteState(
                stripeIntentId = stripeIntentAccessor.intentId,
                stripeClientSecret = stripeIntentAccessor.intentClientSecret
              )
            ).build()
          )
        )
      }
    }
  }
}

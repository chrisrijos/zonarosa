/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.jobs

import io.zonarosa.donations.InAppPaymentType
import io.zonarosa.donations.PaymentSource
import io.zonarosa.donations.StripeApi
import io.zonarosa.donations.StripeIntentAccessor
import io.zonarosa.messenger.components.settings.app.subscription.DonationSerializationHelper.toFiatMoney
import io.zonarosa.messenger.components.settings.app.subscription.OneTimeInAppPaymentRepository
import io.zonarosa.messenger.components.settings.app.subscription.StripeRepository
import io.zonarosa.messenger.components.settings.app.subscription.errors.DonationError
import io.zonarosa.messenger.components.settings.app.subscription.errors.DonationErrorSource
import io.zonarosa.messenger.components.settings.app.subscription.toPaymentSource
import io.zonarosa.messenger.database.InAppPaymentTable
import io.zonarosa.messenger.jobmanager.Job
import io.zonarosa.messenger.jobs.protos.InAppPaymentSetupJobData
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

/**
 *  Handles one-time Stripe transactions.
 */
class InAppPaymentStripeOneTimeSetupJob private constructor(
  data: InAppPaymentSetupJobData,
  parameters: Parameters
) : InAppPaymentSetupJob(data, parameters) {

  companion object {
    const val KEY = "InAppPaymentStripeOneTimeSetupJob"

    /**
     * Creates a new job for performing stripe recurring payment setup. Note that
     * we do not require network for this job, as if the network is not present, we
     * should treat that as an immediate error and fail the job.
     */
    fun create(
      inAppPayment: InAppPaymentTable.InAppPayment,
      paymentSource: PaymentSource
    ): InAppPaymentStripeOneTimeSetupJob {
      return InAppPaymentStripeOneTimeSetupJob(
        getJobData(inAppPayment, paymentSource),
        getParameters(inAppPayment)
      )
    }
  }

  override fun performPreUserAction(inAppPayment: InAppPaymentTable.InAppPayment): RequiredUserAction {
    info("Beginning one-time payment pipeline.")
    val amount = inAppPayment.data.amount!!.toFiatMoney()
    val recipientId = inAppPayment.data.recipientId?.let { RecipientId.from(it) } ?: Recipient.self().id
    if (inAppPayment.type == InAppPaymentType.ONE_TIME_GIFT) {
      info("Verifying recipient $recipientId can receive gift.")
      OneTimeInAppPaymentRepository.verifyRecipientIsAllowedToReceiveAGiftSync(recipientId)
    }

    info("Continuing payment...")
    val intentAccessor = StripeRepository.createPaymentIntent(amount, recipientId, inAppPayment.data.level, data.inAppPaymentSource!!.toPaymentSource().type)

    info("Confirming payment...")
    return when (val action = StripeRepository.confirmPaymentIntent(data.inAppPaymentSource.toPaymentSource(), intentAccessor, recipientId)) {
      is StripeApi.Secure3DSAction.ConfirmRequired -> RequiredUserAction.StripeActionRequired(action)
      is StripeApi.Secure3DSAction.NotNeeded -> RequiredUserAction.StripeActionNotRequired(action)
    }
  }

  override fun performPostUserAction(inAppPayment: InAppPaymentTable.InAppPayment): Result {
    val paymentMethodId = inAppPayment.data.stripeActionComplete!!.paymentMethodId
    val intentAccessor = StripeIntentAccessor(
      objectType = StripeIntentAccessor.ObjectType.PAYMENT_INTENT,
      intentId = inAppPayment.data.stripeActionComplete.stripeIntentId,
      intentClientSecret = inAppPayment.data.stripeActionComplete.stripeClientSecret
    )

    info("Getting status and payment method id from stripe.")
    val data = StripeRepository.getStatusAndPaymentMethodId(intentAccessor, paymentMethodId)

    if (!data.status.canProceed()) {
      warning("Cannot proceed with status ${data.status}.")
      handleFailure(inAppPayment.id, DonationError.UserCancelledPaymentError(DonationErrorSource.ONE_TIME))
      return Result.failure()
    }

    info("Received status and payment method id. Submitting redemption job chain.")
    OneTimeInAppPaymentRepository.submitRedemptionJobChain(inAppPayment, intentAccessor.intentId)

    return Result.success()
  }

  override fun getFactoryKey(): String = KEY

  override fun run(): Result {
    return performTransaction()
  }

  class Factory : Job.Factory<InAppPaymentStripeOneTimeSetupJob> {
    override fun create(parameters: Parameters, serializedData: ByteArray?): InAppPaymentStripeOneTimeSetupJob {
      val data = serializedData?.let { InAppPaymentSetupJobData.ADAPTER.decode(it) } ?: error("Missing job data!")

      return InAppPaymentStripeOneTimeSetupJob(data, parameters)
    }
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.subscription.manage

import android.widget.ProgressBar
import android.widget.TextView
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.messenger.R
import io.zonarosa.messenger.badges.BadgeImageView
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.components.settings.app.subscription.DonationSerializationHelper.toFiatMoney
import io.zonarosa.messenger.database.model.databaseprotos.DonationErrorValue
import io.zonarosa.messenger.database.model.databaseprotos.PendingOneTimeDonation
import io.zonarosa.messenger.databinding.MySupportPreferenceBinding
import io.zonarosa.messenger.payments.FiatMoneyUtil
import io.zonarosa.messenger.util.adapter.mapping.BindingFactory
import io.zonarosa.messenger.util.adapter.mapping.BindingViewHolder
import io.zonarosa.messenger.util.adapter.mapping.MappingAdapter
import io.zonarosa.messenger.util.adapter.mapping.MappingModel
import io.zonarosa.messenger.util.visible

/**
 * Holds state information about pending one-time donations.
 */
object OneTimeDonationPreference {

  fun register(adapter: MappingAdapter) {
    adapter.registerFactory(Model::class.java, BindingFactory(::ViewHolder, MySupportPreferenceBinding::inflate))
  }

  class Model(
    val pendingOneTimeDonation: PendingOneTimeDonation,
    val onPendingClick: (FiatMoney) -> Unit,
    val onErrorClick: (DonationErrorValue) -> Unit
  ) : MappingModel<Model> {
    override fun areItemsTheSame(newItem: Model): Boolean = true

    override fun areContentsTheSame(newItem: Model): Boolean {
      return this.pendingOneTimeDonation == newItem.pendingOneTimeDonation
    }
  }

  class ViewHolder(binding: MySupportPreferenceBinding) : BindingViewHolder<Model, MySupportPreferenceBinding>(binding) {

    val badge: BadgeImageView = binding.mySupportBadge
    val title: TextView = binding.mySupportTitle
    val expiry: TextView = binding.mySupportExpiry
    val progress: ProgressBar = binding.mySupportProgress

    override fun bind(model: Model) {
      badge.setBadge(Badges.fromDatabaseBadge(model.pendingOneTimeDonation.badge!!))
      title.text = context.getString(
        R.string.OneTimeDonationPreference__one_time_s,
        FiatMoneyUtil.format(context.resources, model.pendingOneTimeDonation.amount!!.toFiatMoney(), FiatMoneyUtil.formatOptions().trimZerosAfterDecimal())
      )

      if (model.pendingOneTimeDonation.error != null) {
        presentErrorState(model, model.pendingOneTimeDonation.error)
      } else {
        presentPendingState(model)
      }
    }

    private fun presentErrorState(model: Model, error: DonationErrorValue) {
      expiry.text = getErrorSubtitle(error)

      itemView.setOnClickListener { model.onErrorClick(error) }

      progress.visible = false
    }

    private fun presentPendingState(model: Model) {
      expiry.text = getPendingSubtitle(model.pendingOneTimeDonation.paymentMethodType)

      if (model.pendingOneTimeDonation.paymentMethodType == PendingOneTimeDonation.PaymentMethodType.SEPA_DEBIT) {
        itemView.setOnClickListener { model.onPendingClick(model.pendingOneTimeDonation.amount!!.toFiatMoney()) }
      }

      progress.visible = model.pendingOneTimeDonation.paymentMethodType != PendingOneTimeDonation.PaymentMethodType.SEPA_DEBIT
    }

    private fun getErrorSubtitle(error: DonationErrorValue): String {
      return when (error.type) {
        DonationErrorValue.Type.REDEMPTION -> context.getString(R.string.DonationsErrors__couldnt_add_badge)
        else -> context.getString(R.string.DonationsErrors__donation_failed)
      }
    }

    private fun getPendingSubtitle(paymentMethodType: PendingOneTimeDonation.PaymentMethodType): String {
      return when (paymentMethodType) {
        PendingOneTimeDonation.PaymentMethodType.CARD -> context.getString(R.string.OneTimeDonationPreference__donation_processing)
        PendingOneTimeDonation.PaymentMethodType.SEPA_DEBIT -> context.getString(R.string.OneTimeDonationPreference__donation_pending)
        PendingOneTimeDonation.PaymentMethodType.PAYPAL -> context.getString(R.string.OneTimeDonationPreference__donation_processing)
        PendingOneTimeDonation.PaymentMethodType.IDEAL -> context.getString(R.string.OneTimeDonationPreference__donation_pending)
      }
    }
  }
}

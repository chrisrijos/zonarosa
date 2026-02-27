/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.internal

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.core.util.money.FiatMoney
import io.zonarosa.messenger.badges.Badges
import io.zonarosa.messenger.components.settings.app.subscription.DonationSerializationHelper.toFiatValue
import io.zonarosa.messenger.database.model.databaseprotos.PendingOneTimeDonation
import io.zonarosa.messenger.dependencies.AppDependencies
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale

/**
 * Fetches a badge for our pending donation, which requires downloading the donation config.
 */
class InternalPendingOneTimeDonationConfigurationViewModel : ViewModel() {

  val state: MutableState<PendingOneTimeDonation> = mutableStateOf(
    PendingOneTimeDonation(
      timestamp = System.currentTimeMillis(),
      amount = FiatMoney(BigDecimal.valueOf(20), Currency.getInstance("EUR")).toFiatValue()
    )
  )

  val disposable: Disposable = Single
    .fromCallable {
      AppDependencies.donationsService
        .getDonationsConfiguration(Locale.getDefault())
    }
    .flatMap { it.flattenResult() }
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribe { config ->
      val badge = Badges.fromServiceBadge(config.levels.values.first().badge)
      state.value = state.value.copy(badge = Badges.toDatabaseBadge(badge))
    }

  override fun onCleared() {
    super.onCleared()
  }
}

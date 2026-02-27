/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.badges.self.expired

import androidx.annotation.StringRes
import io.zonarosa.messenger.badges.models.Badge

data class MonthlyDonationCanceledState(
  val loadState: LoadState = LoadState.LOADING,
  val badge: Badge? = null,
  @StringRes val errorMessage: Int = -1
) {
  enum class LoadState {
    LOADING,
    READY,
    FAILED
  }
}

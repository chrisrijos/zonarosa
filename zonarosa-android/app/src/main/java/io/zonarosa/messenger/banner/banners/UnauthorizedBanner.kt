/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.banner.banners

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.R
import io.zonarosa.messenger.banner.Banner
import io.zonarosa.messenger.banner.ui.compose.Action
import io.zonarosa.messenger.banner.ui.compose.DefaultBanner
import io.zonarosa.messenger.banner.ui.compose.Importance
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.registration.ui.RegistrationActivity
import io.zonarosa.messenger.util.ZonaRosaPreferences

/**
 * A banner displayed when the client is unauthorized (deregistered).
 */
class UnauthorizedBanner(val context: Context) : Banner<Unit>() {

  override val enabled: Boolean
    get() = ZonaRosaPreferences.isUnauthorizedReceived(context) || !ZonaRosaStore.account.isRegistered

  override val dataFlow: Flow<Unit>
    get() = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) {
    Banner(contentPadding)
  }
}

@Composable
private fun Banner(contentPadding: PaddingValues) {
  val context = LocalContext.current

  DefaultBanner(
    title = null,
    body = stringResource(id = R.string.UnauthorizedReminder_this_is_likely_because_you_registered_your_phone_number_with_ZonaRosa_on_a_different_device),
    importance = Importance.ERROR,
    actions = listOf(
      Action(R.string.UnauthorizedReminder_reregister_action) {
        val registrationIntent = RegistrationActivity.newIntentForReRegistration(context)
        context.startActivity(registrationIntent)
      }
    ),
    paddingValues = contentPadding
  )
}

@DayNightPreviews
@Composable
private fun BannerPreview() {
  Previews.Preview {
    Banner(PaddingValues(0.dp))
  }
}

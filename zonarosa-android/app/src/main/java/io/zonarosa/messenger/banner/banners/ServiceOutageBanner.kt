/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.banner.banners

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.R
import io.zonarosa.messenger.banner.Banner
import io.zonarosa.messenger.banner.ui.compose.DefaultBanner
import io.zonarosa.messenger.banner.ui.compose.Importance
import io.zonarosa.messenger.util.ZonaRosaPreferences

class ServiceOutageBanner(val context: Context) : Banner<Unit>() {

  override val enabled: Boolean
    get() = ZonaRosaPreferences.getServiceOutage(context)

  override val dataFlow: Flow<Unit> = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) = Banner(contentPadding)
}

@Composable
private fun Banner(contentPadding: PaddingValues) {
  DefaultBanner(
    title = null,
    body = stringResource(id = R.string.reminder_header_service_outage_text),
    importance = Importance.ERROR,
    paddingValues = contentPadding
  )
}

@DayNightPreviews
@Composable
private fun BannerPreview() {
  Previews.Preview {
    Banner(contentPadding = PaddingValues(0.dp))
  }
}

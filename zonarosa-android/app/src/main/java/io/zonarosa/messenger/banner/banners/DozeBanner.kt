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
import io.zonarosa.messenger.banner.ui.compose.Action
import io.zonarosa.messenger.banner.ui.compose.DefaultBanner
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.PowerManagerCompat
import io.zonarosa.messenger.util.ServiceUtil
import io.zonarosa.messenger.util.ZonaRosaPreferences

class DozeBanner(private val context: Context, private val onDismissListener: () -> Unit) : Banner<Unit>() {

  override val enabled: Boolean
    get() = !ZonaRosaStore.account.fcmEnabled && !ZonaRosaPreferences.hasPromptedOptimizeDoze(context) && !ServiceUtil.getPowerManager(context).isIgnoringBatteryOptimizations(context.packageName)

  override val dataFlow: Flow<Unit>
    get() = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) {
    Banner(
      contentPadding = contentPadding,
      onDismissListener = {
        ZonaRosaPreferences.setPromptedOptimizeDoze(context, true)
        onDismissListener.invoke()
      },
      onOkListener = {
        ZonaRosaPreferences.setPromptedOptimizeDoze(context, true)
        PowerManagerCompat.requestIgnoreBatteryOptimizations(context)
      }
    )
  }
}

@Composable
private fun Banner(contentPadding: PaddingValues, onDismissListener: () -> Unit = {}, onOkListener: () -> Unit = {}) {
  DefaultBanner(
    title = stringResource(id = R.string.DozeReminder_optimize_for_missing_play_services),
    body = stringResource(id = R.string.DozeReminder_this_device_does_not_support_play_services_tap_to_disable_system_battery),
    onDismissListener = onDismissListener,
    actions = listOf(
      Action(android.R.string.ok) {
        onOkListener()
      }
    ),
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

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.banner.banners

import android.os.Build
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

class BubbleOptOutBanner(private val inBubble: Boolean, private val actionListener: (Boolean) -> Unit) : Banner<Unit>() {

  override val enabled: Boolean
    get() = inBubble && !ZonaRosaStore.tooltips.hasSeenBubbleOptOutTooltip() && Build.VERSION.SDK_INT > 29

  override val dataFlow: Flow<Unit>
    get() = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) = Banner(contentPadding, actionListener)
}

@Composable
private fun Banner(contentPadding: PaddingValues, actionListener: (Boolean) -> Unit = {}) {
  DefaultBanner(
    title = null,
    body = stringResource(id = R.string.BubbleOptOutTooltip__description),
    actions = listOf(
      Action(R.string.BubbleOptOutTooltip__turn_off) {
        actionListener(true)
      },
      Action(R.string.BubbleOptOutTooltip__not_now) {
        actionListener(false)
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

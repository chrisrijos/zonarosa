/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.banner.banners

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import kotlinx.coroutines.flow.flowOf
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.R
import io.zonarosa.messenger.banner.Banner
import io.zonarosa.messenger.banner.ui.compose.Action
import io.zonarosa.messenger.banner.ui.compose.DefaultBanner
import io.zonarosa.messenger.banner.ui.compose.Importance
import io.zonarosa.messenger.contacts.sync.CdsTemporaryErrorBottomSheet
import io.zonarosa.messenger.keyvalue.ZonaRosaStore

class CdsTemporaryErrorBanner(private val fragmentManager: FragmentManager) : Banner<Unit>() {

  override val enabled: Boolean
    get() {
      val timeUntilUnblock = ZonaRosaStore.misc.cdsBlockedUtil - System.currentTimeMillis()
      return ZonaRosaStore.misc.isCdsBlocked && timeUntilUnblock < CdsPermanentErrorBanner.PERMANENT_TIME_CUTOFF
    }

  override val dataFlow
    get() = flowOf(Unit)

  @Composable
  override fun DisplayBanner(model: Unit, contentPadding: PaddingValues) {
    Banner(
      contentPadding = contentPadding,
      onLearnMoreClicked = { CdsTemporaryErrorBottomSheet.show(fragmentManager) }
    )
  }
}

@Composable
private fun Banner(contentPadding: PaddingValues, onLearnMoreClicked: () -> Unit = {}) {
  DefaultBanner(
    title = null,
    body = stringResource(id = R.string.reminder_cds_warning_body),
    importance = Importance.ERROR,
    actions = listOf(
      Action(R.string.reminder_cds_warning_learn_more) {
        onLearnMoreClicked()
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

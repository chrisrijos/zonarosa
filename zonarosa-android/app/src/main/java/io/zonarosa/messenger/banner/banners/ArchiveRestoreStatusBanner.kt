/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.banner.banners

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgress
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgressState
import io.zonarosa.messenger.backup.v2.ArchiveRestoreProgressState.RestoreStatus
import io.zonarosa.messenger.backup.v2.ui.status.ArchiveRestoreStatusBanner
import io.zonarosa.messenger.banner.Banner

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveRestoreStatusBanner(private val listener: RestoreProgressBannerListener) : Banner<ArchiveRestoreProgressState>() {

  override val enabled: Boolean
    get() = ArchiveRestoreProgress.state.let { it.restoreState.isMediaRestoreOperation || it.restoreStatus == RestoreStatus.FINISHED }

  override val dataFlow: Flow<ArchiveRestoreProgressState> by lazy {
    ArchiveRestoreProgress
      .stateFlow
      .filter {
        it.restoreStatus != RestoreStatus.NONE && (it.restoreState.isMediaRestoreOperation || it.restoreStatus == RestoreStatus.FINISHED)
      }
  }

  @Composable
  override fun DisplayBanner(model: ArchiveRestoreProgressState, contentPadding: PaddingValues) {
    ArchiveRestoreStatusBanner(
      data = model,
      onBannerClick = listener::onBannerClick,
      onActionClick = listener::onActionClick,
      onDismissClick = {
        ArchiveRestoreProgress.clearFinishedStatus()
        listener.onDismissComplete()
      }
    )
  }

  interface RestoreProgressBannerListener {
    fun onBannerClick()
    fun onActionClick(data: ArchiveRestoreProgressState)
    fun onDismissComplete()
  }
}

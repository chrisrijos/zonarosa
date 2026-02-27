/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.stickers.manage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import io.zonarosa.core.ui.compose.copied.androidx.compose.DragAndDropEvent
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme
import io.zonarosa.messenger.PassphraseRequiredActivity
import io.zonarosa.messenger.R
import io.zonarosa.messenger.conversation.mutiselect.forward.MultiselectForwardFragment
import io.zonarosa.messenger.conversation.mutiselect.forward.MultiselectForwardFragmentArgs
import io.zonarosa.messenger.database.model.StickerPackId
import io.zonarosa.messenger.database.model.StickerPackKey
import io.zonarosa.messenger.sharing.MultiShareArgs
import io.zonarosa.messenger.stickers.StickerUrl
import io.zonarosa.messenger.stickers.preview.StickerPackPreviewActivity
import io.zonarosa.messenger.util.viewModel

/**
 * Activity implementation of [StickerManagementScreen].
 */
class StickerManagementActivity : PassphraseRequiredActivity() {
  companion object {
    @JvmStatic
    fun createIntent(context: Context): Intent = Intent(context, StickerManagementActivity::class.java)
  }

  private val viewModel by viewModel { StickerManagementViewModel() }

  override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
    super.onCreate(savedInstanceState, ready)

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.onScreenVisible()
      }
    }

    setContent {
      val uiState by viewModel.uiState.collectAsStateWithLifecycle()

      ZonaRosaTheme {
        StickerManagementScreen(
          uiState = uiState,
          onNavigateBack = ::supportFinishAfterTransition,
          onSetMultiSelectModeEnabled = viewModel::setMultiSelectEnabled,
          onSnackbarDismiss = viewModel::onSnackbarDismiss,
          availableTabCallbacks = remember {
            object : AvailableStickersContentCallbacks {
              override fun onForwardClick(pack: AvailableStickerPack) = openShareSheet(pack.id, pack.key)
              override fun onInstallClick(pack: AvailableStickerPack) = viewModel.installStickerPack(pack)
              override fun onShowPreviewClick(pack: AvailableStickerPack) = navigateToStickerPreview(pack.id, pack.key)
            }
          },
          installedTabCallbacks = remember {
            object : InstalledStickersContentCallbacks {
              override fun onForwardClick(pack: InstalledStickerPack) = openShareSheet(pack.id, pack.key)
              override fun onRemoveClick(packIds: Set<StickerPackId>) = viewModel.onUninstallStickerPacksRequested(packIds)
              override fun onRemoveStickerPacksConfirmed(packIds: Set<StickerPackId>) = viewModel.onUninstallStickerPacksConfirmed(packIds)
              override fun onRemoveStickerPacksCanceled() = viewModel.onUninstallStickerPacksCanceled()
              override fun onSelectionToggle(pack: InstalledStickerPack) = viewModel.toggleSelection(pack)
              override fun onSelectAllToggle() = viewModel.toggleSelectAll()
              override fun onDragAndDropEvent(event: DragAndDropEvent) {
                when (event) {
                  is DragAndDropEvent.OnItemMove -> viewModel.updatePosition(event.fromIndex, event.toIndex)
                  is DragAndDropEvent.OnItemDrop -> viewModel.saveInstalledPacksSortOrder()
                  is DragAndDropEvent.OnDragCancel -> {}
                }
              }

              override fun onShowPreviewClick(pack: InstalledStickerPack) = navigateToStickerPreview(pack.id, pack.key)
            }
          }
        )
      }
    }
  }

  private fun openShareSheet(packId: StickerPackId, packKey: StickerPackKey) {
    MultiselectForwardFragment.showBottomSheet(
      supportFragmentManager = supportFragmentManager,
      multiselectForwardFragmentArgs = MultiselectForwardFragmentArgs(
        multiShareArgs = listOf(
          MultiShareArgs.Builder()
            .withDraftText(StickerUrl.createShareLink(packId.value, packKey.value))
            .build()
        ),
        title = R.string.StickerManagement_share_sheet_title
      )
    )
  }

  private fun navigateToStickerPreview(packId: StickerPackId, packKey: StickerPackKey) {
    startActivity(StickerPackPreviewActivity.getIntent(packId.value, packKey.value))
  }
}

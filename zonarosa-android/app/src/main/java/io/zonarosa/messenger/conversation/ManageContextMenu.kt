package io.zonarosa.messenger.conversation

import android.content.Context
import android.view.View
import android.view.ViewGroup
import io.zonarosa.core.util.DimensionUnit
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.menu.ActionItem
import io.zonarosa.messenger.components.menu.ZonaRosaContextMenu
import io.zonarosa.core.ui.R as CoreUiR

/**
 * A context menu shown when handling selected media only permissions.
 * Will give users the ability to go to settings or to choose more media to give permission to
 */
object ManageContextMenu {

  fun show(
    context: Context,
    anchorView: View,
    rootView: ViewGroup = anchorView.rootView as ViewGroup,
    showAbove: Boolean = false,
    showAtStart: Boolean = false,
    onSelectMore: () -> Unit,
    onSettings: () -> Unit
  ) {
    show(
      context = context,
      anchorView = anchorView,
      rootView = rootView,
      showAbove = showAbove,
      showAtStart = showAtStart,
      callbacks = object : Callbacks {
        override fun onSelectMore() = onSelectMore()
        override fun onSettings() = onSettings()
      }
    )
  }

  private fun show(
    context: Context,
    anchorView: View,
    rootView: ViewGroup = anchorView.rootView as ViewGroup,
    showAbove: Boolean = false,
    showAtStart: Boolean = false,
    callbacks: Callbacks
  ) {
    val actions = mutableListOf<ActionItem>().apply {
      add(
        ActionItem(CoreUiR.drawable.symbol_settings_android_24, context.getString(R.string.AttachmentKeyboard_go_to_settings)) {
          callbacks.onSettings()
        }
      )
      add(
        ActionItem(R.drawable.symbol_album_tilt_24, context.getString(R.string.AttachmentKeyboard_select_more_photos)) {
          callbacks.onSelectMore()
        }
      )
    }

    if (!showAbove) {
      actions.reverse()
    }

    ZonaRosaContextMenu.Builder(anchorView, rootView)
      .preferredHorizontalPosition(if (showAtStart) ZonaRosaContextMenu.HorizontalPosition.START else ZonaRosaContextMenu.HorizontalPosition.END)
      .preferredVerticalPosition(if (showAbove) ZonaRosaContextMenu.VerticalPosition.ABOVE else ZonaRosaContextMenu.VerticalPosition.BELOW)
      .offsetY(DimensionUnit.DP.toPixels(8f).toInt())
      .show(actions)
  }

  private interface Callbacks {
    fun onSelectMore()
    fun onSettings()
  }
}

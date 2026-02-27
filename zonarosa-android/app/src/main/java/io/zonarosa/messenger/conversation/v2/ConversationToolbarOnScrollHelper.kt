package io.zonarosa.messenger.conversation.v2

import android.view.View
import androidx.annotation.ColorRes
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import io.zonarosa.messenger.R
import io.zonarosa.messenger.util.Material3OnScrollHelper
import io.zonarosa.messenger.wallpaper.ChatWallpaper
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Scroll helper to manage the color state of the top bar and status bar.
 */
class ConversationToolbarOnScrollHelper(
  activity: FragmentActivity,
  toolbarBackground: View,
  private val wallpaperProvider: () -> ChatWallpaper?,
  lifecycleOwner: LifecycleOwner
) : Material3OnScrollHelper(
  activity = activity,
  views = listOf(toolbarBackground),
  lifecycleOwner = lifecycleOwner,
  setStatusBarColor = {}
) {
  override val activeColorSet: ColorSet
    get() = ColorSet(getActiveToolbarColor(wallpaperProvider() != null))

  override val inactiveColorSet: ColorSet
    get() = ColorSet(getInactiveToolbarColor(wallpaperProvider() != null))

  @ColorRes
  private fun getActiveToolbarColor(hasWallpaper: Boolean): Int {
    return if (hasWallpaper) R.color.conversation_toolbar_color_wallpaper_scrolled else CoreUiR.color.zonarosa_colorSurface2
  }

  @ColorRes
  private fun getInactiveToolbarColor(hasWallpaper: Boolean): Int {
    return if (hasWallpaper) R.color.conversation_toolbar_color_wallpaper else CoreUiR.color.zonarosa_colorBackground
  }
}

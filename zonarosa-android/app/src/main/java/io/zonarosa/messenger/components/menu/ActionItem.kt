package io.zonarosa.messenger.components.menu

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Represents an action to be rendered via [ZonaRosaContextMenu] or [ZonaRosaBottomActionBar]
 */
data class ActionItem @JvmOverloads constructor(
  @DrawableRes val iconRes: Int,
  val title: CharSequence,
  @ColorRes val tintRes: Int = CoreUiR.color.zonarosa_colorOnSurface,
  val action: Runnable
)

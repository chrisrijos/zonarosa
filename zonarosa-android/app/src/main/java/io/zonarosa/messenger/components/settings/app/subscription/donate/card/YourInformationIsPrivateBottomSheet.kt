package io.zonarosa.messenger.components.settings.app.subscription.donate.card

import io.zonarosa.core.util.dp
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.DSLConfiguration
import io.zonarosa.messenger.components.settings.DSLSettingsAdapter
import io.zonarosa.messenger.components.settings.DSLSettingsBottomSheetFragment
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.configure
import io.zonarosa.core.ui.R as CoreUiR

/**
 * Displays information about how ZonaRosa keeps card details private and how
 * ZonaRosa does not link donation information to your ZonaRosa account.
 */
class YourInformationIsPrivateBottomSheet : DSLSettingsBottomSheetFragment() {
  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    adapter.submitList(getConfiguration().toMappingModelList())
  }

  private fun getConfiguration(): DSLConfiguration {
    return configure {
      space(10.dp)

      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.YourInformationIsPrivateBottomSheet__your_information_is_private,
          DSLSettingsText.CenterModifier,
          DSLSettingsText.TextAppearanceModifier(CoreUiR.style.ZonaRosa_Text_HeadlineMedium)
        )
      )

      space(24.dp)

      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.YourInformationIsPrivateBottomSheet__zonarosa_does_not_collect,
          DSLSettingsText.BodyLargeModifier
        )
      )

      space(24.dp)

      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.YourInformationIsPrivateBottomSheet__we_use_stripe,
          DSLSettingsText.BodyLargeModifier
        )
      )

      space(24.dp)

      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.YourInformationIsPrivateBottomSheet__zonarosa_does_not_and_cannot,
          DSLSettingsText.BodyLargeModifier
        )
      )

      space(24.dp)

      noPadTextPref(
        title = DSLSettingsText.from(
          R.string.YourInformationIsPrivateBottomSheet__thank_you,
          DSLSettingsText.BodyLargeModifier
        )
      )

      space(56.dp)
    }
  }
}

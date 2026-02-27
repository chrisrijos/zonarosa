package io.zonarosa.messenger.components.settings.app.notifications.manual

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import io.zonarosa.core.ui.BottomSheetUtil
import io.zonarosa.core.util.DimensionUnit
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.DSLConfiguration
import io.zonarosa.messenger.components.settings.DSLSettingsAdapter
import io.zonarosa.messenger.components.settings.DSLSettingsBottomSheetFragment
import io.zonarosa.messenger.components.settings.DSLSettingsText
import io.zonarosa.messenger.components.settings.app.AppSettingsActivity
import io.zonarosa.messenger.components.settings.app.notifications.manual.models.NotificationProfileSelection
import io.zonarosa.messenger.components.settings.app.notifications.profiles.NotificationProfilesRepository
import io.zonarosa.messenger.components.settings.configure
import io.zonarosa.messenger.notifications.profiles.NotificationProfile
import io.zonarosa.messenger.notifications.profiles.NotificationProfiles

/**
 * BottomSheetDialogFragment that allows a user to select a notification profile to manually enable/disable.
 */
class NotificationProfileSelectionFragment : DSLSettingsBottomSheetFragment() {

  private val viewModel: NotificationProfileSelectionViewModel by viewModels(
    factoryProducer = {
      NotificationProfileSelectionViewModel.Factory(NotificationProfilesRepository())
    }
  )

  override fun bindAdapter(adapter: DSLSettingsAdapter) {
    NotificationProfileSelection.register(adapter)

    recyclerView.itemAnimator = null

    viewModel.state.observe(viewLifecycleOwner) {
      adapter.submitList(getConfiguration(it).toMappingModelList())
    }
  }

  private fun getConfiguration(state: NotificationProfileSelectionState): DSLConfiguration {
    val activeProfile: NotificationProfile? = NotificationProfiles.getActiveProfile(state.notificationProfiles)

    return configure {
      state.notificationProfiles.sortedDescending().forEach { profile ->
        customPref(
          NotificationProfileSelection.Entry(
            isOn = profile == activeProfile,
            summary = if (profile == activeProfile) DSLSettingsText.from(NotificationProfiles.getActiveProfileDescription(requireContext(), profile)) else DSLSettingsText.from(R.string.NotificationProfileDetails__off),
            notificationProfile = profile,
            isExpanded = profile.id == state.expandedId,
            timeSlotB = state.timeSlotB,
            onRowClick = viewModel::toggleEnabled,
            onTimeSlotAClick = viewModel::enableForOneHour,
            onTimeSlotBClick = viewModel::enableUntil,
            onToggleClick = viewModel::setExpanded,
            onViewSettingsClick = { navigateToSettings(it) }
          )
        )
        space(DimensionUnit.DP.toPixels(16f).toInt())
      }

      customPref(
        NotificationProfileSelection.New(
          onClick = {
            startActivity(AppSettingsActivity.createNotificationProfile(requireContext()))
            dismissAllowingStateLoss()
          }
        )
      )

      space(DimensionUnit.DP.toPixels(20f).toInt())
    }
  }

  private fun navigateToSettings(notificationProfile: NotificationProfile) {
    startActivity(AppSettingsActivity.notificationProfileDetails(requireContext(), notificationProfile.id))
    dismissAllowingStateLoss()
  }

  companion object {
    @JvmStatic
    fun show(fragmentManager: FragmentManager) {
      NotificationProfileSelectionFragment().show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG)
    }
  }
}

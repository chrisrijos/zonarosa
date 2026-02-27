/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import io.zonarosa.core.ui.BottomSheetUtil
import io.zonarosa.core.ui.FixedRoundedCornerBottomSheetDialogFragment
import io.zonarosa.core.util.concurrent.LifecycleDisposable
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.databinding.PromptBatterySaverBottomSheetBinding
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.notifications.DeviceSpecificNotificationConfig
import io.zonarosa.messenger.util.LocalMetrics
import io.zonarosa.messenger.util.PowerManagerCompat
import io.zonarosa.core.ui.R as CoreUiR

class PromptBatterySaverDialogFragment : FixedRoundedCornerBottomSheetDialogFragment() {

  companion object {
    private val TAG = Log.tag(PromptBatterySaverDialogFragment::class.java)
    private const val ARG_LEARN_MORE_LINK = "arg.learn.more.link"

    @JvmStatic
    fun show(fragmentManager: FragmentManager) {
      if (fragmentManager.findFragmentByTag(BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG) == null) {
        val dialog = PromptBatterySaverDialogFragment().apply {
          arguments = bundleOf(
            ARG_LEARN_MORE_LINK to DeviceSpecificNotificationConfig.currentConfig.link
          )
        }
        BottomSheetUtil.show(fragmentManager, BottomSheetUtil.STANDARD_BOTTOM_SHEET_FRAGMENT_TAG, dialog)
        ZonaRosaStore.uiHints.lastBatterySaverPrompt = System.currentTimeMillis()
      }
    }
  }

  override val peekHeightPercentage: Float = 0.66f
  override val themeResId: Int = R.style.Widget_ZonaRosa_FixedRoundedCorners_Messages

  private val binding by ViewBinderDelegate(PromptBatterySaverBottomSheetBinding::bind)

  private val disposables: LifecycleDisposable = LifecycleDisposable()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    return inflater.inflate(R.layout.prompt_battery_saver_bottom_sheet, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    disposables.bindTo(viewLifecycleOwner)

    val learnMoreLink = arguments?.getString(ARG_LEARN_MORE_LINK) ?: getString(R.string.PromptBatterySaverBottomSheet__learn_more_url)
    binding.message.setLearnMoreVisible(true)
    binding.message.setLinkColor(ContextCompat.getColor(requireContext(), CoreUiR.color.zonarosa_colorPrimary))
    binding.message.setLink(learnMoreLink)

    binding.continueButton.setOnClickListener {
      PowerManagerCompat.requestIgnoreBatteryOptimizations(requireContext())
      Log.i(TAG, "Requested to ignore battery optimizations, clearing local metrics.")
      LocalMetrics.clear()
      ZonaRosaStore.uiHints.markDismissedBatterySaverPrompt()
      dismiss()
    }
    binding.dismissButton.setOnClickListener {
      Log.i(TAG, "User denied request to ignore battery optimizations.")
      ZonaRosaStore.uiHints.markDismissedBatterySaverPrompt()
      dismiss()
    }
  }
}

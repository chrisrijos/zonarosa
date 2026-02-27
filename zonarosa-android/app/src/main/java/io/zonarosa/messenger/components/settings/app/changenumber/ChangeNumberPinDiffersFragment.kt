/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.changenumber

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.zonarosa.core.ui.logging.LoggingFragment
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.settings.app.changenumber.ChangeNumberUtil.changeNumberSuccess
import io.zonarosa.messenger.lock.v2.CreateSvrPinActivity

/**
 * A screen to educate the user if their PIN differs from old number to new number.
 */
class ChangeNumberPinDiffersFragment : LoggingFragment(R.layout.fragment_change_number_pin_differs) {

  companion object {
    private val TAG = Log.tag(ChangeNumberPinDiffersFragment::class.java)
  }

  private val confirmCancelDialog = object : OnBackPressedCallback(true) {
    override fun handleOnBackPressed() {
      MaterialAlertDialogBuilder(requireContext())
        .setMessage(R.string.ChangeNumberPinDiffersFragment__keep_old_pin_question)
        .setPositiveButton(android.R.string.ok) { _, _ -> changeNumberSuccess() }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    view.findViewById<View>(R.id.change_number_pin_differs_keep_old_pin).setOnClickListener {
      changeNumberSuccess()
    }

    val changePin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == CreateSvrPinActivity.RESULT_OK) {
        changeNumberSuccess()
      }
    }

    view.findViewById<View>(R.id.change_number_pin_differs_update_pin).setOnClickListener {
      changePin.launch(CreateSvrPinActivity.getIntentForPinChangeFromSettings(requireContext()))
    }

    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, confirmCancelDialog)
  }
}

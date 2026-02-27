/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.quality

import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import io.zonarosa.core.ui.compose.ComposeFullScreenDialogFragment
import io.zonarosa.storageservice.protos.calls.quality.SubmitCallQualitySurveyRequest

class CallQualityDiagnosticsFragment : ComposeFullScreenDialogFragment() {

  companion object {
    private const val REQUEST_KEY = "CallQualityDiagnosticsRequestKey"

    fun create(request: SubmitCallQualitySurveyRequest): CallQualityDiagnosticsFragment {
      return CallQualityDiagnosticsFragment().apply {
        arguments = bundleOf(REQUEST_KEY to request.encode())
      }
    }
  }

  private val callQualitySurveyRequest: SubmitCallQualitySurveyRequest by lazy {
    val bytes = requireArguments().getByteArray(REQUEST_KEY)!!
    SubmitCallQualitySurveyRequest.ADAPTER.decode(bytes)
  }

  @Composable
  override fun DialogContent() {
    CallQualityDiagnosticsScreen(
      callQualitySurveyRequest = callQualitySurveyRequest,
      onNavigationClick = { requireActivity().onBackPressedDispatcher.onBackPressed() }
    )
  }
}

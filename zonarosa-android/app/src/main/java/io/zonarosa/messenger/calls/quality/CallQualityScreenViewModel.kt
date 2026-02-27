/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.quality

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import io.zonarosa.core.util.logging.Log
import io.zonarosa.storageservice.protos.calls.quality.SubmitCallQualitySurveyRequest
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobmanager.impl.NetworkConstraint
import io.zonarosa.messenger.jobs.CallQualitySurveySubmissionJob

class CallQualityScreenViewModel(
  val initialRequest: SubmitCallQualitySurveyRequest
) : ViewModel() {

  companion object {
    private val TAG = Log.tag(CallQualityScreenViewModel::class)
  }

  private val internalState = MutableStateFlow(CallQualitySheetState())
  val state: StateFlow<CallQualitySheetState> = internalState

  fun setUserSatisfiedWithCall(userSatisfiedWithCall: Boolean) {
    internalState.update { it.copy(isUserSatisfiedWithCall = userSatisfiedWithCall) }
  }

  fun onCallQualityIssueSelectionChanged(selection: Set<CallQualityIssue>) {
    internalState.update { it.copy(selectedQualityIssues = selection) }
  }

  fun onSomethingElseDescriptionChanged(somethingElseDescription: String) {
    internalState.update { it.copy(somethingElseDescription = somethingElseDescription) }
  }

  fun onShareDebugLogChanged(shareDebugLog: Boolean) {
    internalState.update { it.copy(isShareDebugLogSelected = shareDebugLog) }
  }

  fun clearFailedDueToNetworkAvailability() {
    internalState.update { it.copy(failedDueToNetworkAvailability = false) }
  }

  fun submit() {
    if (!NetworkConstraint.isMet(AppDependencies.application)) {
      Log.w(TAG, "User does not have a network connection. Failing immediately with retry dialog.")
      internalState.update { it.copy(failedDueToNetworkAvailability = true) }
      return
    }

    if (initialRequest.call_type.isEmpty()) {
      Log.i(TAG, "Ignoring survey submission for blank call_type.")
      return
    }

    AppDependencies.jobManager.add(CallQualitySurveySubmissionJob(getRequestSnapshot(), state.value.isShareDebugLogSelected))
  }

  fun getRequestSnapshot(): SubmitCallQualitySurveyRequest {
    val stateSnapshot = state.value
    val somethingElseDescription: String? = if (stateSnapshot.selectedQualityIssues.contains(CallQualityIssue.SOMETHING_ELSE)) {
      stateSnapshot.somethingElseDescription.takeIf { it.isNotEmpty() }
    } else {
      null
    }

    return initialRequest.newBuilder()
      .user_satisfied(stateSnapshot.isUserSatisfiedWithCall)
      .call_quality_issues(stateSnapshot.selectedQualityIssues.map { it.code })
      .additional_issues_description(somethingElseDescription)
      .build()
  }
}

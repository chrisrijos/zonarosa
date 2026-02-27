/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.quality

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.compose.Buttons
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.Scaffolds
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.core.ui.compose.horizontalGutters
import io.zonarosa.storageservice.protos.calls.quality.SubmitCallQualitySurveyRequest
import io.zonarosa.messenger.R

@Composable
fun CallQualityDiagnosticsScreen(
  callQualitySurveyRequest: SubmitCallQualitySurveyRequest,
  onNavigationClick: () -> Unit = {}
) {
  Scaffolds.Settings(
    title = stringResource(R.string.CallQualityDiagnosticsScreen__diagnostic_information),
    navigationIcon = ZonaRosaIcons.ArrowStart.imageVector,
    navigationContentDescription = stringResource(R.string.CallQualityDiagnosticsScreen__close),
    onNavigationClick = onNavigationClick
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(it)
        .fillMaxSize()
        .horizontalGutters()
    ) {
      Box(
        modifier = Modifier
          .weight(1f)
          .verticalScroll(rememberScrollState())
      ) {
        Text(
          text = callQualitySurveyRequest.toString().substringAfter(SubmitCallQualitySurveyRequest::class.simpleName ?: "")
        )
      }

      Buttons.LargeTonal(
        onClick = onNavigationClick,
        modifier = Modifier
          .padding(top = 10.dp, bottom = 24.dp)
          .widthIn(min = 256.dp)
      ) {
        Text(text = stringResource(R.string.CallQualityDiagnosticsScreen__close))
      }
    }
  }
}

@DayNightPreviews
@Composable
private fun CallQualityDiagnosticsScreenPreview() {
  Previews.Preview {
    CallQualityDiagnosticsScreen(
      callQualitySurveyRequest = SubmitCallQualitySurveyRequest()
    )
  }
}

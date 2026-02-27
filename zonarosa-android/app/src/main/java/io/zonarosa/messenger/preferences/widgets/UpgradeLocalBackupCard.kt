/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.preferences.widgets

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.compose.Buttons
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme
import io.zonarosa.messenger.R

object UpgradeLocalBackupCard {

  @JvmStatic
  fun ComposeView.bind(
    onClick: () -> Unit
  ) {
    setContent {
      ZonaRosaTheme {
        UpgradeLocalBackupCardComponent(onClick = onClick)
      }
    }
  }
}

@Composable
private fun UpgradeLocalBackupCardComponent(onClick: () -> Unit) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 12.dp)
      .padding(top = 8.dp, bottom = 16.dp)
      .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.38f), shape = RoundedCornerShape(12.dp))
      .padding(16.dp)
  ) {
    Icon(
      imageVector = ImageVector.vectorResource(R.drawable.symbol_key_24),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.padding(end = 16.dp)
    )

    Text(
      text = stringResource(R.string.OnDeviceBackupsSettingsScreen__update_to_a_new_recovery_key),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier.padding(end = 8.dp).weight(1f)
    )

    Buttons.Small(
      onClick = onClick
    ) {
      Text(
        text = stringResource(R.string.OnDeviceBackupsSettingsScreen__update)
      )
    }
  }
}

@DayNightPreviews
@Composable
private fun UpgradeLocalBackupCardComponentPreview() {
  Previews.Preview {
    UpgradeLocalBackupCardComponent(onClick = {})
  }
}

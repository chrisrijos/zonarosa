/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.ui.restore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.ZonaRosaIcons
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme
import io.zonarosa.messenger.R

/**
 * Renders row-ux used commonly through the restore flows.
 */
@Composable
fun RestoreRow(
  icon: Painter,
  title: String,
  subtitle: String,
  onRowClick: () -> Unit = {}
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .padding(bottom = 16.dp)
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(ZonaRosaTheme.colors.colorSurface2)
      .clickable(enabled = true, onClick = onRowClick)
      .padding(horizontal = 20.dp, vertical = 22.dp)
  ) {
    Icon(
      painter = icon,
      tint = MaterialTheme.colorScheme.primary,
      contentDescription = null,
      modifier = Modifier.size(48.dp)
    )

    Column(
      modifier = Modifier.padding(start = 16.dp)
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge
      )

      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}

@DayNightPreviews
@Composable
private fun RestoreMethodRowPreview() {
  Previews.Preview {
    RestoreRow(
      icon = ZonaRosaIcons.Backup.painter,
      title = stringResource(R.string.SelectRestoreMethodFragment__restore_zonarosa_backup),
      subtitle = stringResource(R.string.SelectRestoreMethodFragment__restore_your_text_messages_and_media_from)
    )
  }
}

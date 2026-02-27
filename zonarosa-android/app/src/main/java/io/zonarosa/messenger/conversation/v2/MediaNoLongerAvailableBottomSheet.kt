/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.v2

import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.compose.BottomSheets
import io.zonarosa.core.ui.compose.Buttons
import io.zonarosa.core.ui.compose.ComposeBottomSheetDialogFragment
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.horizontalGutters
import io.zonarosa.messenger.R
import io.zonarosa.messenger.backup.v2.MessageBackupTier
import io.zonarosa.messenger.components.settings.app.subscription.MessageBackupsCheckoutLauncher.createBackupsCheckoutLauncher

/**
 * Bottom sheet displayed when the user taps media that is not available for download,
 * over 30 days old, and they do not currently have a subscription.
 */
class MediaNoLongerAvailableBottomSheet : ComposeBottomSheetDialogFragment() {

  private lateinit var checkoutLauncher: ActivityResultLauncher<MessageBackupTier?>

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    checkoutLauncher = createBackupsCheckoutLauncher {
      dismissAllowingStateLoss()
    }
  }

  @Composable
  override fun SheetContent() {
    MediaNoLongerAvailableBottomSheetContent(
      onContinueClick = {
        checkoutLauncher.launch(MessageBackupTier.PAID)
      },
      onNotNowClick = { dismissAllowingStateLoss() }
    )
  }
}

@Composable
private fun MediaNoLongerAvailableBottomSheetContent(
  onContinueClick: () -> Unit = {},
  onNotNowClick: () -> Unit = {}
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .fillMaxWidth()
      .horizontalGutters()
  ) {
    BottomSheets.Handle()

    Image(
      painter = painterResource(R.drawable.image_zonarosa_backups_media),
      contentDescription = null,
      modifier = Modifier.padding(vertical = 16.dp)
    )

    Text(
      text = stringResource(R.string.MediaNoLongerAvailableSheet__this_media_is_no_longer_available),
      style = MaterialTheme.typography.titleLarge,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 10.dp)
    )

    Text(
      text = stringResource(R.string.MediaNoLongerAvailableSheet__to_start_backing_up_all_your_media),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(bottom = 92.dp)
    )

    Buttons.LargeTonal(
      onClick = onContinueClick,
      modifier = Modifier
        .padding(bottom = 22.dp)
        .defaultMinSize(minWidth = 220.dp)
    ) {
      Text(
        text = stringResource(R.string.MediaNoLongerAvailableSheet__continue)
      )
    }

    TextButton(
      onClick = onNotNowClick,
      modifier = Modifier
        .padding(bottom = 32.dp)
        .defaultMinSize(minWidth = 220.dp)
    ) {
      Text(
        text = stringResource(R.string.MediaNoLongerAvailableSheet__not_now)
      )
    }
  }
}

@DayNightPreviews
@Composable
private fun MediaNoLongerAvailableBottomSheetContentPreview() {
  Previews.BottomSheetContentPreview {
    MediaNoLongerAvailableBottomSheetContent()
  }
}

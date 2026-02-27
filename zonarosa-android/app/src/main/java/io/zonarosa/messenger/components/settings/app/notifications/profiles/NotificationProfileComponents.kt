/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.notifications.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.core.ui.compose.Rows
import io.zonarosa.core.ui.compose.horizontalGutters
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme
import io.zonarosa.messenger.R
import io.zonarosa.messenger.avatar.AvatarImage
import io.zonarosa.messenger.components.emoji.Emojifier
import io.zonarosa.messenger.components.settings.app.subscription.BadgeImageMedium
import io.zonarosa.messenger.database.RecipientTable
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.rememberRecipientField

@Composable
fun NotificationProfileAddMembers(
  onClick: () -> Unit
) {
  Rows.TextRow(
    icon = {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_plus_24),
        contentDescription = null,
        modifier = Modifier
          .size(40.dp)
          .background(
            color = ZonaRosaTheme.colors.colorSurface1,
            shape = CircleShape
          )
          .padding(8.dp)
      )
    },
    text = {
      Text(
        text = stringResource(R.string.AddAllowedMembers__add_people_or_groups),
        style = MaterialTheme.typography.bodyLarge
      )
    },
    onClick = onClick
  )
}

@Composable
fun NotificationProfileRecipient(
  recipient: Recipient,
  onRemoveClick: (RecipientId) -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .defaultMinSize(minHeight = 64.dp)
      .horizontalGutters()
  ) {
    val context = LocalContext.current
    val featuredBadge by rememberRecipientField(recipient) { recipient.featuredBadge }
    val displayName by rememberRecipientField(recipient) { recipient.getDisplayName(context) }

    Box(
      modifier = Modifier.padding(top = 6.dp)
    ) {
      AvatarImage(
        recipient = recipient,
        modifier = Modifier.size(40.dp)
      )

      BadgeImageMedium(
        badge = featuredBadge,
        modifier = Modifier
          .padding(top = 22.dp, start = 20.dp)
          .size(24.dp)
      )
    }

    Spacer(modifier = Modifier.size(20.dp))

    Emojifier(displayName) { string, map ->
      Text(
        text = string,
        inlineContent = map,
        modifier = Modifier.weight(1f)
      )
    }

    IconButton(onClick = {
      onRemoveClick(recipient.id)
    }) {
      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_minus_circle_20),
        contentDescription = stringResource(R.string.delete),
        tint = colorResource(R.color.core_grey_45)
      )
    }
  }
}

@DayNightPreviews
@Composable
fun NotificationProfileAddMembersPreview() {
  Previews.Preview {
    NotificationProfileAddMembers(
      onClick = {}
    )
  }
}

@DayNightPreviews
@Composable
fun NotificationProfileRecipientPreview() {
  Previews.Preview {
    NotificationProfileRecipient(
      recipient = Recipient(
        id = RecipientId.from(1L),
        isResolving = false,
        registeredValue = RecipientTable.RegisteredState.REGISTERED,
        systemContactName = "Miles Morales"
      ),
      onRemoveClick = {}
    )
  }
}

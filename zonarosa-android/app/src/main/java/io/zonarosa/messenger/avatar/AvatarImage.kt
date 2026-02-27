/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.avatar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.R
import io.zonarosa.messenger.components.AvatarImageView
import io.zonarosa.messenger.conversation.colors.AvatarColor
import io.zonarosa.messenger.database.model.ProfileAvatarFileDetails
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.rememberRecipientField

@Composable
fun AvatarImage(
  recipient: Recipient,
  modifier: Modifier = Modifier,
  useProfile: Boolean = true,
  contentDescription: String? = null
) {
  AvatarImage(
    recipientId = recipient.id,
    modifier = modifier,
    useProfile = useProfile,
    contentDescription = contentDescription
  )
}

@Composable
fun AvatarImage(
  recipientId: RecipientId,
  modifier: Modifier = Modifier,
  useProfile: Boolean = true,
  contentDescription: String? = null
) {
  if (LocalInspectionMode.current) {
    Image(
      painter = painterResource(R.drawable.ic_avatar_abstract_02),
      contentDescription = null,
      modifier = modifier
        .background(color = Color(AvatarColor.random().colorInt()), CircleShape)
    )
  } else {
    val context = LocalContext.current
    val avatarImageState by rememberRecipientField(recipientId) {
      AvatarImageState(
        getDisplayName(context),
        this,
        profileAvatarFileDetails
      )
    }

    AndroidView(
      factory = {
        AvatarImageView(context).apply {
          initialize(context, null)
          this.contentDescription = contentDescription
        }
      },
      modifier = modifier.background(color = Color.Transparent, shape = CircleShape)
    ) {
      if (useProfile) {
        it.setAvatarUsingProfile(avatarImageState.self)
      } else {
        it.setAvatar(avatarImageState.self)
      }
    }
  }
}

@DayNightPreviews
@Composable
private fun AvatarImagePreview() {
  Previews.Preview {
    AvatarImage(
      recipientId = RecipientId.from(1)
    )
  }
}

private data class AvatarImageState(
  val displayName: String?,
  val self: Recipient,
  val avatarFileDetails: ProfileAvatarFileDetails
)

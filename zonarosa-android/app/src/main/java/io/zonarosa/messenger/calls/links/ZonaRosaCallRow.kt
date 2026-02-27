/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import io.zonarosa.core.ui.compose.Buttons
import io.zonarosa.core.ui.compose.DayNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.R
import io.zonarosa.messenger.conversation.colors.AvatarColorPair
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.CallLinkPeekInfo
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkState
import java.time.Instant
import io.zonarosa.core.ui.R as CoreUiR

@DayNightPreviews
@Composable
private fun ZonaRosaCallRowPreview() {
  val callLink = remember {
    val credentials = CallLinkCredentials(byteArrayOf(1, 2, 3, 4), byteArrayOf(5, 6, 7, 8))
    CallLinkTable.CallLink(
      recipientId = RecipientId.UNKNOWN,
      roomId = CallLinkRoomId.fromBytes(byteArrayOf(1, 3, 5, 7)),
      credentials = credentials,
      state = ZonaRosaCallLinkState(
        name = "Call Name",
        restrictions = io.zonarosa.ringrtc.CallLinkState.Restrictions.NONE,
        expiration = Instant.MAX,
        revoked = false
      ),
      deletionTimestamp = 0L
    )
  }
  Previews.Preview {
    Column(
      verticalArrangement = spacedBy(8.dp)
    ) {
      ZonaRosaCallRow(
        callLink = callLink,
        callLinkPeekInfo = null,
        onJoinClicked = {}
      )

      ZonaRosaCallRow(
        callLink = callLink,
        callLinkPeekInfo = CallLinkPeekInfo(null, true, true),
        onJoinClicked = {}
      )
    }
  }
}

@Composable
fun ZonaRosaCallRow(
  callLink: CallLinkTable.CallLink,
  callLinkPeekInfo: CallLinkPeekInfo?,
  onJoinClicked: (() -> Unit)?,
  modifier: Modifier = Modifier
) {
  val callUrl = if (LocalInspectionMode.current) {
    "https://zonarosa.call.example.com"
  } else {
    remember(callLink.credentials) {
      callLink.credentials?.let { CallLinks.url(it.linkKeyBytes) } ?: ""
    }
  }

  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = dimensionResource(id = CoreUiR.dimen.gutter))
      .border(
        width = 1.25.dp,
        color = MaterialTheme.colorScheme.outline,
        shape = RoundedCornerShape(18.dp)
      )
      .padding(16.dp)
  ) {
    val callColorPair = AvatarColorPair.create(LocalContext.current, callLink.avatarColor)

    Image(
      imageVector = ImageVector.vectorResource(id = R.drawable.symbol_video_display_bold_40),
      contentScale = ContentScale.Inside,
      contentDescription = null,
      colorFilter = ColorFilter.tint(Color(callColorPair.foregroundColor)),
      modifier = Modifier
        .size(64.dp)
        .background(
          color = Color(callColorPair.backgroundColor),
          shape = CircleShape
        )
    )

    Spacer(modifier = Modifier.width(10.dp))

    Column(
      modifier = Modifier
        .weight(1f)
        .align(CenterVertically)
    ) {
      Text(
        text = callLink.state.name.ifEmpty { stringResource(id = R.string.CreateCallLinkBottomSheetDialogFragment__zonarosa_call) },
        color = MaterialTheme.colorScheme.onSurface
      )
      Text(
        text = callUrl,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    if (onJoinClicked != null) {
      Spacer(modifier = Modifier.width(10.dp))

      Buttons.Small(
        onClick = onJoinClicked,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier.align(CenterVertically)
      ) {
        val textId = if (callLinkPeekInfo?.isJoined == true) {
          R.string.CallLogAdapter__return
        } else {
          R.string.CreateCallLinkBottomSheetDialogFragment__join
        }

        Text(text = stringResource(id = textId))
      }
    }
  }
}

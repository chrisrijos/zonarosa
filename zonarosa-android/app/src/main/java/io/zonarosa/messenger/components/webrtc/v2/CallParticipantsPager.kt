/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.v2

import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.zonarosa.core.ui.compose.AllNightPreviews
import io.zonarosa.core.ui.compose.Previews
import io.zonarosa.messenger.conversation.colors.ChatColorsPalette
import io.zonarosa.messenger.events.CallParticipant
import io.zonarosa.messenger.events.CallParticipantId
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

@Composable
fun CallParticipantsPager(
  callParticipantsPagerState: CallParticipantsPagerState,
  pagerState: PagerState,
  modifier: Modifier = Modifier
) {
  if (callParticipantsPagerState.focusedParticipant == null) {
    return
  }

  val firstParticipantAR = rememberParticipantAspectRatio(
    callParticipantsPagerState.callParticipants.firstOrNull()?.videoSink
  )

  // Use movableContentOf to preserve CallGrid state when switching between
  // single participant (no pager) and multiple participants (with pager)
  val callGridContent = remember {
    movableContentOf { state: CallParticipantsPagerState, mod: Modifier, aspectRatio: Float? ->
      CallGrid(
        items = state.callParticipants,
        singleParticipantAspectRatio = aspectRatio,
        modifier = mod,
        itemKey = { it.callParticipantId }
      ) { participant, itemModifier ->
        RemoteParticipantContent(
          participant = participant,
          renderInPip = state.isRenderInPip,
          raiseHandAllowed = false,
          onInfoMoreInfoClick = null,
          showAudioIndicator = state.callParticipants.size > 1,
          modifier = itemModifier
        )
      }
    }
  }

  if (callParticipantsPagerState.callParticipants.size > 1) {
    VerticalPager(
      state = pagerState,
      modifier = modifier
        .displayCutoutPadding()
        .statusBarsPadding()
    ) { page ->
      when (page) {
        0 -> {
          callGridContent(callParticipantsPagerState, Modifier.fillMaxSize(), firstParticipantAR)
        }

        1 -> {
          RemoteParticipantContent(
            participant = callParticipantsPagerState.focusedParticipant,
            renderInPip = callParticipantsPagerState.isRenderInPip,
            raiseHandAllowed = false,
            onInfoMoreInfoClick = null,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  } else {
    callGridContent(callParticipantsPagerState, modifier, firstParticipantAR)
  }
}

@AllNightPreviews
@Composable
private fun CallParticipantsPagerPreview() {
  Previews.Preview {
    val participants = remember {
      (1..5).map {
        CallParticipant(
          callParticipantId = CallParticipantId(0, RecipientId.from(it.toLong())),
          recipient = Recipient(
            isResolving = false,
            chatColorsValue = ChatColorsPalette.UNKNOWN_CONTACT
          )
        )
      }
    }

    val state = remember {
      CallParticipantsPagerState(
        callParticipants = participants,
        focusedParticipant = participants.first(),
        isRenderInPip = false,
        hideAvatar = false
      )
    }

    Surface(
      modifier = Modifier.fillMaxSize()
    ) {
      CallGrid(
        items = state.callParticipants,
        modifier = Modifier.fillMaxSize(),
        itemKey = { it.callParticipantId }
      ) { participant, itemModifier ->
        RemoteParticipantContent(
          participant = participant,
          renderInPip = state.isRenderInPip,
          raiseHandAllowed = false,
          onInfoMoreInfoClick = null,
          modifier = itemModifier
        )
      }
    }
  }
}

@Immutable
data class CallParticipantsPagerState(
  val callParticipants: List<CallParticipant> = emptyList(),
  val focusedParticipant: CallParticipant? = null,
  val isRenderInPip: Boolean = false,
  val hideAvatar: Boolean = false
)

/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.v2

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.LinearLayoutManager
import io.zonarosa.messenger.components.recyclerview.NoTouchingRecyclerView
import io.zonarosa.messenger.components.webrtc.WebRtcReactionsAlphaItemDecoration
import io.zonarosa.messenger.components.webrtc.WebRtcReactionsItemAnimator
import io.zonarosa.messenger.components.webrtc.WebRtcReactionsRecyclerAdapter
import io.zonarosa.messenger.events.GroupCallReactionEvent

/**
 * Displays a list of reactions sent during a group call.
 *
 * Due to how LazyColumn deals with touch events and how Column doesn't have proper
 * per-item animation support, we utilize a recycler view as we do in the old call
 * screen.
 */
@Composable
fun CallScreenReactionsContainer(
  reactions: List<GroupCallReactionEvent>,
  modifier: Modifier = Modifier
) {
  val adapter = remember { WebRtcReactionsRecyclerAdapter() }
  AndroidView(factory = {
    val view = NoTouchingRecyclerView(it)
    view.layoutManager = LinearLayoutManager(it, LinearLayoutManager.VERTICAL, true)
    view.adapter = adapter
    view.addItemDecoration(WebRtcReactionsAlphaItemDecoration())
    view.itemAnimator = WebRtcReactionsItemAnimator()
    view.isClickable = false
    view.isVerticalScrollBarEnabled = false

    view
  }, modifier = modifier.padding(16.dp)) {
    adapter.submitList(reactions.toMutableList())
  }
}

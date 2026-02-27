/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentActivity
import io.zonarosa.core.ui.compose.theme.ZonaRosaTheme
import io.zonarosa.core.util.getParcelableExtraCompat
import io.zonarosa.messenger.calls.links.EditCallLinkNameDialogFragment
import io.zonarosa.messenger.main.MainNavigationDetailLocation
import io.zonarosa.messenger.main.MainNavigationListLocation
import io.zonarosa.messenger.main.MainNavigationRouter
import io.zonarosa.messenger.service.webrtc.links.CallLinkRoomId
import io.zonarosa.messenger.util.viewModel

class CallLinkDetailsActivity : FragmentActivity() {

  companion object {
    private const val ARG_ROOM_ID = "room.id"

    fun createIntent(context: Context, callLinkRoomId: CallLinkRoomId): Intent {
      return Intent(context, CallLinkDetailsActivity::class.java)
        .putExtra(ARG_ROOM_ID, callLinkRoomId)
    }
  }

  private val roomId: CallLinkRoomId
    get() = intent.getParcelableExtraCompat(ARG_ROOM_ID, CallLinkRoomId::class.java)!!

  private val viewModel: CallLinkDetailsViewModel by viewModel {
    CallLinkDetailsViewModel(roomId)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()

    super.onCreate(savedInstanceState)

    setContent {
      ZonaRosaTheme {
        CallLinkDetailsScreen(
          roomId = roomId,
          viewModel = viewModel,
          router = remember { Router() }
        )
      }
    }
  }

  private inner class Router : MainNavigationRouter {
    override fun goTo(location: MainNavigationDetailLocation) {
      when (location) {
        is MainNavigationDetailLocation.Calls.CallLinks.EditCallLinkName -> {
          EditCallLinkNameDialogFragment().apply {
            arguments = bundleOf(EditCallLinkNameDialogFragment.ARG_NAME to viewModel.nameSnapshot)
          }.show(supportFragmentManager, null)
        }

        is MainNavigationDetailLocation.Empty -> {
          finishAfterTransition()
        }

        else -> error("Unsupported route $location")
      }
    }

    override fun goTo(location: MainNavigationListLocation) = Unit
  }
}

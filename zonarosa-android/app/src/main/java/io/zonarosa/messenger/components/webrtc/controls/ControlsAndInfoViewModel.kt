/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.webrtc.controls

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.zonarosa.messenger.calls.links.CallLinks
import io.zonarosa.messenger.calls.links.UpdateCallLinkRepository
import io.zonarosa.messenger.calls.links.details.CallLinkDetailsRepository
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.UpdateCallLinkResult

/**
 * Provides a view model communicating with the Controls and Info view, [CallInfoView].
 */
class ControlsAndInfoViewModel(
  private val repository: CallLinkDetailsRepository = CallLinkDetailsRepository(),
  private val mutationRepository: UpdateCallLinkRepository = UpdateCallLinkRepository()
) : ViewModel() {
  private val disposables = CompositeDisposable()
  private var callRecipientId: RecipientId? = null
  private val _state = mutableStateOf(ControlAndInfoState())
  val state: State<ControlAndInfoState> = _state

  val rootKeySnapshot: ByteArray
    get() = state.value.callLink?.credentials?.linkKeyBytes ?: error("Call link not loaded yet.")

  fun setRecipient(recipient: Recipient) {
    if (recipient.isCallLink && callRecipientId != recipient.id) {
      callRecipientId = recipient.id
      disposables += repository.refreshCallLinkState(recipient.requireCallLinkRoomId())
      disposables += CallLinks.watchCallLink(recipient.requireCallLinkRoomId()).subscribeBy {
        _state.value = _state.value.copy(callLink = it)
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    disposables.dispose()
  }

  fun resetScrollState() {
    _state.value = _state.value.copy(resetScrollState = System.currentTimeMillis())
  }

  fun setName(name: String): Single<UpdateCallLinkResult> {
    val credentials = _state.value.callLink?.credentials ?: error("User cannot change the name of this call.")
    return mutationRepository.setCallName(credentials, name)
  }

  class Factory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return modelClass.cast(ControlsAndInfoViewModel()) as T
    }
  }
}

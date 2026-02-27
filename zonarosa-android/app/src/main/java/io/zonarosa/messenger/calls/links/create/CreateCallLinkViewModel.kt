/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links.create

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import io.zonarosa.ringrtc.CallLinkState.Restrictions
import io.zonarosa.messenger.calls.links.CallLinks
import io.zonarosa.messenger.calls.links.UpdateCallLinkRepository
import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.service.webrtc.links.CallLinkCredentials
import io.zonarosa.messenger.service.webrtc.links.ZonaRosaCallLinkState
import io.zonarosa.messenger.service.webrtc.links.UpdateCallLinkResult
import java.time.Instant

class CreateCallLinkViewModel(
  private val repository: CreateCallLinkRepository = CreateCallLinkRepository(),
  private val mutationRepository: UpdateCallLinkRepository = UpdateCallLinkRepository()
) : ViewModel() {
  private val initialCredentials = CallLinkCredentials.generate()
  private val _callLink: MutableState<CallLinkTable.CallLink> = mutableStateOf(
    CallLinkTable.CallLink(
      recipientId = RecipientId.UNKNOWN,
      roomId = initialCredentials.roomId,
      credentials = initialCredentials,
      state = ZonaRosaCallLinkState(
        name = "",
        restrictions = Restrictions.ADMIN_APPROVAL,
        revoked = false,
        expiration = Instant.MAX
      ),
      deletionTimestamp = 0L
    )
  )

  val callLink: State<CallLinkTable.CallLink> = _callLink

  val linkKeyBytes: ByteArray
    get() = callLink.value.credentials!!.linkKeyBytes

  private val internalShowAlreadyInACall = MutableStateFlow(false)
  val showAlreadyInACall: StateFlow<Boolean> = internalShowAlreadyInACall

  private val internalIsLoadingAdminApprovalChange = MutableStateFlow(false)
  val isLoadingAdminApprovalChange: StateFlow<Boolean> = internalIsLoadingAdminApprovalChange

  private val disposables = CompositeDisposable()

  init {
    disposables += CallLinks.watchCallLink(initialCredentials.roomId)
      .subscribeBy {
        _callLink.value = it
      }
  }

  override fun onCleared() {
    super.onCleared()
    disposables.dispose()
  }

  fun setShowAlreadyInACall(showAlreadyInACall: Boolean) {
    internalShowAlreadyInACall.update { showAlreadyInACall }
  }

  fun commitCallLink(): Single<EnsureCallLinkCreatedResult> {
    return repository.ensureCallLinkCreated(initialCredentials)
      .observeOn(AndroidSchedulers.mainThread())
  }

  fun setApproveAllMembers(approveAllMembers: Boolean): Single<UpdateCallLinkResult> {
    return commitCallLink()
      .flatMap {
        when (it) {
          is EnsureCallLinkCreatedResult.Success -> mutationRepository.setCallRestrictions(
            callLink.value.credentials!!,
            if (approveAllMembers) Restrictions.ADMIN_APPROVAL else Restrictions.NONE
          )
          is EnsureCallLinkCreatedResult.Failure -> Single.just(UpdateCallLinkResult.Failure(it.failure.status))
        }
      }
      .observeOn(AndroidSchedulers.mainThread())
      .doOnSubscribe {
        internalIsLoadingAdminApprovalChange.update { true }
      }
      .doFinally {
        internalIsLoadingAdminApprovalChange.update { false }
      }
  }

  fun setCallName(callName: String): Single<UpdateCallLinkResult> {
    return commitCallLink()
      .flatMap {
        when (it) {
          is EnsureCallLinkCreatedResult.Success -> mutationRepository.setCallName(
            callLink.value.credentials!!,
            callName
          )
          is EnsureCallLinkCreatedResult.Failure -> Single.just(UpdateCallLinkResult.Failure(it.failure.status))
        }
      }
      .observeOn(AndroidSchedulers.mainThread())
  }
}

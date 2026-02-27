/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.nicknames

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

class ViewNoteSheetViewModel(
  recipientId: RecipientId
) : ViewModel() {
  private val internalNote = mutableStateOf("")
  val note: State<String> = internalNote

  private val recipientDisposable = Recipient.observable(recipientId)
    .map { it.note ?: "" }
    .subscribeOn(Schedulers.io())
    .observeOn(AndroidSchedulers.mainThread())
    .subscribeBy { internalNote.value = it }

  override fun onCleared() {
    recipientDisposable.dispose()
  }
}

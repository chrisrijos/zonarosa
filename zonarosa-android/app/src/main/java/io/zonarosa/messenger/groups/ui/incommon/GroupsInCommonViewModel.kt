/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.groups.ui.incommon

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import io.zonarosa.messenger.groups.GroupsInCommonRepository
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

class GroupsInCommonViewModel(
  context: Context,
  recipientId: RecipientId
) : ViewModel() {

  val groups: StateFlow<List<Recipient>> = GroupsInCommonRepository.getGroupsInCommon(context, recipientId)
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(),
      initialValue = emptyList()
    )
}

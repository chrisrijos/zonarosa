/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.groups.memberlabel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.concurrent.ZonaRosaDispatchers
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.recipients.Recipient

class MemberLabelEducationViewModel(
  private val groupId: GroupId.V2,
  private val repository: MemberLabelRepository = MemberLabelRepository.instance
) : ViewModel() {

  data class UiState(
    val selfHasLabel: Boolean = false,
    val selfCanSetLabel: Boolean = false
  )

  private val _uiState = MutableStateFlow(UiState())
  val uiState: StateFlow<UiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch(ZonaRosaDispatchers.IO) {
      val self = Recipient.self()
      val selfMemberLabel = repository.getLabel(groupId, self.id)
      val selfCanSetLabel = repository.canSetLabel(groupId, self)
      _uiState.update {
        it.copy(
          selfHasLabel = selfMemberLabel != null,
          selfCanSetLabel = selfCanSetLabel
        )
      }
    }
  }
}

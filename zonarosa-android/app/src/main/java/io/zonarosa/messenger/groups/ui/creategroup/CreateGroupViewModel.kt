/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.groups.ui.creategroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.contacts.SelectedContact
import io.zonarosa.messenger.groups.SelectionLimits
import io.zonarosa.messenger.groups.ui.creategroup.CreateGroupUiState.NavTarget
import io.zonarosa.messenger.groups.ui.creategroup.CreateGroupUiState.UserMessage
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.PhoneNumber
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.RecipientRepository
import io.zonarosa.messenger.recipients.ui.RecipientSelection
import io.zonarosa.messenger.util.RemoteConfig

class CreateGroupViewModel : ViewModel() {
  companion object {
    private val TAG = Log.tag(CreateGroupViewModel::class)
  }

  private val internalUiState = MutableStateFlow(CreateGroupUiState())
  val uiState: StateFlow<CreateGroupUiState> = internalUiState.asStateFlow()

  fun onSearchQueryChanged(query: String) {
    internalUiState.update { it.copy(searchQuery = query) }
  }

  suspend fun shouldAllowSelection(selection: RecipientSelection): Boolean = when (selection) {
    is RecipientSelection.HasId -> true
    is RecipientSelection.HasPhone -> recipientExists(selection.phone)
  }

  private suspend fun recipientExists(phone: PhoneNumber): Boolean {
    internalUiState.update { it.copy(isLookingUpRecipient = true) }

    return when (val lookupResult = RecipientRepository.lookup(phone)) {
      is RecipientRepository.PhoneLookupResult.Found -> {
        internalUiState.update { it.copy(isLookingUpRecipient = false) }
        true
      }

      is RecipientRepository.LookupResult.Failure -> {
        internalUiState.update {
          it.copy(
            isLookingUpRecipient = false,
            userMessage = UserMessage.RecipientLookupFailed(failure = lookupResult)
          )
        }
        false
      }
    }
  }

  fun onSelectionChanged(newSelections: List<SelectedContact>) {
    internalUiState.update {
      it.copy(
        searchQuery = "",
        newSelections = newSelections
      )
    }
  }

  fun selectRecipient(id: RecipientId) {
    internalUiState.update {
      it.copy(pendingRecipientSelections = it.pendingRecipientSelections + id)
    }
  }

  fun clearPendingRecipientSelections() {
    internalUiState.update {
      it.copy(pendingRecipientSelections = emptySet())
    }
  }

  fun continueToGroupDetails() {
    viewModelScope.launch {
      internalUiState.update { it.copy(isLookingUpRecipient = true) }

      val selectedRecipientIds = uiState.value.newSelections.map { it.orCreateRecipientId }
      when (val lookupResult = RecipientRepository.lookup(recipientIds = selectedRecipientIds)) {
        is RecipientRepository.IdLookupResult.FoundAll -> {
          internalUiState.update {
            it.copy(
              isLookingUpRecipient = false,
              pendingDestination = NavTarget.AddGroupDetails(recipientIds = selectedRecipientIds)
            )
          }
        }

        is RecipientRepository.LookupResult.Failure -> {
          internalUiState.update {
            it.copy(
              isLookingUpRecipient = false,
              userMessage = UserMessage.RecipientLookupFailed(failure = lookupResult)
            )
          }
        }
      }
    }
  }

  fun clearUserMessage() {
    internalUiState.update { it.copy(userMessage = null) }
  }

  fun clearPendingDestination() {
    internalUiState.update { it.copy(pendingDestination = null) }
  }
}

data class CreateGroupUiState(
  val forceSplitPane: Boolean = ZonaRosaStore.internal.forceSplitPane,
  val searchQuery: String = "",
  val selectionLimits: SelectionLimits = RemoteConfig.groupLimits.excludingSelf(),
  val newSelections: List<SelectedContact> = emptyList(),
  val isLookingUpRecipient: Boolean = false,
  val pendingRecipientSelections: Set<RecipientId> = emptySet(),
  val pendingDestination: NavTarget? = null,
  val userMessage: UserMessage? = null
) {
  sealed interface UserMessage {
    data class RecipientLookupFailed(val failure: RecipientRepository.LookupResult.Failure) : UserMessage
  }

  sealed interface NavTarget {
    data class AddGroupDetails(val recipientIds: List<RecipientId>) : NavTarget
  }
}

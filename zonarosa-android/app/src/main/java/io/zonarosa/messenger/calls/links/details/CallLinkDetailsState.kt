/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.calls.links.details

import io.zonarosa.messenger.database.CallLinkTable
import io.zonarosa.messenger.service.webrtc.CallLinkPeekInfo

data class CallLinkDetailsState(
  val displayRevocationDialog: Boolean = false,
  val isLoadingAdminApprovalChange: Boolean = false,
  val callLink: CallLinkTable.CallLink? = null,
  val peekInfo: CallLinkPeekInfo? = null,
  val failureSnackbar: FailureSnackbar? = null
) {
  enum class FailureSnackbar {
    COULD_NOT_DELETE_CALL_LINK,
    COULD_NOT_SAVE_CHANGES,
    COULD_NOT_UPDATE_ADMIN_APPROVAL
  }
}

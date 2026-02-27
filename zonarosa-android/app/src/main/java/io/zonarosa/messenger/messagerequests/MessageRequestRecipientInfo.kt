/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */
package io.zonarosa.messenger.messagerequests

import io.zonarosa.messenger.recipients.Recipient

/**
 * Thread recipient and message request state information necessary to render
 * a thread header.
 */
data class MessageRequestRecipientInfo(
  val recipient: Recipient,
  val groupInfo: GroupInfo = GroupInfo.ZERO,
  val sharedGroups: List<String> = emptyList(),
  val messageRequestState: MessageRequestState? = null
)

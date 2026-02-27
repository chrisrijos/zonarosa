/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database.model

import io.zonarosa.messenger.attachments.DatabaseAttachment
import io.zonarosa.messenger.database.CallTable
import io.zonarosa.messenger.payments.Payment
import io.zonarosa.messenger.polls.PollRecord

fun MessageRecord.withReactions(reactions: List<ReactionRecord>): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withReactions(reactions)
  } else {
    this
  }
}

fun MessageRecord.withAttachments(attachments: List<DatabaseAttachment>): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withAttachments(attachments)
  } else {
    this
  }
}
fun MessageRecord.withPayment(payment: Payment): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withPayment(payment)
  } else {
    this
  }
}

fun MessageRecord.withCall(call: CallTable.Call): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withCall(call)
  } else {
    this
  }
}

fun MessageRecord.withPoll(poll: PollRecord): MessageRecord {
  return if (this is MmsMessageRecord) {
    this.withPoll(poll)
  } else {
    this
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.clicklisteners

import android.view.View
import io.zonarosa.core.util.concurrent.ZonaRosaExecutors
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.database.model.MessageRecord
import io.zonarosa.messenger.mms.Slide
import io.zonarosa.messenger.mms.SlidesClickedListener
import io.zonarosa.messenger.sms.MessageSender

class ResendClickListener(private val messageRecord: MessageRecord) : SlidesClickedListener {
  override fun onClick(v: View?, slides: MutableList<Slide>?) {
    if (v == null) {
      Log.w(TAG, "Could not resend message, view was null!")
      return
    }

    ZonaRosaExecutors.BOUNDED.execute {
      MessageSender.resend(v.context, messageRecord)
    }
  }

  companion object {
    private val TAG = Log.tag(ResendClickListener::class.java)
  }
}

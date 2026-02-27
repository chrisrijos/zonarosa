/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.registration.sample.fcm

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.zonarosa.core.util.logging.Log

/**
 * Firebase Cloud Messaging service for receiving push notifications.
 * During registration, this is used to receive push challenge tokens from the server.
 */
class FcmReceiveService : FirebaseMessagingService() {

  companion object {
    private val TAG = Log.tag(FcmReceiveService::class)
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    Log.d(TAG, "onMessageReceived: ${remoteMessage.messageId}")

    val challenge = remoteMessage.data["challenge"]
    if (challenge != null) {
      Log.d(TAG, "Received push challenge")
      PushChallengeReceiver.onChallengeReceived(challenge)
    }
  }

  override fun onNewToken(token: String) {
    Log.d(TAG, "onNewToken")
  }
}

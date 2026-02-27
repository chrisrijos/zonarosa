//
// Copyright 2025 ZonaRosa Platform.
// SPDX-License-Identifier: MIT-3.0-only
//

package io.zonarosa.libzonarosa.net

open class NoOpListener : ChatConnectionListener {
  override fun onIncomingMessage(
    chat: ChatConnection,
    envelope: ByteArray,
    serverDeliveryTimestamp: Long,
    sendAck: ChatConnectionListener.ServerMessageAck,
  ) {}
}

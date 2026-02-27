/**
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc.links

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import okio.ByteString
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Hex
import io.zonarosa.core.util.Serializer
import io.zonarosa.ringrtc.CallLinkRootKey

@Serializable
@Parcelize
class CallLinkRoomId private constructor(private val roomId: ByteArray) : Parcelable {
  fun serialize(): String = DatabaseSerializer.serialize(this)

  fun encodeForProto(): ByteString = roomId.toByteString()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CallLinkRoomId

    if (!roomId.contentEquals(other.roomId)) return false

    return true
  }

  override fun hashCode(): Int {
    return roomId.contentHashCode()
  }

  /**
   * Prints call link room id as a hex string, explicitly for logging.
   */
  override fun toString(): String {
    return Hex.toStringCondensed(roomId)
  }

  object DatabaseSerializer : Serializer<CallLinkRoomId, String> {
    override fun serialize(data: CallLinkRoomId): String {
      return Base64.encodeWithPadding(data.roomId)
    }

    override fun deserialize(data: String): CallLinkRoomId {
      return fromBytes(Base64.decode(data))
    }
  }

  companion object {
    @JvmStatic
    fun fromBytes(byteArray: ByteArray): CallLinkRoomId {
      return CallLinkRoomId(byteArray)
    }

    fun fromCallLinkRootKey(callLinkRootKey: CallLinkRootKey): CallLinkRoomId {
      return CallLinkRoomId(callLinkRootKey.deriveRoomId())
    }
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.conversation.colors

import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.CryptoUtil
import io.zonarosa.messenger.groups.GroupId

/**
 * Stolen from iOS. Utilizes a simple hash to map different characteristics to an avatar color index.
 */
object AvatarColorHash {

  /**
   * Utilize Uppercase UUID of ServiceId.
   *
   * Uppercase is necessary here because iOS utilizes uppercase UUIDs by default.
   */
  fun forAddress(serviceId: ServiceId?, e164: String?): AvatarColor {
    if (serviceId != null) {
      return forData(serviceId.toByteArray())
    }

    if (!e164.isNullOrEmpty()) {
      return forData(e164.toByteArray(Charsets.UTF_8))
    }

    return AvatarColor.A100
  }

  fun forGroupId(group: GroupId): AvatarColor {
    return forData(group.decodedId)
  }

  @JvmStatic
  fun forCallLink(rootKey: ByteArray): AvatarColor {
    return forIndex(rootKey.first().toInt())
  }

  private fun forData(data: ByteArray): AvatarColor {
    val hash = CryptoUtil.sha256(data)
    val firstByte: Byte = hash[0]
    return forIndex(firstByte.toInt())
  }

  private fun forIndex(index: Int): AvatarColor {
    return AvatarColor.RANDOM_OPTIONS[(index.toUInt() % AvatarColor.RANDOM_OPTIONS.size.toUInt()).toInt()]
  }
}

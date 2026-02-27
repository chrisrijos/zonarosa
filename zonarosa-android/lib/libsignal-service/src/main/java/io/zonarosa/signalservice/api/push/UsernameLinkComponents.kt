/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.push

import java.util.UUID

/**
 * Wrapper for passing around the two components of a username link: the entropy and serverId, which when combined together form the full link handle.
 */
data class UsernameLinkComponents(
  val entropy: ByteArray,
  val serverId: UUID
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as UsernameLinkComponents

    if (!entropy.contentEquals(other.entropy)) return false
    return serverId == other.serverId
  }

  override fun hashCode(): Int {
    var result = entropy.contentHashCode()
    result = 31 * result + serverId.hashCode()
    return result
  }
}

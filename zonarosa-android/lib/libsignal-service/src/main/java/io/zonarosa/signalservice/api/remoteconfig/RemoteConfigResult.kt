/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.remoteconfig

data class RemoteConfigResult(
  val config: Map<String, Any>,
  val serverEpochTimeMilliseconds: Long,
  val eTag: String? = ""
)

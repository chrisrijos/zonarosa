/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Represents the response when fetching the archive backup info.
 */
data class ArchiveGetBackupInfoResponse(
  @JsonProperty
  val cdn: Int?,
  @JsonProperty
  val backupDir: String?,
  @JsonProperty
  val mediaDir: String?,
  @JsonProperty
  val backupName: String?,
  @JsonProperty
  val usedSpace: Long?
)

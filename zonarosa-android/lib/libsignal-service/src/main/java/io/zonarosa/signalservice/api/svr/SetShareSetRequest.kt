/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.svr

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.zonarosa.service.internal.push.ByteArraySerializerBase64NoPadding

/**
 * Request body for setting a share-set on the service.
 */
class SetShareSetRequest(
  @JsonProperty
  @JsonSerialize(using = ByteArraySerializerBase64NoPadding::class)
  val shareSet: ByteArray
)

/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.database

import kotlinx.serialization.json.Json
import io.zonarosa.core.models.media.TransformProperties
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.mms.SentMediaQuality
import io.zonarosa.service.internal.util.JsonUtil
import java.io.IOException
import java.util.Optional

private val TAG = Log.tag(TransformProperties::class.java)

/**
 * Serializes the TransformProperties to a JSON string using Jackson.
 */
fun TransformProperties.serialize(): String {
  return Json.encodeToString(this)
}

/**
 * Parses a JSON string to create a TransformProperties instance.
 */
fun parseTransformProperties(serialized: String?): TransformProperties {
  return if (serialized == null) {
    TransformProperties.empty()
  } else {
    try {
      JsonUtil.fromJson(serialized, TransformProperties::class.java)
    } catch (e: IOException) {
      Log.w(TAG, "Failed to parse TransformProperties!", e)
      TransformProperties.empty()
    }
  }
}

/**
 * Creates TransformProperties for the given media quality, preserving existing properties.
 */
fun transformPropertiesForSentMediaQuality(currentProperties: Optional<TransformProperties>, sentMediaQuality: SentMediaQuality): TransformProperties {
  val existing = currentProperties.orElse(TransformProperties.empty())
  return existing.copy(sentMediaQuality = sentMediaQuality.code)
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.registration.data.network

import io.zonarosa.core.util.logging.Log

enum class Challenge(val key: String) {
  CAPTCHA("captcha"),
  PUSH("pushChallenge");

  companion object {
    private val TAG = Log.tag(Challenge::class)

    fun parse(strings: List<String>): List<Challenge> {
      return strings.mapNotNull {
        when (it) {
          CAPTCHA.key -> CAPTCHA
          PUSH.key -> PUSH
          else -> {
            Log.i(TAG, "Encountered unknown challenge type: $it")
            null
          }
        }
      }
    }
  }
}

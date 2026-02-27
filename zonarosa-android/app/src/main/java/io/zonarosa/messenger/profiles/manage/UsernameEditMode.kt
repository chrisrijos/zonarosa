/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.profiles.manage

enum class UsernameEditMode {
  /** A typical launch, no special conditions. */
  NORMAL,

  /** Screen was launched because the username was in a bad state and needs to be recovered. Shows a special dialog. */
  RECOVERY
}

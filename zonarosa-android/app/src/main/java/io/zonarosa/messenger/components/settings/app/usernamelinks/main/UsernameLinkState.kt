/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.app.usernamelinks.main

sealed class UsernameLinkState {

  /** Link is set. */
  data class Present(val link: String) : UsernameLinkState()

  /** Link has not been set yet or otherwise does not exist. */
  object NotSet : UsernameLinkState()

  /** Link is in the process of being reset. */
  object Resetting : UsernameLinkState()
}

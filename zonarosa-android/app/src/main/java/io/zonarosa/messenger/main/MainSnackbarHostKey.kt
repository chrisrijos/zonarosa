/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.main

import io.zonarosa.messenger.components.snackbars.SnackbarHostKey

sealed interface MainSnackbarHostKey : SnackbarHostKey {
  data object Chat : MainSnackbarHostKey
  data object MainChrome : MainSnackbarHostKey
}

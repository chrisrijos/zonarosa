/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.snackbars

/**
 * Marker interface for identifying snackbar host locations.
 *
 * Implement this interface to define distinct snackbar display locations within the app.
 * When a [SnackbarState] is emitted, its [SnackbarState.hostKey] is used to route the
 * snackbar to the appropriate registered consumer.
 */
interface SnackbarHostKey {
  object Global : SnackbarHostKey
}

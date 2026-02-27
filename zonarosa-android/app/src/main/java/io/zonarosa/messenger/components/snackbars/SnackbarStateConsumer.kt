/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.snackbars

/**
 * A consumer that can display snackbar messages.
 *
 * Implementations are typically UI components that host a snackbar display area.
 */
fun interface SnackbarStateConsumer {
  /**
   * Consumes the given snackbar state.
   *
   * @param snackbarState The snackbar to display.
   */
  fun consume(snackbarState: SnackbarState)
}

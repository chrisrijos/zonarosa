/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.link

/**
 * Error response options chosen by a user. Response is sent to a linked device after its transfer archive has failed
 */
enum class TransferArchiveError {
  RELINK_REQUESTED,
  CONTINUE_WITHOUT_UPLOAD
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2.ui.status

sealed interface ArchiveUploadStatusBannerViewEvents {
  data object BannerClicked : ArchiveUploadStatusBannerViewEvents
  data object CancelClicked : ArchiveUploadStatusBannerViewEvents
  data object HideClicked : ArchiveUploadStatusBannerViewEvents
}

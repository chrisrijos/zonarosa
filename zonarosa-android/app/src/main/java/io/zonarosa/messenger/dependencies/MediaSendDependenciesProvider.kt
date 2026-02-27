/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.dependencies

import io.zonarosa.mediasend.MediaSendDependencies
import io.zonarosa.mediasend.MediaSendRepository
import io.zonarosa.mediasend.preupload.PreUploadRepository
import io.zonarosa.messenger.mediasend.v3.MediaSendV3PreUploadRepository
import io.zonarosa.messenger.mediasend.v3.MediaSendV3Repository

object MediaSendDependenciesProvider : MediaSendDependencies.Provider {
  override fun provideMediaSendRepository(): MediaSendRepository = MediaSendV3Repository

  override fun providePreUploadRepository(): PreUploadRepository = MediaSendV3PreUploadRepository
}

/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.mediasend

import android.app.Application
import io.zonarosa.mediasend.preupload.PreUploadRepository

/**
 * MediaSend Feature Module dependencies
 */
object MediaSendDependencies {
  private lateinit var _application: Application
  private lateinit var _provider: Provider

  @Synchronized
  fun init(application: Application, provider: Provider) {
    if (this::_application.isInitialized || this::_provider.isInitialized) {
      return
    }

    _application = application
    _provider = provider
  }

  val application
    get() = _application

  val preUploadRepository: PreUploadRepository
    get() = _provider.providePreUploadRepository()

  val mediaSendRepository: MediaSendRepository
    get() = _provider.provideMediaSendRepository()

  interface Provider {
    fun provideMediaSendRepository(): MediaSendRepository
    fun providePreUploadRepository(): PreUploadRepository
  }
}

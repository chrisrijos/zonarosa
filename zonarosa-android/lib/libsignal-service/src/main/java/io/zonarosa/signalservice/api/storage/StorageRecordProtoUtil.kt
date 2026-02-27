/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

@file:JvmName("StorageRecordProtoUtil")

package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.AccountRecord

/**
 * Provide helpers for various Storage Service protos.
 */
object StorageRecordProtoUtil {

  /** Must match tag value specified for ManifestRecord.Identifier#type in StorageService.proto */
  const val STORAGE_ID_TYPE_TAG = 2

  @JvmStatic
  val defaultAccountRecord by lazy { AccountRecord() }
}

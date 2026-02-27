/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import io.zonarosa.service.internal.storage.protos.CallLinkRecord
import java.io.IOException

/**
 * Wrapper around a [CallLinkRecord] to pair it with a [StorageId].
 */
data class ZonaRosaCallLinkRecord(
  override val id: StorageId,
  override val proto: CallLinkRecord
) : ZonaRosaRecord<CallLinkRecord> {

  companion object {
    fun newBuilder(serializedUnknowns: ByteArray?): CallLinkRecord.Builder {
      return serializedUnknowns?.let { builderFromUnknowns(it) } ?: CallLinkRecord.Builder()
    }

    private fun builderFromUnknowns(serializedUnknowns: ByteArray): CallLinkRecord.Builder {
      return try {
        CallLinkRecord.ADAPTER.decode(serializedUnknowns).newBuilder()
      } catch (e: IOException) {
        CallLinkRecord.Builder()
      }
    }
  }
}

/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.storage

import com.squareup.wire.FieldEncoding
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.util.getUnknownEnumValue
import io.zonarosa.service.internal.storage.protos.ManifestRecord

/**
 * Wire makes it harder to write specific values to proto enums, since they use actual enums under the hood.
 * This method handles creating an identifier from a possibly-unknown enum type, writing an unknown field if
 * necessary to preserve the specific value.
 */
fun ManifestRecord.Identifier.Companion.fromPossiblyUnknownType(typeInt: Int, rawId: ByteArray): ManifestRecord.Identifier {
  val builder = ManifestRecord.Identifier.Builder()
  builder.raw = rawId.toByteString()

  val type = ManifestRecord.Identifier.Type.fromValue(typeInt)
  if (type != null) {
    builder.type = type
  } else {
    builder.type = ManifestRecord.Identifier.Type.UNKNOWN
    builder.addUnknownField(StorageRecordProtoUtil.STORAGE_ID_TYPE_TAG, FieldEncoding.VARINT, typeInt)
  }

  return builder.build()
}

/**
 * Wire makes it harder to read the underlying int value of an unknown enum.
 * This value represents the _true_ int value of the enum, even if it is not one of the known values.
 */
val ManifestRecord.Identifier.typeValue: Int
  get() {
    return if (this.type != ManifestRecord.Identifier.Type.UNKNOWN) {
      this.type.value
    } else {
      this.getUnknownEnumValue(StorageRecordProtoUtil.STORAGE_ID_TYPE_TAG)
    }
  }

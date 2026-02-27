/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.service.api.archive

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import io.zonarosa.core.util.Base64
import io.zonarosa.libzonarosa.zkgroup.backups.BackupAuthCredentialRequest

/**
 * Represents the request body when setting the archive backupId.
 */
class ArchiveSetBackupIdRequest(
  @JsonProperty
  @JsonSerialize(using = BackupAuthCredentialRequestSerializer::class)
  val messagesBackupAuthCredentialRequest: BackupAuthCredentialRequest?,
  @JsonProperty
  @JsonSerialize(using = BackupAuthCredentialRequestSerializer::class)
  val mediaBackupAuthCredentialRequest: BackupAuthCredentialRequest?
) {
  class BackupAuthCredentialRequestSerializer : JsonSerializer<BackupAuthCredentialRequest>() {
    override fun serialize(value: BackupAuthCredentialRequest, gen: JsonGenerator, serializers: SerializerProvider) {
      gen.writeString(Base64.encodeWithPadding(value.serialize()))
    }
  }
}

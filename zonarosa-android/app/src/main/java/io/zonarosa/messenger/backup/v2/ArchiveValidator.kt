/*
 * Copyright 2024 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.backup.v2

import io.zonarosa.core.models.backup.MessageBackupKey
import io.zonarosa.core.util.isNotNullOrBlank
import io.zonarosa.libzonarosa.messagebackup.BackupForwardSecrecyToken
import io.zonarosa.libzonarosa.messagebackup.MessageBackup
import io.zonarosa.libzonarosa.messagebackup.ValidationError
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.util.isStory
import io.zonarosa.messenger.util.isStoryReaction
import java.io.File
import java.io.IOException
import io.zonarosa.libzonarosa.messagebackup.BackupKey as LibZonaRosaBackupKey
import io.zonarosa.libzonarosa.messagebackup.MessageBackupKey as LibZonaRosaMessageBackupKey

object ArchiveValidator {

  fun validateZonaRosaBackup(backupFile: File, backupKey: MessageBackupKey, backupForwardSecrecyToken: BackupForwardSecrecyToken): ValidationResult {
    return validate(backupFile, backupKey, backupForwardSecrecyToken, forTransfer = false)
  }

  fun validateLocalOrLinking(backupFile: File, backupKey: MessageBackupKey, forTransfer: Boolean): ValidationResult {
    return validate(backupFile, backupKey, forwardSecrecyToken = null, forTransfer)
  }

  /**
   * Validates the provided [backupFile] that is encrypted with the provided [backupKey].
   */
  fun validate(backupFile: File, backupKey: MessageBackupKey, forwardSecrecyToken: BackupForwardSecrecyToken?, forTransfer: Boolean): ValidationResult {
    return try {
      val backupId = backupKey.deriveBackupId(ZonaRosaStore.account.requireAci())
      val libZonaRosaBackupKey = LibZonaRosaBackupKey(backupKey.value)
      val libZonaRosaMessageBackupKey = LibZonaRosaMessageBackupKey(libZonaRosaBackupKey, backupId.value, forwardSecrecyToken)

      MessageBackup.validate(
        libZonaRosaMessageBackupKey,
        if (forTransfer) MessageBackup.Purpose.DEVICE_TRANSFER else MessageBackup.Purpose.REMOTE_BACKUP,
        { backupFile.inputStream() },
        backupFile.length()
      )

      ValidationResult.Success
    } catch (e: IOException) {
      ValidationResult.ReadError(e)
    } catch (e: ValidationError) {
      if (e.message?.contains("have the same phone number") == true) {
        val recipientIds = """RecipientId\((\d+)\)""".toRegex()
          .findAll(e.message ?: "")
          .map { it.groupValues[1] }
          .mapNotNull { it.toLongOrNull() }
          .map { RecipientId.from(it) }
          .toList()

        val recipientIdA = recipientIds.getOrNull(0)
        val recipientIdB = recipientIds.getOrNull(1)

        val e164A = recipientIdA?.let { Recipient.resolved(it).e164.orElse("UNKNOWN") }.let { "KEEP_E164::$it" }
        val e164B = recipientIdB?.let { Recipient.resolved(it).e164.orElse("UNKNOWN") }.let { "KEEP_E164::$it" }

        ValidationResult.RecipientDuplicateE164Error(
          exception = e,
          details = DuplicateRecipientDetails(
            recipientIdA = recipientIds.getOrNull(0),
            recipientIdB = recipientIds.getOrNull(1),
            e164A = e164A,
            e164B = e164B
          )
        )
      } else {
        val sentTimestamp = "\\d{10,}+".toRegex().find(e.message ?: "")?.value?.toLongOrNull()
        ValidationResult.MessageValidationError(
          exception = e,
          messageDetails = sentTimestamp?.let { fetchMessageDetails(it) } ?: emptyList()
        )
      }
    }
  }

  private fun fetchMessageDetails(sentTimestamp: Long): List<MessageDetails> {
    val messages = ZonaRosaDatabase.messages.getMessagesBySentTimestamp(sentTimestamp)
    return messages.map {
      MessageDetails(
        messageId = it.id,
        dateSent = it.dateSent,
        threadId = it.threadId,
        threadRecipientId = ZonaRosaDatabase.threads.getRecipientForThreadId(it.threadId)?.id?.toLong() ?: 0L,
        type = it.type,
        fromRecipientId = it.fromRecipient.id.toLong(),
        toRecipientId = it.toRecipient.id.toLong(),
        hasBody = it.body.isNotNullOrBlank(),
        hasExtras = it.messageExtras != null,
        outgoing = it.isOutgoing,
        viewOnce = it.isViewOnce,
        isStory = it.isStory(),
        isStoryReaction = it.isStoryReaction(),
        originalMessageId = it.originalMessageId?.id ?: 0,
        isLatestRevision = it.isLatestRevision
      )
    }
  }

  sealed interface ValidationResult {
    data object Success : ValidationResult
    data class ReadError(val exception: IOException) : ValidationResult
    data class MessageValidationError(
      val exception: ValidationError,
      val messageDetails: List<MessageDetails>
    ) : ValidationResult
    data class RecipientDuplicateE164Error(
      val exception: ValidationError,
      val details: DuplicateRecipientDetails
    ) : ValidationResult
  }

  data class MessageDetails(
    val messageId: Long,
    val dateSent: Long,
    val threadId: Long,
    val threadRecipientId: Long,
    val type: Long,
    val fromRecipientId: Long,
    val toRecipientId: Long,
    val hasBody: Boolean,
    val hasExtras: Boolean,
    val outgoing: Boolean,
    val viewOnce: Boolean,
    val isStory: Boolean,
    val isStoryReaction: Boolean,
    val originalMessageId: Long,
    val isLatestRevision: Boolean
  )

  data class DuplicateRecipientDetails(
    val recipientIdA: RecipientId?,
    val recipientIdB: RecipientId?,
    val e164A: String?,
    val e164B: String?
  )
}

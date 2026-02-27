/*
 * Copyright 2025 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.components.settings.conversation

import androidx.annotation.WorkerThread
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.AnnotatedString
import io.zonarosa.core.util.Base64
import io.zonarosa.core.util.Hex
import io.zonarosa.messenger.components.settings.app.subscription.InAppPaymentsRepository
import io.zonarosa.messenger.database.ZonaRosaDatabase
import io.zonarosa.messenger.database.model.InAppPaymentSubscriberRecord
import io.zonarosa.messenger.database.model.RecipientRecord
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId

@Immutable
data class InternalConversationSettingsState(
  val recipientId: RecipientId,
  val isGroup: Boolean,
  val e164: String,
  val aci: String,
  val pni: String,
  val groupId: GroupId?,
  val threadId: Long?,
  val profileName: String,
  val profileKeyBase64: String,
  val profileKeyHex: String,
  val sealedSenderAccessMode: String,
  val phoneNumberSharing: String,
  val phoneNumberDiscoverability: String,
  val profileSharing: String,
  val capabilities: AnnotatedString,
  val hasServiceId: Boolean,
  val isSelf: Boolean,
  val subscriberId: String
) {

  val groupIdString = groupId?.toString()
  val threadIdString = threadId?.toString() ?: "N/A"

  companion object {
    @WorkerThread
    fun create(recipient: Recipient, threadId: Long?, groupId: GroupId?): InternalConversationSettingsState {
      return InternalConversationSettingsState(
        recipientId = recipient.id,
        isGroup = recipient.isGroup,
        e164 = recipient.e164.orElse("null"),
        aci = recipient.aci.map { it.toString() }.orElse("null"),
        pni = recipient.pni.map { it.toString() }.orElse("null"),
        groupId = groupId,
        threadId = threadId,
        profileName = with(recipient) {
          if (isGroup) "" else "[${profileName.givenName}] [${profileName.familyName}]"
        },
        profileKeyBase64 = with(recipient) {
          if (isGroup) "" else profileKey?.let(Base64::encodeWithPadding) ?: "None"
        },
        profileKeyHex = with(recipient) {
          if (isGroup) "" else profileKey?.let(Hex::toStringCondensed) ?: "None"
        },
        sealedSenderAccessMode = recipient.sealedSenderAccessMode.toString(),
        phoneNumberSharing = recipient.phoneNumberSharing.name,
        phoneNumberDiscoverability = ZonaRosaDatabase.recipients.getPhoneNumberDiscoverability(recipient.id)?.name ?: "null",
        profileSharing = recipient.isProfileSharing.toString(),
        capabilities = buildCapabilities(recipient),
        hasServiceId = recipient.hasServiceId,
        isSelf = recipient.isSelf,
        subscriberId = buildSubscriberId(recipient)
      )
    }

    @WorkerThread
    private fun buildSubscriberId(recipient: Recipient): String {
      return if (recipient.isSelf) {
        val subscriber: InAppPaymentSubscriberRecord? = InAppPaymentsRepository.getSubscriber(InAppPaymentSubscriberRecord.Type.DONATION)
        if (subscriber != null) {
          """currency code: ${subscriber.currency!!.currencyCode}
            |subscriber id: ${subscriber.subscriberId.serialize()}
          """.trimMargin()
        } else {
          "None"
        }
      } else {
        "None"
      }
    }

    @WorkerThread
    private fun buildCapabilities(recipient: Recipient): AnnotatedString {
      return if (recipient.isGroup) {
        AnnotatedString("null")
      } else {
        val capabilities: RecipientRecord.Capabilities? = ZonaRosaDatabase.recipients.getCapabilities(recipient.id)
        if (capabilities != null) {
          AnnotatedString("No capabilities right now.")
          // Left as an example in case we add one in the future
//          val style: SpanStyle = when (capabilities.storageServiceEncryptionV2) {
//            Recipient.Capability.SUPPORTED -> SpanStyle(color = Color(0, 150, 0))
//            Recipient.Capability.NOT_SUPPORTED -> SpanStyle(color = Color.Red)
//            Recipient.Capability.UNKNOWN -> SpanStyle(fontStyle = FontStyle.Italic)
//          }
//
//          buildAnnotatedString {
//            withStyle(style = style) {
//              append("SSREv2")
//            }
//          }
        } else {
          AnnotatedString("Recipient not found!")
        }
      }
    }
  }
}

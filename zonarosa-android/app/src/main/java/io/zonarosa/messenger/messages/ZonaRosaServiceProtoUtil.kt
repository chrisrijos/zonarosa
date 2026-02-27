package io.zonarosa.messenger.messages

import com.squareup.wire.Message
import okio.ByteString
import okio.ByteString.Companion.toByteString
import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.isNotEmpty
import io.zonarosa.core.util.orNull
import io.zonarosa.libzonarosa.protocol.message.DecryptionErrorMessage
import io.zonarosa.libzonarosa.zkgroup.groups.GroupMasterKey
import io.zonarosa.storageservice.storage.protos.groups.local.DecryptedGroupChange
import io.zonarosa.messenger.attachments.Attachment
import io.zonarosa.messenger.attachments.Cdn
import io.zonarosa.messenger.attachments.PointerAttachment
import io.zonarosa.messenger.database.model.StoryType
import io.zonarosa.messenger.groups.GroupId
import io.zonarosa.messenger.stickers.StickerLocator
import io.zonarosa.messenger.util.MediaUtil
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.service.api.InvalidMessageStructureException
import io.zonarosa.service.api.crypto.EnvelopeMetadata
import io.zonarosa.service.api.messages.ZonaRosaServiceAttachmentPointer
import io.zonarosa.service.api.payments.Money
import io.zonarosa.service.api.util.AttachmentPointerUtil
import io.zonarosa.service.internal.push.AttachmentPointer
import io.zonarosa.service.internal.push.DataMessage
import io.zonarosa.service.internal.push.DataMessage.Payment
import io.zonarosa.service.internal.push.GroupContextV2
import io.zonarosa.service.internal.push.StoryMessage
import io.zonarosa.service.internal.push.SyncMessage
import io.zonarosa.service.internal.push.SyncMessage.Sent
import io.zonarosa.service.internal.push.TypingMessage
import io.zonarosa.service.internal.util.Util
import java.util.Optional
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object ZonaRosaServiceProtoUtil {

  @JvmStatic
  val emptyGroupChange: DecryptedGroupChange by lazy { DecryptedGroupChange() }

  /** Contains some user data that affects the conversation  */
  val DataMessage.hasRenderableContent: Boolean
    get() {
      return attachments.isNotEmpty() ||
        body != null ||
        quote != null ||
        contact.isNotEmpty() ||
        preview.isNotEmpty() ||
        bodyRanges.isNotEmpty() ||
        sticker != null ||
        reaction != null ||
        hasRemoteDelete ||
        pollCreate != null ||
        pollVote != null ||
        pollTerminate != null ||
        pinMessage != null ||
        unpinMessage != null ||
        adminDelete != null
    }

  val DataMessage.hasDisallowedAnnouncementOnlyContent: Boolean
    get() {
      return body != null ||
        attachments.isNotEmpty() ||
        quote != null ||
        preview.isNotEmpty() ||
        bodyRanges.isNotEmpty() ||
        sticker != null
    }

  val DataMessage.isExpirationUpdate: Boolean
    get() = flags != null && flags!! and DataMessage.Flags.EXPIRATION_TIMER_UPDATE.value != 0

  val DataMessage.hasRemoteDelete: Boolean
    get() = delete != null && delete!!.targetSentTimestamp != null

  val DataMessage.isGroupV2Update: Boolean
    get() = !hasRenderableContent && hasSignedGroupChange

  val DataMessage?.hasGroupContext: Boolean
    get() = this?.groupV2?.masterKey.isNotEmpty()

  val DataMessage.hasSignedGroupChange: Boolean
    get() = hasGroupContext && groupV2!!.hasSignedGroupChange

  val DataMessage.isMediaMessage: Boolean
    get() = attachments.isNotEmpty() || quote != null || contact.isNotEmpty() || sticker != null || bodyRanges.isNotEmpty() || preview.isNotEmpty()

  val DataMessage.isEndSession: Boolean
    get() = flags != null && flags!! and DataMessage.Flags.END_SESSION.value != 0

  val DataMessage.isStoryReaction: Boolean
    get() = reaction != null && storyContext != null

  val DataMessage.isPaymentActivationRequest: Boolean
    get() = payment?.activation?.type == Payment.Activation.Type.REQUEST

  val DataMessage.isPaymentActivated: Boolean
    get() = payment?.activation?.type == Payment.Activation.Type.ACTIVATED

  val DataMessage.isInvalid: Boolean
    get() {
      if (isViewOnce == true) {
        val contentType = attachments.getOrNull(0)?.contentType?.lowercase()
        return attachments.size != 1 || !MediaUtil.isImageOrVideoType(contentType)
      }
      return false
    }

  val DataMessage.isEmptyGroupV2Message: Boolean
    get() = hasGroupContext && !isGroupV2Update && !hasRenderableContent

  val DataMessage.expireTimerDuration: Duration
    get() = (expireTimer ?: 0).seconds

  val GroupContextV2.hasSignedGroupChange: Boolean
    get() = groupChange.isNotEmpty()

  val GroupContextV2.signedGroupChange: ByteArray
    get() = groupChange!!.toByteArray()

  val GroupContextV2.groupMasterKey: GroupMasterKey
    get() = GroupMasterKey(masterKey!!.toByteArray())

  val GroupContextV2?.isValid: Boolean
    get() = this?.masterKey.isNotEmpty()

  val GroupContextV2.groupId: GroupId.V2?
    get() = if (isValid) GroupId.v2(groupMasterKey) else null

  val StoryMessage.type: StoryType
    get() {
      return if (allowsReplies == true) {
        if (textAttachment != null) {
          StoryType.TEXT_STORY_WITH_REPLIES
        } else {
          StoryType.STORY_WITH_REPLIES
        }
      } else {
        if (textAttachment != null) {
          StoryType.TEXT_STORY_WITHOUT_REPLIES
        } else {
          StoryType.STORY_WITHOUT_REPLIES
        }
      }
    }

  fun Sent.isUnidentified(serviceId: ServiceId?): Boolean {
    return serviceId != null && unidentifiedStatus.firstOrNull { ServiceId.parseOrNull(it.destinationServiceId, it.destinationServiceIdBinary) == serviceId }?.unidentified ?: false
  }

  val Sent.serviceIdsToUnidentifiedStatus: Map<ServiceId, Boolean>
    get() {
      return unidentifiedStatus
        .mapNotNull { status ->
          val serviceId = ServiceId.parseOrNull(status.destinationServiceId, status.destinationServiceIdBinary)
          if (serviceId != null) {
            serviceId to (status.unidentified ?: false)
          } else {
            null
          }
        }
        .toMap()
    }

  val TypingMessage.hasStarted: Boolean
    get() = action == TypingMessage.Action.STARTED

  fun ByteString.toDecryptionErrorMessage(metadata: EnvelopeMetadata): DecryptionErrorMessage {
    try {
      return DecryptionErrorMessage(toByteArray())
    } catch (e: InvalidMessageStructureException) {
      throw InvalidMessageStructureException(e, metadata.sourceServiceId.toString(), metadata.sourceDeviceId)
    }
  }

  fun List<AttachmentPointer>.toPointersWithinLimit(): List<Attachment> {
    return mapNotNull { it.toPointer() }.take(RemoteConfig.maxAttachmentCount)
  }

  fun AttachmentPointer.toPointer(stickerLocator: StickerLocator? = null): Attachment? {
    return try {
      val pointer = PointerAttachment.forPointer(Optional.of(toZonaRosaServiceAttachmentPointer()), stickerLocator).orNull()
      if (pointer?.cdn != Cdn.S3) {
        pointer
      } else {
        null
      }
    } catch (e: InvalidMessageStructureException) {
      null
    }
  }

  fun AttachmentPointer.toZonaRosaServiceAttachmentPointer(): ZonaRosaServiceAttachmentPointer {
    return AttachmentPointerUtil.createZonaRosaAttachmentPointer(this)
  }

  fun Long.toMobileCoinMoney(): Money {
    return Money.picoMobileCoin(this)
  }

  fun SyncMessage.Builder.pad(length: Int = 512): SyncMessage.Builder {
    padding(Util.getRandomLengthSecretBytes(length).toByteString())
    return this
  }

  @Suppress("UNCHECKED_CAST")
  inline fun <reified MessageType : Message<MessageType, BuilderType>, BuilderType : Message.Builder<MessageType, BuilderType>> Message.Builder<MessageType, BuilderType>.buildWith(block: BuilderType.() -> Unit): MessageType {
    block(this as BuilderType)
    return build()
  }
}

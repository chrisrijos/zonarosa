package io.zonarosa.messenger.messages

import io.zonarosa.core.models.ServiceId
import io.zonarosa.core.util.orNull
import io.zonarosa.ringrtc.CallId
import io.zonarosa.messenger.database.model.IdentityRecord
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.jobs.ProfileKeySendJob
import io.zonarosa.messenger.messages.MessageContentProcessor.Companion.log
import io.zonarosa.messenger.messages.MessageContentProcessor.Companion.warn
import io.zonarosa.messenger.recipients.Recipient
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.recipients.RecipientUtil
import io.zonarosa.messenger.ringrtc.RemotePeer
import io.zonarosa.messenger.service.webrtc.WebRtcData.AnswerMetadata
import io.zonarosa.messenger.service.webrtc.WebRtcData.CallMetadata
import io.zonarosa.messenger.service.webrtc.WebRtcData.HangupMetadata
import io.zonarosa.messenger.service.webrtc.WebRtcData.OfferMetadata
import io.zonarosa.messenger.service.webrtc.WebRtcData.OpaqueMessageMetadata
import io.zonarosa.messenger.service.webrtc.WebRtcData.ReceivedAnswerMetadata
import io.zonarosa.messenger.service.webrtc.WebRtcData.ReceivedOfferMetadata
import io.zonarosa.service.api.crypto.EnvelopeMetadata
import io.zonarosa.service.api.messages.calls.HangupMessage
import io.zonarosa.service.api.messages.calls.OfferMessage
import io.zonarosa.service.internal.push.CallMessage
import io.zonarosa.service.internal.push.CallMessage.Offer
import io.zonarosa.service.internal.push.CallMessage.Opaque
import io.zonarosa.service.internal.push.Content
import io.zonarosa.service.internal.push.Envelope
import kotlin.time.Duration.Companion.milliseconds

object CallMessageProcessor {
  fun process(
    senderRecipient: Recipient,
    envelope: Envelope,
    content: Content,
    metadata: EnvelopeMetadata,
    serverDeliveredTimestamp: Long
  ) {
    val callMessage = content.callMessage!!

    if (metadata.destinationServiceId is ServiceId.PNI) {
      if (RecipientUtil.isCallRequestAccepted(senderRecipient) && callMessage.offer != null) {
        log(envelope.timestamp!!, "Received call offer message at our PNI from trusted sender, responding with profile and pni signature")
        RecipientUtil.shareProfileIfFirstSecureMessage(senderRecipient)
        ProfileKeySendJob.create(senderRecipient, false)?.let { AppDependencies.jobManager.add(it) }
      }

      if (callMessage.offer != null) {
        log(envelope.timestamp!!, "Call message at our PNI is an offer, continuing.")
      } else {
        log(envelope.timestamp!!, "Call message at our PNI is not an offer, ignoring.")
        return
      }
    }

    when {
      callMessage.offer != null -> handleCallOfferMessage(envelope, metadata, callMessage.offer!!, senderRecipient.id, serverDeliveredTimestamp)
      callMessage.answer != null -> handleCallAnswerMessage(envelope, metadata, callMessage.answer!!, senderRecipient.id)
      callMessage.iceUpdate.isNotEmpty() -> handleCallIceUpdateMessage(envelope, metadata, callMessage.iceUpdate, senderRecipient.id)
      callMessage.hangup != null -> handleCallHangupMessage(envelope, metadata, callMessage.hangup!!, senderRecipient.id)
      callMessage.busy != null -> handleCallBusyMessage(envelope, metadata, callMessage.busy!!, senderRecipient.id)
      callMessage.opaque != null -> handleCallOpaqueMessage(envelope, metadata, callMessage.opaque!!, senderRecipient.requireAci(), serverDeliveredTimestamp)
    }
  }

  private fun handleCallOfferMessage(envelope: Envelope, metadata: EnvelopeMetadata, offer: Offer, senderRecipientId: RecipientId, serverDeliveredTimestamp: Long) {
    log(envelope.timestamp!!, "handleCallOfferMessage...")

    val offerId = if (offer.id != null && offer.type != null && offer.opaque != null) {
      offer.id!!
    } else {
      warn(envelope.timestamp!!, "Invalid offer, missing id, type, or opaque")
      return
    }

    val remotePeer = RemotePeer(senderRecipientId, CallId(offerId))
    val remoteIdentityKey = AppDependencies.protocolStore.get(metadata.destinationServiceId).identities().getIdentityRecord(senderRecipientId).map { (_, identityKey): IdentityRecord -> identityKey.serialize() }.orNull()

    AppDependencies.zonarosaCallManager
      .receivedOffer(
        CallMetadata(remotePeer, metadata.sourceDeviceId),
        OfferMetadata(offer.opaque?.toByteArray(), OfferMessage.Type.fromProto(offer.type!!)),
        ReceivedOfferMetadata(
          metadata.destinationServiceId,
          remoteIdentityKey,
          envelope.serverTimestamp!!,
          serverDeliveredTimestamp
        )
      )
  }

  private fun handleCallAnswerMessage(
    envelope: Envelope,
    metadata: EnvelopeMetadata,
    answer: CallMessage.Answer,
    senderRecipientId: RecipientId
  ) {
    log(envelope.timestamp!!, "handleCallAnswerMessage...")

    val answerId = if (answer.id != null && answer.opaque != null) {
      answer.id!!
    } else {
      warn(envelope.timestamp!!, "Invalid answer, missing id or opaque")
      return
    }

    val remotePeer = RemotePeer(senderRecipientId, CallId(answerId))
    val remoteIdentityKey = AppDependencies.protocolStore.aci().identities().getIdentityRecord(senderRecipientId).map { (_, identityKey): IdentityRecord -> identityKey.serialize() }.get()

    AppDependencies.zonarosaCallManager
      .receivedAnswer(
        CallMetadata(remotePeer, metadata.sourceDeviceId),
        AnswerMetadata(answer.opaque?.toByteArray()),
        ReceivedAnswerMetadata(remoteIdentityKey)
      )
  }

  private fun handleCallIceUpdateMessage(
    envelope: Envelope,
    metadata: EnvelopeMetadata,
    iceUpdateList: List<CallMessage.IceUpdate>,
    senderRecipientId: RecipientId
  ) {
    log(envelope.timestamp!!, "handleCallIceUpdateMessage... " + iceUpdateList.size)

    val iceCandidates: MutableList<ByteArray> = ArrayList(iceUpdateList.size)
    var callId: Long = -1

    iceUpdateList
      .filter { it.opaque != null && it.id != null }
      .forEach { iceUpdate ->
        iceCandidates += iceUpdate.opaque!!.toByteArray()
        callId = iceUpdate.id!!
      }

    if (iceCandidates.isNotEmpty()) {
      val remotePeer = RemotePeer(senderRecipientId, CallId(callId))
      AppDependencies.zonarosaCallManager
        .receivedIceCandidates(
          CallMetadata(remotePeer, metadata.sourceDeviceId),
          iceCandidates
        )
    } else {
      warn(envelope.timestamp!!, "Invalid ice updates, all missing opaque and/or call id")
    }
  }

  private fun handleCallHangupMessage(
    envelope: Envelope,
    metadata: EnvelopeMetadata,
    hangup: CallMessage.Hangup?,
    senderRecipientId: RecipientId
  ) {
    log(envelope.timestamp!!, "handleCallHangupMessage")

    val (hangupId: Long, hangupDeviceId: Int?) = if (hangup?.id != null) {
      hangup.id!! to hangup.deviceId
    } else {
      warn(envelope.timestamp!!, "Invalid hangup, null message or missing id/deviceId")
      return
    }

    val remotePeer = RemotePeer(senderRecipientId, CallId(hangupId))
    AppDependencies.zonarosaCallManager
      .receivedCallHangup(
        CallMetadata(remotePeer, metadata.sourceDeviceId),
        HangupMetadata(HangupMessage.Type.fromProto(hangup.type), hangupDeviceId ?: 0)
      )
  }

  private fun handleCallBusyMessage(envelope: Envelope, metadata: EnvelopeMetadata, busy: CallMessage.Busy, senderRecipientId: RecipientId) {
    log(envelope.timestamp!!, "handleCallBusyMessage")

    val busyId = if (busy.id != null) {
      busy.id!!
    } else {
      warn(envelope.timestamp!!, "Invalid busy, missing call id")
      return
    }

    val remotePeer = RemotePeer(senderRecipientId, CallId(busyId))
    AppDependencies.zonarosaCallManager.receivedCallBusy(CallMetadata(remotePeer, metadata.sourceDeviceId))
  }

  private fun handleCallOpaqueMessage(envelope: Envelope, metadata: EnvelopeMetadata, opaque: Opaque, senderServiceId: ServiceId, serverDeliveredTimestamp: Long) {
    log(envelope.timestamp!!, "handleCallOpaqueMessage")

    val data = if (opaque.data_ != null) {
      opaque.data_!!.toByteArray()
    } else {
      warn(envelope.timestamp!!, "Invalid opaque message, null data")
      return
    }

    var messageAgeSeconds: Long = 0
    if (envelope.serverTimestamp in 1..serverDeliveredTimestamp) {
      messageAgeSeconds = (serverDeliveredTimestamp - envelope.serverTimestamp!!).milliseconds.inWholeSeconds
    }

    AppDependencies.zonarosaCallManager
      .receivedOpaqueMessage(
        OpaqueMessageMetadata(
          senderServiceId.rawUuid,
          data,
          metadata.sourceDeviceId,
          messageAgeSeconds
        )
      )
  }
}

/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.service.webrtc

import io.zonarosa.core.util.logging.Log
import io.zonarosa.libzonarosa.zkgroup.GenericServerPublicParams
import io.zonarosa.libzonarosa.zkgroup.InvalidInputException
import io.zonarosa.libzonarosa.zkgroup.ServerPublicParams
import io.zonarosa.libzonarosa.zkgroup.VerificationFailedException
import io.zonarosa.libzonarosa.zkgroup.calllinks.CallLinkSecretParams
import io.zonarosa.ringrtc.CallException
import io.zonarosa.ringrtc.CallLinkRootKey
import io.zonarosa.messenger.database.ZonaRosaDatabase.Companion.callLinks
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.events.WebRtcViewModel
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.ringrtc.RemotePeer
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceState
import io.zonarosa.messenger.util.NetworkUtil
import java.io.IOException

/**
 * Process actions while the user is in the pre-join lobby for the call link.
 */
class CallLinkPreJoinActionProcessor(
  actionProcessorFactory: MultiPeerActionProcessorFactory,
  webRtcInteractor: WebRtcInteractor
) : GroupPreJoinActionProcessor(actionProcessorFactory, webRtcInteractor, TAG) {

  companion object {
    private val TAG = Log.tag(CallLinkPreJoinActionProcessor::class.java)
  }

  override fun handleSetRingGroup(currentState: WebRtcServiceState, ringGroup: Boolean): WebRtcServiceState {
    Log.i(TAG, "handleSetRingGroup(): Ignoring.")
    return currentState
  }

  override fun handlePreJoinCall(currentState: WebRtcServiceState, remotePeer: RemotePeer): WebRtcServiceState {
    Log.i(TAG, "handlePreJoinCall():")

    val groupCall = try {
      val callLink = callLinks.getCallLinkByRoomId(remotePeer.recipient.requireCallLinkRoomId())
      if (callLink?.credentials == null) {
        return groupCallFailure(currentState, "No access to this call link.", Exception())
      }

      val callLinkRootKey = CallLinkRootKey(callLink.credentials.linkKeyBytes)
      val callLinkSecretParams = CallLinkSecretParams.deriveFromRootKey(callLink.credentials.linkKeyBytes)
      val genericServerPublicParams = GenericServerPublicParams(
        AppDependencies.zonarosaServiceNetworkAccess
          .getConfiguration()
          .genericServerPublicParams
      )
      val serverPublicParams = ServerPublicParams(
        AppDependencies.zonarosaServiceNetworkAccess
          .getConfiguration()
          .zkGroupServerPublicParams
      )

      val callLinkAuthCredentialPresentation = AppDependencies
        .groupsV2Authorization
        .getCallLinkAuthorizationForToday(genericServerPublicParams, callLinkSecretParams)

      webRtcInteractor.callManager.createCallLinkCall(
        ZonaRosaStore.internal.groupCallingServer,
        serverPublicParams.endorsementPublicKey,
        callLinkAuthCredentialPresentation.serialize(),
        callLinkRootKey,
        callLink.credentials.adminPassBytes,
        ByteArray(0),
        AUDIO_LEVELS_INTERVAL,
        RingRtcDynamicConfiguration.getAudioConfig(),
        webRtcInteractor.groupCallObserver
      )
    } catch (e: InvalidInputException) {
      return groupCallFailure(currentState, "Failed to create server public parameters.", e)
    } catch (e: IOException) {
      return groupCallFailure(currentState, "Failed to get call link authorization", e)
    } catch (e: VerificationFailedException) {
      return groupCallFailure(currentState, "Failed to get call link authorization", e)
    } catch (e: CallException) {
      return groupCallFailure(currentState, "Failed to parse call link root key", e)
    } ?: return groupCallFailure(currentState, "Failed to create group call object for call link.", Exception())

    try {
      groupCall.setOutgoingAudioMuted(true)
      groupCall.setOutgoingVideoMuted(true)
      groupCall.setDataMode(NetworkUtil.getCallingDataMode(context, groupCall.localDeviceState.networkRoute.localAdapterType))
      Log.i(TAG, "Connecting to group call: " + currentState.callInfoState.callRecipient.id)
      groupCall.connect()
    } catch (e: CallException) {
      return groupCallFailure(currentState, "Unable to connect to call link", e)
    }

    ZonaRosaStore.tooltips.markGroupCallingLobbyEntered()
    return currentState.builder()
      .changeCallSetupState(RemotePeer.GROUP_CALL_ID)
      .setRingGroup(false)
      .commit()
      .changeCallInfoState()
      .groupCall(groupCall)
      .groupCallState(WebRtcViewModel.GroupCallState.DISCONNECTED)
      .activePeer(RemotePeer(currentState.callInfoState.callRecipient.id, RemotePeer.GROUP_CALL_ID))
      .build()
  }

  override fun handleGroupRequestUpdateMembers(currentState: WebRtcServiceState): WebRtcServiceState {
    Log.i(tag, "handleGroupRequestUpdateMembers():")

    return currentState
  }
}

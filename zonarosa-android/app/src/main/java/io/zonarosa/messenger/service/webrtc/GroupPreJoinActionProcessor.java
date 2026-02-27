package io.zonarosa.messenger.service.webrtc;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.ringrtc.CallException;
import io.zonarosa.ringrtc.GroupCall;
import io.zonarosa.ringrtc.PeekInfo;
import io.zonarosa.messenger.components.webrtc.BroadcastVideoSink;
import io.zonarosa.messenger.components.webrtc.CallParticipantsState;
import io.zonarosa.messenger.components.webrtc.EglBaseWrapper;
import io.zonarosa.messenger.events.CallParticipant;
import io.zonarosa.messenger.events.CallParticipantId;
import io.zonarosa.messenger.events.WebRtcViewModel;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.ringrtc.RemotePeer;
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceState;
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceStateBuilder;
import io.zonarosa.messenger.util.NetworkUtil;
import io.zonarosa.service.api.messages.calls.OfferMessage;
import io.zonarosa.core.models.ServiceId.ACI;

import java.util.List;

import static io.zonarosa.messenger.webrtc.CallNotificationBuilder.TYPE_OUTGOING_RINGING;

/**
 * Process actions while the user is in the pre-join lobby for the call.
 */
public class GroupPreJoinActionProcessor extends GroupActionProcessor {

  private static final String TAG = Log.tag(GroupPreJoinActionProcessor.class);

  public GroupPreJoinActionProcessor(@NonNull MultiPeerActionProcessorFactory actionProcessorFactory, @NonNull WebRtcInteractor webRtcInteractor) {
    this(actionProcessorFactory, webRtcInteractor, TAG);
  }

  protected GroupPreJoinActionProcessor(@NonNull MultiPeerActionProcessorFactory actionProcessorFactory, @NonNull WebRtcInteractor webRtcInteractor, @NonNull String tag) {
    super(actionProcessorFactory, webRtcInteractor, tag);
  }

  @Override
  protected @NonNull WebRtcServiceState handlePreJoinCall(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    Log.i(tag, "handlePreJoinCall():");

    byte[]      groupId = currentState.getCallInfoState().getCallRecipient().requireGroupId().getDecodedId();
    GroupCall groupCall = webRtcInteractor.getCallManager().createGroupCall(groupId,
                                                                            ZonaRosaStore.internal().getGroupCallingServer(),
                                                                            new byte[0],
                                                                            AUDIO_LEVELS_INTERVAL,
                                                                            RingRtcDynamicConfiguration.getAudioConfig(),
                                                                            webRtcInteractor.getGroupCallObserver());

    if (groupCall == null) {
      return groupCallFailure(currentState, "RingRTC did not create a group call", null);
    }

    try {
      groupCall.setOutgoingAudioMuted(true);
      groupCall.setOutgoingVideoMuted(true);
      groupCall.setDataMode(NetworkUtil.getCallingDataMode(context, groupCall.getLocalDeviceState().getNetworkRoute().getLocalAdapterType()));

      Log.i(tag, "Connecting to group call: " + currentState.getCallInfoState().getCallRecipient().getId());
      groupCall.connect();
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to connect to group call", e);
    }

    ZonaRosaStore.tooltips().markGroupCallingLobbyEntered();
    return currentState.builder()
                       .changeCallInfoState()
                       .groupCall(groupCall)
                       .groupCallState(WebRtcViewModel.GroupCallState.DISCONNECTED)
                       .activePeer(new RemotePeer(currentState.getCallInfoState().getCallRecipient().getId(), RemotePeer.GROUP_CALL_ID))
                       .build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleCancelPreJoinCall(@NonNull WebRtcServiceState currentState) {
    Log.i(tag, "handleCancelPreJoinCall():");

    GroupCall groupCall = currentState.getCallInfoState().requireGroupCall();
    try {
      groupCall.disconnect();
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to disconnect from group call", e);
    }

    WebRtcVideoUtil.deinitializeVideo(currentState);
    EglBaseWrapper.releaseEglBase(RemotePeer.GROUP_CALL_ID.longValue());

    return new WebRtcServiceState(new IdleActionProcessor(webRtcInteractor));
  }

  @Override
  protected @NonNull WebRtcServiceState handleGroupLocalDeviceStateChanged(@NonNull WebRtcServiceState currentState) {
    Log.i(tag, "handleGroupLocalDeviceStateChanged():");

    currentState = super.handleGroupLocalDeviceStateChanged(currentState);

    GroupCall                  groupCall = currentState.getCallInfoState().requireGroupCall();
    GroupCall.LocalDeviceState device    = groupCall.getLocalDeviceState();

    Log.i(tag, "local device changed: " + device.getConnectionState() + " " + device.getJoinState());

    return currentState.builder()
                       .changeCallInfoState()
                       .groupCallState(WebRtcUtil.groupCallStateForConnection(device.getConnectionState()))
                       .build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleGroupJoinedMembershipChanged(@NonNull WebRtcServiceState currentState) {
    Log.i(tag, "handleGroupJoinedMembershipChanged():");

    GroupCall groupCall = currentState.getCallInfoState().requireGroupCall();
    PeekInfo  peekInfo  = groupCall.getPeekInfo();

    if (peekInfo == null) {
      Log.i(tag, "No peek info available");
      return currentState;
    }

    List<Recipient> callParticipants = Stream.of(peekInfo.getJoinedMembers())
                                             .map(uuid -> Recipient.externalPush(ACI.from(uuid)))
                                             .toList();

    WebRtcServiceStateBuilder.CallInfoStateBuilder builder = currentState.builder()
                                                                         .changeCallInfoState()
                                                                         .remoteDevicesCount(peekInfo.getDeviceCountExcludingPendingDevices())
                                                                         .participantLimit(peekInfo.getMaxDevices())
                                                                         .clearParticipantMap();

    for (Recipient recipient : callParticipants) {
      builder.putParticipant(recipient, CallParticipant.createRemote(new CallParticipantId(recipient),
                                                                     recipient,
                                                                     null,
                                                                     new BroadcastVideoSink(),
                                                                     true,
                                                                     true,
                                                                     true,
                                                                     CallParticipant.HAND_LOWERED,
                                                                     0,
                                                                     false,
                                                                     0,
                                                                     false,
                                                                     CallParticipant.DeviceOrdinal.PRIMARY));
    }

    WebRtcServiceStateBuilder stateBuilder = builder.commit();

    if (peekInfo.getDeviceCountExcludingPendingDevices() >= CallParticipantsState.PRE_JOIN_MUTE_THRESHOLD && currentState.getLocalDeviceState().isMicrophoneEnabled()) {
      Log.i(tag, "Large call detected (" + peekInfo.getDeviceCountExcludingPendingDevices() + " participants), auto-muting microphone");
      return stateBuilder.changeLocalDeviceState()
                         .isMicrophoneEnabled(false)
                         .build();
    }

    return stateBuilder.build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleOutgoingCall(@NonNull WebRtcServiceState currentState,
                                                           @NonNull RemotePeer remotePeer,
                                                           @NonNull OfferMessage.Type offerType)
  {
    Log.i(tag, "handleOutgoingCall():");

    GroupCall groupCall = currentState.getCallInfoState().requireGroupCall();

    currentState = WebRtcVideoUtil.reinitializeCamera(context, webRtcInteractor.getCameraEventListener(), currentState);

    webRtcInteractor.setCallInProgressNotification(TYPE_OUTGOING_RINGING, currentState.getCallInfoState().getCallRecipient(), true);
    webRtcInteractor.updatePhoneState(WebRtcUtil.getInCallPhoneState(context));
    webRtcInteractor.initializeAudioForCall();

    try {
      groupCall.setOutgoingVideoSource(currentState.getVideoState().requireLocalSink(), currentState.getVideoState().requireCamera());
      groupCall.setOutgoingVideoMuted(!currentState.getLocalDeviceState().getCameraState().isEnabled());
      groupCall.setOutgoingAudioMuted(!currentState.getLocalDeviceState().isMicrophoneEnabled());
      groupCall.setDataMode(NetworkUtil.getCallingDataMode(context, groupCall.getLocalDeviceState().getNetworkRoute().getLocalAdapterType()));

      groupCall.join();
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to join group call", e);
    }

    return currentState.builder()
                       .actionProcessor(actionProcessorFactory.createJoiningActionProcessor(webRtcInteractor))
                       .changeCallInfoState()
                       .callState(WebRtcViewModel.State.CALL_OUTGOING)
                       .groupCallState(WebRtcViewModel.GroupCallState.CONNECTED_AND_JOINING)
                       .commit()
                       .changeLocalDeviceState()
                       .build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetEnableVideo(@NonNull WebRtcServiceState currentState, boolean enable) {
    Log.i(tag, "handleSetEnableVideo(): Changing for pre-join group call. enable: " + enable);

    currentState.getVideoState().requireCamera().setEnabled(enable);
    return currentState.builder()
                       .changeCallSetupState(RemotePeer.GROUP_CALL_ID)
                       .enableVideoOnCreate(enable)
                       .commit()
                       .changeLocalDeviceState()
                       .cameraState(currentState.getVideoState().requireCamera().getCameraState())
                       .build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetMuteAudio(@NonNull WebRtcServiceState currentState, boolean muted) {
    Log.i(tag, "handleSetMuteAudio(): Changing for pre-join group call. muted: " + muted);

    return currentState.builder()
                       .changeLocalDeviceState()
                       .isMicrophoneEnabled(!muted)
                       .build();
  }

  @Override
  public @NonNull WebRtcServiceState handleNetworkChanged(@NonNull WebRtcServiceState currentState, boolean available) {
    if (!available) {
      return currentState.builder()
                         .actionProcessor(actionProcessorFactory.createNetworkUnavailableActionProcessor(webRtcInteractor))
                         .changeCallInfoState()
                         .callState(WebRtcViewModel.State.NETWORK_FAILURE)
                         .build();
    } else {
      return currentState;
    }
  }
}

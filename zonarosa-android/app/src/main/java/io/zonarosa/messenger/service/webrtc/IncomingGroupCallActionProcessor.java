package io.zonarosa.messenger.service.webrtc;

import android.net.Uri;

import androidx.annotation.NonNull;

import io.zonarosa.core.models.ServiceId.ACI;
import io.zonarosa.core.util.logging.Log;
import io.zonarosa.ringrtc.CallException;
import io.zonarosa.ringrtc.CallId;
import io.zonarosa.ringrtc.CallManager;
import io.zonarosa.ringrtc.GroupCall;
import io.zonarosa.messenger.components.webrtc.BroadcastVideoSink;
import io.zonarosa.messenger.components.webrtc.EglBaseWrapper;
import io.zonarosa.messenger.database.RecipientTable;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.events.CallParticipant;
import io.zonarosa.messenger.events.CallParticipantId;
import io.zonarosa.messenger.events.WebRtcViewModel;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.keyvalue.ZonaRosaStore;
import io.zonarosa.messenger.notifications.DoNotDisturbUtil;
import io.zonarosa.messenger.notifications.NotificationChannels;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.ringrtc.Camera;
import io.zonarosa.messenger.ringrtc.RemotePeer;
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceState;
import io.zonarosa.messenger.util.AppForegroundObserver;
import io.zonarosa.messenger.util.NetworkUtil;
import io.zonarosa.messenger.webrtc.locks.LockManager;

import java.util.Optional;

import static io.zonarosa.messenger.webrtc.CallNotificationBuilder.TYPE_ESTABLISHED;
import static io.zonarosa.messenger.webrtc.CallNotificationBuilder.TYPE_INCOMING_RINGING;

/**
 * Process actions to go from incoming "ringing" group call to joining. By the time this processor
 * is running, the group call to ring has been verified to have at least one active device.
 */
public final class IncomingGroupCallActionProcessor extends DeviceAwareActionProcessor {

  private static final String TAG = Log.tag(IncomingGroupCallActionProcessor.class);

  public IncomingGroupCallActionProcessor(WebRtcInteractor webRtcInteractor) {
    super(webRtcInteractor, TAG);
  }

  @Override
  protected @NonNull WebRtcServiceState handleGroupCallRingUpdate(@NonNull WebRtcServiceState currentState,
                                                                  @NonNull RemotePeer remotePeerGroup,
                                                                  @NonNull GroupId.V2 groupId,
                                                                  long ringId,
                                                                  @NonNull ACI sender,
                                                                  @NonNull CallManager.RingUpdate ringUpdate)
  {
    Log.i(TAG, "handleGroupCallRingUpdate(): recipient: " + remotePeerGroup.getId() + " ring: " + ringId + " update: " + ringUpdate);

    Recipient recipient              = remotePeerGroup.getRecipient();
    boolean   updateForCurrentRingId = ringId == currentState.getCallSetupState(RemotePeer.GROUP_CALL_ID).getRingId();
    boolean   isCurrentlyRinging     = currentState.getCallInfoState().getGroupCallState().isRinging();

    if (ZonaRosaDatabase.calls().isRingCancelled(ringId, remotePeerGroup.getId()) && !updateForCurrentRingId) {
      try {
        Log.i(TAG, "Ignoring incoming ring request for already cancelled ring: " + ringId);
        webRtcInteractor.getCallManager().cancelGroupRing(groupId.getDecodedId(), ringId, null);
      } catch (CallException e) {
        Log.w(TAG, "Error while trying to cancel ring: " + ringId, e);
      }
      return currentState;
    }

    if (ringUpdate != CallManager.RingUpdate.REQUESTED) {
      ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromRingState(ringId,
                                                                  remotePeerGroup.getId(),
                                                                  sender,
                                                                  System.currentTimeMillis(),
                                                                  ringUpdate);

      if (updateForCurrentRingId && isCurrentlyRinging) {
        Log.i(TAG, "Cancelling current ring: " + ringId);

        currentState = currentState.builder()
                                   .changeCallInfoState()
                                   .callState(WebRtcViewModel.State.CALL_DISCONNECTED)
                                   .build();

        webRtcInteractor.postStateUpdate(currentState);

        return terminateGroupCall(currentState);
      } else {
        return currentState;
      }
    }

    if (!updateForCurrentRingId && isCurrentlyRinging) {
      try {
        Log.i(TAG, "Already ringing so reply busy for new ring: " + ringId);
        webRtcInteractor.getCallManager().cancelGroupRing(groupId.getDecodedId(), ringId, CallManager.RingCancelReason.Busy);
      } catch (CallException e) {
        Log.w(TAG, "Error while trying to cancel ring: " + ringId, e);
      }
      return currentState;
    }

    if (updateForCurrentRingId) {
      Log.i(TAG, "Already ringing for ring: " + ringId);
      return currentState;
    }

    Log.i(TAG, "Requesting new ring: " + ringId);

    Recipient ringerRecipient = Recipient.externalPush(sender);
    ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromRingState(
        ringId,
        remotePeerGroup.getId(),
        ringerRecipient.getId(),
        System.currentTimeMillis(),
        ringUpdate
    );

    currentState = WebRtcVideoUtil.initializeVideo(context, webRtcInteractor.getCameraEventListener(), currentState, RemotePeer.GROUP_CALL_ID.longValue());

    webRtcInteractor.setCallInProgressNotification(TYPE_INCOMING_RINGING, remotePeerGroup, true);
    webRtcInteractor.initializeAudioForCall();

    boolean shouldDisturbUserWithCall = DoNotDisturbUtil.shouldDisturbUserWithCall(context.getApplicationContext());
    if (shouldDisturbUserWithCall) {
      webRtcInteractor.updatePhoneState(LockManager.PhoneState.INTERACTIVE);
      boolean started = webRtcInteractor.startWebRtcCallActivityIfPossible();
      if (!started) {
        Log.i(TAG, "Unable to start call activity due to OS version or not being in the foreground");
        AppForegroundObserver.addListener(webRtcInteractor.getForegroundListener());
      }
    }

    boolean isCallNotificationsEnabled = ZonaRosaStore.settings().isCallNotificationsEnabled() && NotificationChannels.getInstance().areNotificationsEnabled();
    if (shouldDisturbUserWithCall && isCallNotificationsEnabled) {
      Uri                         ringtone     = recipient.resolve().getCallRingtone();
      RecipientTable.VibrateState vibrateState = recipient.resolve().getCallVibrate();

      if (ringtone == null) {
        ringtone = ZonaRosaStore.settings().getCallRingtone();
      }

      webRtcInteractor.startIncomingRinger(ringtone, vibrateState == RecipientTable.VibrateState.ENABLED || (vibrateState == RecipientTable.VibrateState.DEFAULT && ZonaRosaStore.settings().isCallVibrateEnabled()));
    }

    webRtcInteractor.registerPowerButtonReceiver();

    return currentState.builder()
                       .changeCallSetupState(RemotePeer.GROUP_CALL_ID)
                       .isRemoteVideoOffer(true)
                       .ringId(ringId)
                       .ringerRecipient(ringerRecipient)
                       .commit()
                       .changeCallInfoState()
                       .activePeer(new RemotePeer(currentState.getCallInfoState().getCallRecipient().getId(), RemotePeer.GROUP_CALL_ID))
                       .callRecipient(remotePeerGroup.getRecipient())
                       .callState(WebRtcViewModel.State.CALL_INCOMING)
                       .groupCallState(WebRtcViewModel.GroupCallState.RINGING)
                       .putParticipant(remotePeerGroup.getRecipient(),
                                       CallParticipant.createRemote(new CallParticipantId(remotePeerGroup.getRecipient()),
                                                                    remotePeerGroup.getRecipient(),
                                                                    null,
                                                                    new BroadcastVideoSink(currentState.getVideoState().getLockableEglBase(),
                                                                                           true,
                                                                                           true,
                                                                                           currentState.getLocalDeviceState().getOrientation().getDegrees()),
                                                                    true,
                                                                    true,
                                                                    false,
                                                                    CallParticipant.HAND_LOWERED,
                                                                    0,
                                                                    true,
                                                                    0,
                                                                    false,
                                                                    CallParticipant.DeviceOrdinal.PRIMARY
                                       ))
                       .build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleSetIncomingRingingVanity(@NonNull WebRtcServiceState currentState, boolean enabled) {
    boolean cameraAlreadyEnabled = currentState.getLocalDeviceState().getCameraState().isEnabled();

    if (enabled && cameraAlreadyEnabled) {
      return currentState;
    }

    if (!enabled && !cameraAlreadyEnabled) {
      return currentState;
    }

    Camera camera = currentState.getVideoState().requireCamera();

    if (enabled && !camera.isInitialized()) {
      Log.i(TAG, "handleSetIncomingRingingVanity(): initializing vanity camera");
      return WebRtcVideoUtil.initializeVanityCamera(currentState);
    } else if (enabled) {
      Log.i(TAG, "handleSetIncomingRingingVanity(): enabling vanity camera");
      camera.setEnabled(true);
    } else {
      Log.i(TAG, "handleSetIncomingRingingVanity(): disabling vanity camera");
      camera.setEnabled(false);
    }

    return currentState.builder()
                       .changeLocalDeviceState()
                       .cameraState(camera.getCameraState())
                       .build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleAcceptCall(@NonNull WebRtcServiceState currentState, boolean answerWithVideo) {
    currentState = WebRtcVideoUtil.reinitializeCamera(context, webRtcInteractor.getCameraEventListener(), currentState);

    byte[] groupId = currentState.getCallInfoState().getCallRecipient().requireGroupId().getDecodedId();
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

      Log.i(TAG, "Connecting to group call: " + currentState.getCallInfoState().getCallRecipient().getId());
      groupCall.connect();
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to connect to group call", e);
    }

    currentState = currentState.builder()
                               .changeCallInfoState()
                               .groupCall(groupCall)
                               .groupCallState(WebRtcViewModel.GroupCallState.DISCONNECTED)
                               .commit()
                               .changeCallSetupState(RemotePeer.GROUP_CALL_ID)
                               .isRemoteVideoOffer(false)
                               .enableVideoOnCreate(answerWithVideo)
                               .build();

    webRtcInteractor.setCallInProgressNotification(TYPE_ESTABLISHED, currentState.getCallInfoState().getCallRecipient(), true);
    webRtcInteractor.updatePhoneState(WebRtcUtil.getInCallPhoneState(context));
    webRtcInteractor.initializeAudioForCall();

    try {
      groupCall.setOutgoingVideoSource(currentState.getVideoState().requireLocalSink(), currentState.getVideoState().requireCamera());
      groupCall.setOutgoingVideoMuted(!answerWithVideo);
      groupCall.setOutgoingAudioMuted(!currentState.getLocalDeviceState().isMicrophoneEnabled());
      groupCall.setDataMode(NetworkUtil.getCallingDataMode(context, groupCall.getLocalDeviceState().getNetworkRoute().getLocalAdapterType()));

      groupCall.join();
    } catch (CallException e) {
      return groupCallFailure(currentState, "Unable to join group call", e);
    }

    if (answerWithVideo) {
      Camera camera = currentState.getVideoState().requireCamera();
      camera.setEnabled(true);
      currentState = currentState.builder()
                                 .changeLocalDeviceState()
                                 .cameraState(camera.getCameraState())
                                 .build();
    }

    return currentState.builder()
                       .actionProcessor(MultiPeerActionProcessorFactory.GroupActionProcessorFactory.INSTANCE.createJoiningActionProcessor(webRtcInteractor))
                       .changeCallInfoState()
                       .callState(WebRtcViewModel.State.CALL_OUTGOING)
                       .groupCallState(WebRtcViewModel.GroupCallState.CONNECTED_AND_JOINING)
                       .commit()
                       .changeLocalDeviceState()
                       .build();
  }

  @Override
  protected @NonNull WebRtcServiceState handleDenyCall(@NonNull WebRtcServiceState currentState) {
    Log.i(TAG, "handleDenyCall():");

    Recipient         recipient = currentState.getCallInfoState().getCallRecipient();
    Optional<GroupId> groupId   = recipient.getGroupId();
    long              ringId    = currentState.getCallSetupState(RemotePeer.GROUP_CALL_ID).getRingId();
    Recipient         ringer    = currentState.getCallSetupState(RemotePeer.GROUP_CALL_ID).getRingerRecipient();

    ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromRingState(ringId,
                                                                recipient.getId(),
                                                                ringer.getId(),
                                                                System.currentTimeMillis(),
                                                                CallManager.RingUpdate.DECLINED_ON_ANOTHER_DEVICE);

    try {
      webRtcInteractor.getCallManager().cancelGroupRing(groupId.get().getDecodedId(),
                                                        ringId,
                                                        CallManager.RingCancelReason.DeclinedByUser);
    } catch (CallException e) {
      Log.w(TAG, "Error while trying to cancel ring " + ringId, e);
    }

    CallId     callId     = new CallId(ringId);
    RemotePeer remotePeer = new RemotePeer(recipient.getId(), callId);

    webRtcInteractor.sendGroupCallNotAcceptedCallEventSyncMessage(remotePeer, false);
    webRtcInteractor.sendGroupCallMessage(currentState.getCallInfoState().getCallRecipient(), null, callId, true, false);
    webRtcInteractor.updatePhoneState(LockManager.PhoneState.PROCESSING);
    webRtcInteractor.stopAudio(false);
    webRtcInteractor.updatePhoneState(LockManager.PhoneState.IDLE);
    webRtcInteractor.stopForegroundService();

    currentState = WebRtcVideoUtil.deinitializeVideo(currentState);
    EglBaseWrapper.releaseEglBase(RemotePeer.GROUP_CALL_ID.longValue());

    return currentState.builder()
                       .actionProcessor(new IdleActionProcessor(webRtcInteractor))
                       .terminate(RemotePeer.GROUP_CALL_ID)
                       .build();
  }
}

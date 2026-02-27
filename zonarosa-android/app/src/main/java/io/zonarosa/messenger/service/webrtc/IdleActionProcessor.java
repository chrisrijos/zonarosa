package io.zonarosa.messenger.service.webrtc;

import androidx.annotation.NonNull;

import io.zonarosa.core.util.logging.Log;
import io.zonarosa.ringrtc.CallException;
import io.zonarosa.ringrtc.CallManager;
import io.zonarosa.ringrtc.PeekInfo;
import io.zonarosa.messenger.components.webrtc.EglBaseWrapper;
import io.zonarosa.messenger.database.ZonaRosaDatabase;
import io.zonarosa.messenger.events.WebRtcViewModel;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.notifications.profiles.NotificationProfile;
import io.zonarosa.messenger.notifications.profiles.NotificationProfiles;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.ringrtc.RemotePeer;
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceState;
import io.zonarosa.messenger.util.RemoteConfig;
import io.zonarosa.service.api.messages.calls.OfferMessage;
import io.zonarosa.core.models.ServiceId.ACI;

/**
 * Action handler for when the system is at rest. Mainly responsible
 * for starting pre-call state, starting an outgoing call, or receiving an
 * incoming call.
 */
public class IdleActionProcessor extends WebRtcActionProcessor {

  private static final String TAG = Log.tag(IdleActionProcessor.class);

  private final BeginCallActionProcessorDelegate beginCallDelegate;

  public IdleActionProcessor(@NonNull WebRtcInteractor webRtcInteractor) {
    super(webRtcInteractor, TAG);
    beginCallDelegate = new BeginCallActionProcessorDelegate(webRtcInteractor, TAG);
  }

  protected @NonNull WebRtcServiceState handleStartIncomingCall(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer, @NonNull OfferMessage.Type offerType) {
    Log.i(TAG, "handleStartIncomingCall():");

    currentState = WebRtcVideoUtil.initializeVideo(context, webRtcInteractor.getCameraEventListener(), currentState, remotePeer.getCallId().longValue());
    return beginCallDelegate.handleStartIncomingCall(currentState, remotePeer, offerType);
  }

  @Override
  protected @NonNull WebRtcServiceState handleOutgoingCall(@NonNull WebRtcServiceState currentState,
                                                           @NonNull RemotePeer remotePeer,
                                                           @NonNull OfferMessage.Type offerType)
  {
    Log.i(TAG, "handleOutgoingCall():");

    Recipient recipient = Recipient.resolved(remotePeer.getId());
    if (recipient.isGroup() || recipient.isCallLink()) {
      Log.w(TAG, "Aborting attempt to start 1:1 call for group or call link recipient: " + remotePeer.getId());
      return currentState;
    }

    currentState = WebRtcVideoUtil.initializeVideo(context, webRtcInteractor.getCameraEventListener(), currentState, EglBaseWrapper.OUTGOING_PLACEHOLDER);
    return beginCallDelegate.handleOutgoingCall(currentState, remotePeer, offerType);
  }

  @Override
  protected @NonNull WebRtcServiceState handlePreJoinCall(@NonNull WebRtcServiceState currentState, @NonNull RemotePeer remotePeer) {
    Log.i(TAG, "handlePreJoinCall():");

    boolean isGroupCall = remotePeer.getRecipient().isPushV2Group() || remotePeer.getRecipient().isCallLink();

    final WebRtcActionProcessor processor;
    if (remotePeer.getRecipient().isCallLink()) {
      processor = MultiPeerActionProcessorFactory.CallLinkActionProcessorFactory.INSTANCE.createPreJoinActionProcessor(webRtcInteractor);
    } else if (remotePeer.getRecipient().isPushV2Group()) {
      processor = MultiPeerActionProcessorFactory.GroupActionProcessorFactory.INSTANCE.createPreJoinActionProcessor(webRtcInteractor);
    } else {
      processor = new PreJoinActionProcessor(webRtcInteractor);
    }

    currentState = WebRtcVideoUtil.initializeVanityCamera(WebRtcVideoUtil.initializeVideo(context,
                                                                                          webRtcInteractor.getCameraEventListener(),
                                                                                          currentState,
                                                                                          isGroupCall ? RemotePeer.GROUP_CALL_ID.longValue()
                                                                                                      : EglBaseWrapper.OUTGOING_PLACEHOLDER));

    currentState = currentState.builder()
                               .actionProcessor(processor)
                               .changeCallInfoState()
                               .callState(WebRtcViewModel.State.CALL_PRE_JOIN)
                               .callRecipient(remotePeer.getRecipient())
                               .build();

    return isGroupCall ? currentState.getActionProcessor().handlePreJoinCall(currentState, remotePeer)
                       : currentState;
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

    int groupSize = remotePeerGroup.getRecipient().getParticipantIds().size();
    if (groupSize > RemoteConfig.maxGroupCallRingSize()) {
      Log.w(TAG, "Received ring request for large group, dropping. size: " + groupSize + " max: " + RemoteConfig.maxGroupCallRingSize());
      return currentState;
    }

    if (ringUpdate != CallManager.RingUpdate.REQUESTED) {
      ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromRingState(ringId, remotePeerGroup.getId(), sender, System.currentTimeMillis(), ringUpdate);
      return currentState;
    } else if (ZonaRosaDatabase.calls().isRingCancelled(ringId, remotePeerGroup.getId())) {
      try {
        Log.i(TAG, "Incoming ring request for already cancelled ring: " + ringId);
        webRtcInteractor.getCallManager().cancelGroupRing(groupId.getDecodedId(), ringId, null);
      } catch (CallException e) {
        Log.w(TAG, "Error while trying to cancel ring: " + ringId, e);
      }
      return currentState;
    }

    NotificationProfile activeProfile = NotificationProfiles.getActiveProfile(ZonaRosaDatabase.notificationProfiles().getProfiles());
    if (activeProfile != null && !(activeProfile.isRecipientAllowed(remotePeerGroup.getId()) || activeProfile.getAllowAllCalls())) {
      try {
        Log.i(TAG, "Incoming ring request for profile restricted recipient");
        ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromRingState(ringId, remotePeerGroup.getId(), sender, System.currentTimeMillis(), CallManager.RingUpdate.EXPIRED_REQUEST, true);
        webRtcInteractor.getCallManager().cancelGroupRing(groupId.getDecodedId(), ringId, CallManager.RingCancelReason.DeclinedByUser);
      } catch (CallException e) {
        Log.w(TAG, "Error while trying to cancel ring: " + ringId, e);
      }
      return currentState;
    }

    webRtcInteractor.peekGroupCallForRingingCheck(new GroupCallRingCheckInfo(remotePeerGroup.getId(), groupId, ringId, sender, ringUpdate));

    return currentState;
  }

  @Override
  protected @NonNull WebRtcServiceState handleReceivedGroupCallPeekForRingingCheck(@NonNull WebRtcServiceState currentState, @NonNull GroupCallRingCheckInfo info, @NonNull PeekInfo peekInfo) {
    Log.i(tag, "handleReceivedGroupCallPeekForRingingCheck(): recipient: " + info.getRecipientId() + " ring: " + info.getRingId());

    if (ZonaRosaDatabase.calls().isRingCancelled(info.getRingId(), info.getRecipientId())) {
      try {
        Log.i(TAG, "Ring was cancelled while getting peek info ring: " + info.getRingId());
        webRtcInteractor.getCallManager().cancelGroupRing(info.getGroupId().getDecodedId(), info.getRingId(), null);
      } catch (CallException e) {
        Log.w(TAG, "Error while trying to cancel ring: " + info.getRingId(), e);
      }
      return currentState;
    }

    if (peekInfo.getDeviceCount() == 0) {
      Log.i(TAG, "No one in the group call, mark as expired and do not ring");
      ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromRingState(info.getRingId(), info.getRecipientId(), info.getRingerAci(), System.currentTimeMillis(), CallManager.RingUpdate.EXPIRED_REQUEST);
      return currentState;
    } else if (peekInfo.getJoinedMembers().contains(Recipient.self().requireServiceId().getRawUuid())) {
      Log.i(TAG, "We are already in the call, mark accepted on another device and do not ring");
      ZonaRosaDatabase.calls().insertOrUpdateGroupCallFromRingState(info.getRingId(), info.getRecipientId(), info.getRingerAci(), System.currentTimeMillis(), CallManager.RingUpdate.ACCEPTED_ON_ANOTHER_DEVICE);
      return currentState;
    }

    currentState = currentState.builder()
                               .actionProcessor(new IncomingGroupCallActionProcessor(webRtcInteractor))
                               .build();

    return currentState.getActionProcessor().handleGroupCallRingUpdate(currentState, new RemotePeer(info.getRecipientId()), info.getGroupId(), info.getRingId(), info.getRingerAci(), info.getRingUpdate());
  }
}

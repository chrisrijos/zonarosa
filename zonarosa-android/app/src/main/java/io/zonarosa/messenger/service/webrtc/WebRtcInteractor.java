package io.zonarosa.messenger.service.webrtc;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.ringrtc.CallId;
import io.zonarosa.ringrtc.CallManager;
import io.zonarosa.ringrtc.GroupCall;
import io.zonarosa.messenger.database.CallTable;
import io.zonarosa.messenger.groups.GroupId;
import io.zonarosa.messenger.recipients.Recipient;
import io.zonarosa.messenger.recipients.RecipientId;
import io.zonarosa.messenger.ringrtc.CameraEventListener;
import io.zonarosa.messenger.ringrtc.RemotePeer;
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceState;
import io.zonarosa.messenger.util.AppForegroundObserver;
import io.zonarosa.messenger.webrtc.audio.AudioManagerCommand;
import io.zonarosa.messenger.webrtc.audio.ZonaRosaAudioManager;
import io.zonarosa.messenger.webrtc.locks.LockManager;
import io.zonarosa.service.api.messages.calls.ZonaRosaServiceCallMessage;

import java.util.Collection;
import java.util.UUID;

/**
 * Serves as the bridge between the action processing framework as the WebRTC service. Attempts
 * to minimize direct access to various managers by providing a simple proxy to them. Due to the
 * heavy use of {@link CallManager} throughout, it was exempted from the rule.
 */
public class WebRtcInteractor {

  private final Context                        context;
  private final ZonaRosaCallManager              zonarosaCallManager;
  private final LockManager                    lockManager;
  private final CameraEventListener            cameraEventListener;
  private final GroupCall.Observer             groupCallObserver;
  private final AppForegroundObserver.Listener foregroundListener;

  public WebRtcInteractor(@NonNull Context context,
                          @NonNull ZonaRosaCallManager zonarosaCallManager,
                          @NonNull LockManager lockManager,
                          @NonNull CameraEventListener cameraEventListener,
                          @NonNull GroupCall.Observer groupCallObserver,
                          @NonNull AppForegroundObserver.Listener foregroundListener)
  {
    this.context             = context;
    this.zonarosaCallManager   = zonarosaCallManager;
    this.lockManager         = lockManager;
    this.cameraEventListener = cameraEventListener;
    this.groupCallObserver   = groupCallObserver;
    this.foregroundListener  = foregroundListener;
  }

  @NonNull Context getContext() {
    return context;
  }

  @NonNull CameraEventListener getCameraEventListener() {
    return cameraEventListener;
  }

  @NonNull CallManager getCallManager() {
    return zonarosaCallManager.getRingRtcCallManager();
  }

  @NonNull GroupCall.Observer getGroupCallObserver() {
    return groupCallObserver;
  }

  @NonNull AppForegroundObserver.Listener getForegroundListener() {
    return foregroundListener;
  }

  void updatePhoneState(@NonNull LockManager.PhoneState phoneState) {
    lockManager.updatePhoneState(phoneState);
  }

  void postStateUpdate(@NonNull WebRtcServiceState state) {
    zonarosaCallManager.postStateUpdate(state);
  }

  void sendCallMessage(@NonNull RemotePeer remotePeer, @NonNull ZonaRosaServiceCallMessage callMessage) {
    zonarosaCallManager.sendCallMessage(remotePeer, callMessage);
  }

  void sendGroupCallMessage(@NonNull Recipient recipient, @Nullable String groupCallEraId, @Nullable CallId callId, boolean isIncoming, boolean isJoinEvent) {
    zonarosaCallManager.sendGroupCallUpdateMessage(recipient, groupCallEraId, callId, isIncoming, isJoinEvent);
  }

  void updateGroupCallUpdateMessage(@NonNull RecipientId groupId, @Nullable String groupCallEraId, @NonNull Collection<UUID> joinedMembers, boolean isCallFull) {
    zonarosaCallManager.updateGroupCallUpdateMessage(groupId, groupCallEraId, joinedMembers, isCallFull);
  }

  void setCallInProgressNotification(int type, @NonNull RemotePeer remotePeer, boolean isVideoCall) {
    ActiveCallManager.update(context, type, remotePeer.getRecipient().getId(), isVideoCall);
  }

  void setCallInProgressNotification(int type, @NonNull Recipient recipient, boolean isVideoCall) {
    ActiveCallManager.update(context, type, recipient.getId(), isVideoCall);
  }

  void retrieveTurnServers(@NonNull RemotePeer remotePeer) {
    zonarosaCallManager.retrieveTurnServers(remotePeer);
  }

  void stopForegroundService() {
    ActiveCallManager.stop();
  }

  void insertMissedCall(@NonNull RemotePeer remotePeer, long timestamp, boolean isVideoOffer) {
    insertMissedCall(remotePeer, timestamp, isVideoOffer, CallTable.Event.MISSED);
  }

  void insertMissedCall(@NonNull RemotePeer remotePeer, long timestamp, boolean isVideoOffer, @NonNull CallTable.Event missedEvent) {
    zonarosaCallManager.insertMissedCall(remotePeer, timestamp, isVideoOffer, missedEvent);
  }

  void insertReceivedCall(@NonNull RemotePeer remotePeer, boolean isVideoOffer) {
    zonarosaCallManager.insertReceivedCall(remotePeer, isVideoOffer);
  }

  boolean startWebRtcCallActivityIfPossible() {
    return zonarosaCallManager.startCallCardActivityIfPossible();
  }

  void registerPowerButtonReceiver() {
    ActiveCallManager.changePowerButtonReceiver(context, true);
  }

  void unregisterPowerButtonReceiver() {
    ActiveCallManager.changePowerButtonReceiver(context, false);
  }

  void silenceIncomingRinger() {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.SilenceIncomingRinger());
  }

  void initializeAudioForCall() {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.Initialize());
  }

  void startIncomingRinger(@Nullable Uri ringtoneUri, boolean vibrate) {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.StartIncomingRinger(ringtoneUri, vibrate));
  }

  void startOutgoingRinger() {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.StartOutgoingRinger());
  }

  void stopAudio(boolean playDisconnect) {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.Stop(playDisconnect));
  }

  void startAudioCommunication() {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.Start());
  }

  public void setUserAudioDevice(@Nullable RecipientId recipientId, @NonNull ZonaRosaAudioManager.ChosenAudioDeviceIdentifier userDevice) {
    if (userDevice.isLegacy()) {
      ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.SetUserDevice(recipientId, userDevice.getDesiredAudioDeviceLegacy().ordinal(), false));
    } else {
      ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.SetUserDevice(recipientId, userDevice.getDesiredAudioDevice31(), true));
    }
  }

  public void setDefaultAudioDevice(@NonNull RecipientId recipientId, @NonNull ZonaRosaAudioManager.AudioDevice userDevice, boolean clearUserEarpieceSelection) {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.SetDefaultDevice(recipientId, userDevice, clearUserEarpieceSelection));
  }

  public void playStateChangeUp() {
    ActiveCallManager.sendAudioManagerCommand(context, new AudioManagerCommand.PlayStateChangeUp());
  }

  void peekGroupCallForRingingCheck(@NonNull GroupCallRingCheckInfo groupCallRingCheckInfo) {
    zonarosaCallManager.peekGroupCallForRingingCheck(groupCallRingCheckInfo);
  }

  public void activateCall(RecipientId recipientId) {
    AndroidTelecomUtil.activateCall(recipientId);
  }

  public void terminateCall(RecipientId recipientId) {
    AndroidTelecomUtil.terminateCall(recipientId);
  }

  public boolean addNewIncomingCall(RecipientId recipientId, long callId, boolean remoteVideoOffer) {
    return AndroidTelecomUtil.addIncomingCall(recipientId, callId, remoteVideoOffer);
  }

  public void rejectIncomingCall(RecipientId recipientId) {
    AndroidTelecomUtil.reject(recipientId);
  }

  public boolean addNewOutgoingCall(RecipientId recipientId, long callId, boolean isVideoCall) {
    return AndroidTelecomUtil.addOutgoingCall(recipientId, callId, isVideoCall);
  }

  public void requestGroupMembershipProof(GroupId.V2 groupId, int groupCallHashCode) {
    zonarosaCallManager.requestGroupMembershipToken(groupId, groupCallHashCode);
  }

  public void sendAcceptedCallEventSyncMessage(@NonNull RemotePeer remotePeer, boolean isOutgoing, boolean isVideoCall) {
    zonarosaCallManager.sendAcceptedCallEventSyncMessage(remotePeer, isOutgoing, isVideoCall);
  }

  public void sendNotAcceptedCallEventSyncMessage(@NonNull RemotePeer remotePeer, boolean isOutgoing, boolean isVideoCall) {
    zonarosaCallManager.sendNotAcceptedCallEventSyncMessage(remotePeer, isOutgoing, isVideoCall);
  }

  public void sendGroupCallNotAcceptedCallEventSyncMessage(@NonNull RemotePeer remotePeer, boolean isOutgoing) {
    zonarosaCallManager.sendGroupCallNotAcceptedCallEventSyncMessage(remotePeer, isOutgoing);
  }
}

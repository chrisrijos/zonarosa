package io.zonarosa.messenger.service.webrtc;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.zonarosa.libzonarosa.protocol.InvalidKeyException;
import io.zonarosa.libzonarosa.protocol.ecc.ECPublicKey;
import io.zonarosa.ringrtc.CallManager;
import io.zonarosa.ringrtc.GroupCall;
import io.zonarosa.ringrtc.PeekInfo;
import io.zonarosa.messenger.dependencies.AppDependencies;
import io.zonarosa.messenger.events.WebRtcViewModel;
import io.zonarosa.messenger.service.webrtc.state.WebRtcServiceState;
import io.zonarosa.messenger.webrtc.audio.AudioManagerCompat;
import io.zonarosa.messenger.webrtc.audio.ZonaRosaAudioManager;
import io.zonarosa.messenger.webrtc.locks.LockManager;
import io.zonarosa.service.api.messages.calls.HangupMessage;
import io.zonarosa.service.api.messages.calls.OfferMessage;
import io.zonarosa.service.api.messages.calls.OpaqueMessage;

/**
 * Calling specific helpers.
 */
public final class WebRtcUtil {

  private WebRtcUtil() {}

  public static @NonNull byte[] getPublicKeyBytes(@NonNull byte[] identityKey) throws InvalidKeyException {
    ECPublicKey key = new ECPublicKey(identityKey);
    return key.getPublicKeyBytes();
  }

  public static @NonNull LockManager.PhoneState getInCallPhoneState(@NonNull Context context) {
    AudioManagerCompat audioManager = AppDependencies.getAndroidCallAudioManager();
    if (audioManager.isSpeakerphoneOn() || audioManager.isBluetoothConnected() || audioManager.isWiredHeadsetOn()) {
      return LockManager.PhoneState.IN_HANDS_FREE_CALL;
    } else {
      return LockManager.PhoneState.IN_CALL;
    }
  }

  /**
   * Returns the appropriate phone state for an in-call scenario, considering both local and remote video state.
   * If either local or remote video is enabled, returns {@link LockManager.PhoneState#IN_VIDEO} to keep the screen on.
   * Otherwise, falls back to audio-device based phone state.
   */
  public static @NonNull LockManager.PhoneState getInCallPhoneState(@NonNull Context context, boolean localVideoEnabled, boolean remoteVideoEnabled) {
    if (localVideoEnabled || remoteVideoEnabled) {
      return LockManager.PhoneState.IN_VIDEO;
    }
    return getInCallPhoneState(context);
  }

  public static @NonNull CallManager.CallMediaType getCallMediaTypeFromOfferType(@NonNull OfferMessage.Type offerType) {
    return offerType == OfferMessage.Type.VIDEO_CALL ? CallManager.CallMediaType.VIDEO_CALL : CallManager.CallMediaType.AUDIO_CALL;
  }

  public static @NonNull OfferMessage.Type getOfferTypeFromCallMediaType(@Nullable CallManager.CallMediaType callMediaType) {
    return callMediaType == CallManager.CallMediaType.VIDEO_CALL ? OfferMessage.Type.VIDEO_CALL : OfferMessage.Type.AUDIO_CALL;
  }

  public static @NonNull HangupMessage.Type getHangupTypeFromCallHangupType(@NonNull CallManager.HangupType hangupType) {
    switch (hangupType) {
      case ACCEPTED:
        return HangupMessage.Type.ACCEPTED;
      case BUSY:
        return HangupMessage.Type.BUSY;
      case NORMAL:
        return HangupMessage.Type.NORMAL;
      case DECLINED:
        return HangupMessage.Type.DECLINED;
      case NEED_PERMISSION:
        return HangupMessage.Type.NEED_PERMISSION;
      default:
        throw new IllegalArgumentException("Unexpected hangup type: " + hangupType);
    }
  }

  public static OpaqueMessage.Urgency getUrgencyFromCallUrgency(@NonNull CallManager.CallMessageUrgency urgency) {
    if (urgency == CallManager.CallMessageUrgency.HANDLE_IMMEDIATELY) {
      return OpaqueMessage.Urgency.HANDLE_IMMEDIATELY;
    }
    return OpaqueMessage.Urgency.DROPPABLE;
  }

  public static void enableSpeakerPhoneIfNeeded(@NonNull WebRtcInteractor webRtcInteractor, WebRtcServiceState currentState) {
    if (!currentState.getLocalDeviceState().getCameraState().isEnabled()) {
      return;
    }

    if (currentState.getLocalDeviceState().getActiveDevice() == ZonaRosaAudioManager.AudioDevice.EARPIECE ||
        currentState.getLocalDeviceState().getActiveDevice() == ZonaRosaAudioManager.AudioDevice.NONE &&
        currentState.getCallInfoState().getActivePeer() != null)
    {
      webRtcInteractor.setDefaultAudioDevice(currentState.getCallInfoState().requireActivePeer().getId(), ZonaRosaAudioManager.AudioDevice.SPEAKER_PHONE, true);
    }
  }

  public static @NonNull WebRtcViewModel.GroupCallState groupCallStateForConnection(@NonNull GroupCall.ConnectionState connectionState) {
    switch (connectionState) {
      case CONNECTING:
        return WebRtcViewModel.GroupCallState.CONNECTING;
      case CONNECTED:
        return WebRtcViewModel.GroupCallState.CONNECTED;
      case RECONNECTING:
        return WebRtcViewModel.GroupCallState.RECONNECTING;
      default:
        return WebRtcViewModel.GroupCallState.DISCONNECTED;
    }
  }

  public static @Nullable String getGroupCallEraId(@Nullable GroupCall groupCall) {
    if (groupCall == null) {
      return null;
    }

    PeekInfo peekInfo = groupCall.getPeekInfo();
    return peekInfo != null ? peekInfo.getEraId() : null;
  }

  public static boolean isCallFull(@Nullable PeekInfo peekInfo) {
    return peekInfo != null && peekInfo.getMaxDevices() != null && peekInfo.getDeviceCount() >= peekInfo.getMaxDevices();
  }
}

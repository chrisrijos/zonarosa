package io.zonarosa.messenger.service.webrtc.state

import io.zonarosa.messenger.components.sensors.Orientation
import io.zonarosa.messenger.events.CallParticipant
import io.zonarosa.messenger.ringrtc.CameraState
import io.zonarosa.messenger.webrtc.audio.ZonaRosaAudioManager
import org.webrtc.PeerConnection

/**
 * Local device specific state.
 */
data class LocalDeviceState(
  var cameraState: CameraState = CameraState.UNKNOWN,
  var isMicrophoneEnabled: Boolean = true,
  var orientation: Orientation = Orientation.PORTRAIT_BOTTOM_EDGE,
  var isLandscapeEnabled: Boolean = false,
  var deviceOrientation: Orientation = Orientation.PORTRAIT_BOTTOM_EDGE,
  var activeDevice: ZonaRosaAudioManager.AudioDevice = ZonaRosaAudioManager.AudioDevice.NONE,
  var availableDevices: Set<ZonaRosaAudioManager.AudioDevice> = emptySet(),
  var bluetoothPermissionDenied: Boolean = false,
  var isAudioDeviceChangePending: Boolean = false,
  var networkConnectionType: PeerConnection.AdapterType = PeerConnection.AdapterType.UNKNOWN,
  var handRaisedTimestamp: Long = CallParticipant.HAND_LOWERED,
  var remoteMutedBy: CallParticipant? = null
) {

  fun duplicate(): LocalDeviceState {
    return copy()
  }
}

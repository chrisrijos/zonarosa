package io.zonarosa.messenger.service.webrtc

import android.content.Context
import android.content.Intent
import android.telecom.CallAudioState
import android.telecom.Connection
import androidx.annotation.RequiresApi
import io.zonarosa.core.ui.permissions.Permissions
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.components.webrtc.v2.CallIntent
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.recipients.RecipientId
import io.zonarosa.messenger.webrtc.CallNotificationBuilder
import io.zonarosa.messenger.webrtc.audio.AudioManagerCommand
import io.zonarosa.messenger.webrtc.audio.ZonaRosaAudioManager

/**
 * ZonaRosa implementation for the telecom system connection. Provides an interaction point for the system to
 * inform us about changes in the telecom system. Created and returned by [AndroidCallConnectionService].
 */
@RequiresApi(26)
class AndroidCallConnection(
  private val context: Context,
  private val recipientId: RecipientId,
  isOutgoing: Boolean,
  private val isVideoCall: Boolean
) : Connection() {

  private var needToResetAudioRoute = isOutgoing && !isVideoCall
  private var initialAudioRoute: ZonaRosaAudioManager.AudioDevice? = null

  init {
    connectionProperties = PROPERTY_SELF_MANAGED
    connectionCapabilities = CAPABILITY_SUPPORTS_VT_LOCAL_BIDIRECTIONAL or
      CAPABILITY_SUPPORTS_VT_REMOTE_BIDIRECTIONAL or
      CAPABILITY_MUTE
  }

  override fun onShowIncomingCallUi() {
    Log.i(TAG, "onShowIncomingCallUi()")
    ActiveCallManager.update(context, CallNotificationBuilder.TYPE_INCOMING_CONNECTING, recipientId, isVideoCall)
    setRinging()
  }

  override fun onCallAudioStateChanged(state: CallAudioState) {
    Log.i(TAG, "onCallAudioStateChanged($state)")

    val activeDevice = state.route.toDevices().firstOrNull() ?: ZonaRosaAudioManager.AudioDevice.EARPIECE
    val availableDevices = state.supportedRouteMask.toDevices()

    AppDependencies.zonarosaCallManager.onAudioDeviceChanged(activeDevice, availableDevices)

    if (needToResetAudioRoute) {
      if (initialAudioRoute == null) {
        initialAudioRoute = activeDevice
      } else if (activeDevice == ZonaRosaAudioManager.AudioDevice.SPEAKER_PHONE) {
        Log.i(TAG, "Resetting audio route from SPEAKER_PHONE to $initialAudioRoute")
        AndroidTelecomUtil.selectAudioDevice(recipientId, initialAudioRoute!!)
        needToResetAudioRoute = false
      }
    }
  }

  override fun onAnswer(videoState: Int) {
    Log.i(TAG, "onAnswer($videoState)")
    if (Permissions.hasAll(context, android.Manifest.permission.RECORD_AUDIO)) {
      AppDependencies.zonarosaCallManager.acceptCall(false)
    } else {
      val intent = CallIntent.Builder(context)
        .withAddedIntentFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        .withAction(if (isVideoCall) CallIntent.Action.ANSWER_VIDEO else CallIntent.Action.ANSWER_AUDIO)
        .build()
      context.startActivity(intent)
    }
  }

  override fun onSilence() {
    ActiveCallManager.sendAudioManagerCommand(context, AudioManagerCommand.SilenceIncomingRinger())
  }

  override fun onReject() {
    Log.i(TAG, "onReject()")
    ActiveCallManager.denyCall()
  }

  override fun onDisconnect() {
    Log.i(TAG, "onDisconnect()")
    ActiveCallManager.hangup()
  }

  companion object {
    private val TAG: String = Log.tag(AndroidCallConnection::class.java)
  }
}

private fun Int.toDevices(): Set<ZonaRosaAudioManager.AudioDevice> {
  val devices = mutableSetOf<ZonaRosaAudioManager.AudioDevice>()

  if (this and CallAudioState.ROUTE_BLUETOOTH != 0) {
    devices += ZonaRosaAudioManager.AudioDevice.BLUETOOTH
  }

  if (this and CallAudioState.ROUTE_EARPIECE != 0) {
    devices += ZonaRosaAudioManager.AudioDevice.EARPIECE
  }

  if (this and CallAudioState.ROUTE_WIRED_HEADSET != 0) {
    devices += ZonaRosaAudioManager.AudioDevice.WIRED_HEADSET
  }

  if (this and CallAudioState.ROUTE_SPEAKER != 0) {
    devices += ZonaRosaAudioManager.AudioDevice.SPEAKER_PHONE
  }

  return devices
}

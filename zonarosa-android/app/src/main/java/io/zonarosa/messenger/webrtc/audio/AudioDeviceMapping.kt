package io.zonarosa.messenger.webrtc.audio

import android.media.AudioDeviceInfo
import androidx.annotation.RequiresApi

@RequiresApi(31)
object AudioDeviceMapping {

  private val systemDeviceTypeMap: Map<ZonaRosaAudioManager.AudioDevice, List<Int>> = mapOf(
    ZonaRosaAudioManager.AudioDevice.BLUETOOTH to listOf(AudioDeviceInfo.TYPE_BLUETOOTH_SCO, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_HEARING_AID),
    ZonaRosaAudioManager.AudioDevice.EARPIECE to listOf(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE),
    ZonaRosaAudioManager.AudioDevice.SPEAKER_PHONE to listOf(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE),
    ZonaRosaAudioManager.AudioDevice.WIRED_HEADSET to listOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET),
    ZonaRosaAudioManager.AudioDevice.NONE to emptyList()
  )

  @JvmStatic
  fun getEquivalentPlatformTypes(audioDevice: ZonaRosaAudioManager.AudioDevice): List<Int> {
    return systemDeviceTypeMap[audioDevice]!!
  }

  @JvmStatic
  fun fromPlatformType(type: Int): ZonaRosaAudioManager.AudioDevice {
    for (kind in ZonaRosaAudioManager.AudioDevice.entries) {
      if (getEquivalentPlatformTypes(kind).contains(type)) return kind
    }
    return ZonaRosaAudioManager.AudioDevice.NONE
  }
}

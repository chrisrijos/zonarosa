package io.zonarosa.messenger.service.webrtc

import android.os.Build
import io.zonarosa.core.util.asListContains
import io.zonarosa.ringrtc.AudioConfig
import io.zonarosa.messenger.keyvalue.ZonaRosaStore
import io.zonarosa.messenger.util.RemoteConfig
import io.zonarosa.messenger.webrtc.audio.AudioDeviceConfig

/**
 * Utility class to determine the audio configuration that RingRTC should use.
 */
object RingRtcDynamicConfiguration {
  private var lastFetchTime: Long = 0

  fun isTelecomAllowedForDevice(): Boolean {
    return RemoteConfig.telecomManufacturerAllowList.lowercase().asListContains(Build.MANUFACTURER.lowercase()) &&
      !RemoteConfig.telecomModelBlocklist.lowercase().asListContains(Build.MODEL.lowercase())
  }

  @JvmStatic
  fun getAudioConfig(): AudioConfig {
    if (RemoteConfig.internalUser && ZonaRosaStore.internal.callingSetAudioConfig) {
      // Use the internal audio settings.
      var audioConfig = AudioConfig()
      audioConfig.useOboe = ZonaRosaStore.internal.callingUseOboeAdm
      audioConfig.useSoftwareAec = ZonaRosaStore.internal.callingUseSoftwareAec
      audioConfig.useSoftwareNs = ZonaRosaStore.internal.callingUseSoftwareNs
      audioConfig.useInputLowLatency = ZonaRosaStore.internal.callingUseInputLowLatency
      audioConfig.useInputVoiceComm = ZonaRosaStore.internal.callingUseInputVoiceComm

      return audioConfig
    }

    // Use the audio settings provided by the remote configuration.
    if (lastFetchTime != ZonaRosaStore.remoteConfig.lastFetchTime) {
      // The remote config has been updated.
      AudioDeviceConfig.refresh()
      lastFetchTime = ZonaRosaStore.remoteConfig.lastFetchTime
    }
    return AudioDeviceConfig.getCurrentConfig()
  }
}

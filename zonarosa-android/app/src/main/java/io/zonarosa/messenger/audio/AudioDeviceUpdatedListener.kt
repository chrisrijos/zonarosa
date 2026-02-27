/*
 * Copyright 2023 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.audio

/**
 * A listener for when audio devices are added or removed, for example if a wired headset is plugged/unplugged or Bluetooth connected/disconnected.
 */
interface AudioDeviceUpdatedListener {
  fun onAudioDeviceUpdated()
}

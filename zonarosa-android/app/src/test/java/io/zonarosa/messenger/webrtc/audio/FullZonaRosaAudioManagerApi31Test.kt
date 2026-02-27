/*
 * Copyright 2026 ZonaRosa Platform
 * SPDX-License-Identifier: MIT-3.0-only
 */

package io.zonarosa.messenger.webrtc.audio

import android.app.Application
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.SoundPool
import assertk.assertThat
import assertk.assertions.isTrue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import io.zonarosa.core.util.logging.Log
import io.zonarosa.messenger.dependencies.AppDependencies
import io.zonarosa.messenger.testutil.MockAppDependenciesRule
import io.zonarosa.messenger.testutil.SystemOutLogger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, application = Application::class, sdk = [31])
class FullZonaRosaAudioManagerApi31Test {

  companion object {
    @JvmStatic
    @BeforeClass
    fun setUpClass() {
      Log.initialize(SystemOutLogger())
    }
  }

  @get:Rule
  val appDependencies = MockAppDependenciesRule()

  private lateinit var androidAudioManager: AudioManagerCompat
  private lateinit var eventListener: ZonaRosaAudioManager.EventListener

  @Before
  fun setUp() {
    androidAudioManager = AppDependencies.androidCallAudioManager
    eventListener = mockk(relaxed = true)

    val soundPool: SoundPool = mockk(relaxed = true)
    every { androidAudioManager.createSoundPool() } returns soundPool
    every { soundPool.load(any<Context>(), any(), any()) } returns 1
  }

  @Test
  fun `reasserts user selected device when Android reports a different communication device`() {
    val userSelectedDevice = createDevice(10, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "Phone speaker")
    val systemChangedDevice = createDevice(11, AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, "Phone earpiece")

    var currentCommunicationDevice: AudioDeviceInfo? = systemChangedDevice
    every { androidAudioManager.communicationDevice } answers { currentCommunicationDevice }
    every { androidAudioManager.availableCommunicationDevices } returns listOf(userSelectedDevice, systemChangedDevice)
    every { androidAudioManager.setCommunicationDevice(any()) } answers {
      currentCommunicationDevice = firstArg()
      true
    }

    val manager = FullZonaRosaAudioManagerApi31(AppDependencies.application, eventListener)

    try {
      setState(manager, ZonaRosaAudioManager.State.RUNNING)
      setUserSelectedAudioDevice(manager, userSelectedDevice)

      clearMocks(androidAudioManager, answers = false, recordedCalls = true)
      clearMocks(eventListener, answers = false, recordedCalls = true)

      triggerCommunicationDeviceChanged(manager, systemChangedDevice)

      verify(timeout = 2_000) { androidAudioManager.setCommunicationDevice(userSelectedDevice) }
      verify(timeout = 2_000) {
        eventListener.onAudioDeviceChanged(
          ZonaRosaAudioManager.AudioDevice.SPEAKER_PHONE,
          setOf(ZonaRosaAudioManager.AudioDevice.SPEAKER_PHONE, ZonaRosaAudioManager.AudioDevice.EARPIECE)
        )
      }
      verify(exactly = 0) { eventListener.onAudioDeviceChangeFailed() }
    } finally {
      shutdownManager(manager)
    }
  }

  @Test
  fun `does not reassert when Android reports the same user selected communication device`() {
    val selectedDevice = createDevice(20, AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, "Phone speaker")

    every { androidAudioManager.communicationDevice } returns selectedDevice
    every { androidAudioManager.availableCommunicationDevices } returns listOf(selectedDevice)

    val manager = FullZonaRosaAudioManagerApi31(AppDependencies.application, eventListener)

    try {
      setState(manager, ZonaRosaAudioManager.State.RUNNING)
      setUserSelectedAudioDevice(manager, selectedDevice)

      clearMocks(androidAudioManager, answers = false, recordedCalls = true)
      clearMocks(eventListener, answers = false, recordedCalls = true)

      triggerCommunicationDeviceChanged(manager, selectedDevice)

      verify(exactly = 0) { androidAudioManager.setCommunicationDevice(any()) }
      verify(exactly = 0) { eventListener.onAudioDeviceChanged(any(), any()) }
      verify(exactly = 0) { eventListener.onAudioDeviceChangeFailed() }
    } finally {
      shutdownManager(manager)
    }
  }

  private fun createDevice(id: Int, type: Int, productName: String): AudioDeviceInfo {
    return mockk {
      every { this@mockk.id } returns id
      every { this@mockk.type } returns type
      every { this@mockk.productName } returns productName
    }
  }

  private fun triggerCommunicationDeviceChanged(manager: FullZonaRosaAudioManagerApi31, device: AudioDeviceInfo) {
    val listenerField = FullZonaRosaAudioManagerApi31::class.java.getDeclaredField("communicationDeviceChangedListener")
    listenerField.isAccessible = true
    val listener = listenerField.get(manager) as AudioManager.OnCommunicationDeviceChangedListener

    val handlerField = ZonaRosaAudioManager::class.java.getDeclaredField("handler")
    handlerField.isAccessible = true
    val handler = handlerField.get(manager) as ZonaRosaAudioHandler

    val latch = CountDownLatch(1)
    val posted = handler.post {
      listener.onCommunicationDeviceChanged(device)
      latch.countDown()
    }

    assertThat(posted).isTrue()
    assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue()
  }

  private fun setState(manager: FullZonaRosaAudioManagerApi31, state: ZonaRosaAudioManager.State) {
    val stateField = ZonaRosaAudioManager::class.java.getDeclaredField("state")
    stateField.isAccessible = true
    stateField.set(manager, state)
  }

  private fun setUserSelectedAudioDevice(manager: FullZonaRosaAudioManagerApi31, device: AudioDeviceInfo?) {
    val userSelectedField = FullZonaRosaAudioManagerApi31::class.java.getDeclaredField("userSelectedAudioDevice")
    userSelectedField.isAccessible = true
    userSelectedField.set(manager, device)
  }

  private fun shutdownManager(manager: FullZonaRosaAudioManagerApi31) {
    setState(manager, ZonaRosaAudioManager.State.UNINITIALIZED)
    setUserSelectedAudioDevice(manager, null)
    manager.shutdown()

    val threadField = ZonaRosaAudioManager::class.java.getDeclaredField("commandAndControlThread")
    threadField.isAccessible = true

    val start = System.nanoTime()
    while (threadField.get(manager) != null && TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) <= 2_000) {
      Thread.sleep(10)
    }
  }
}

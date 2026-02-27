package io.zonarosa.baselineprofile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until

object BenchmarkSetup {
  fun setup(type: String, device: UiDevice) {
    device.executeShellCommand("am start -W -n io.zonarosa.messenger/io.zonarosa.benchmark.BenchmarkSetupActivity --es setup-type $type")
    device.wait(Until.hasObject(By.textContains("done")), 25_000L)
  }
}

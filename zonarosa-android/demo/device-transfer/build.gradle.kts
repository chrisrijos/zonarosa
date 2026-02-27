plugins {
  id("zonarosa-sample-app")
  alias(libs.plugins.compose.compiler)
}

android {
  namespace = "io.zonarosa.devicetransfer.app"

  defaultConfig {
    applicationId = "io.zonarosa.devicetransfer.app"

    ndk {
      abiFilters += setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
    }

    buildConfigField("String", "LIBZONAROSA_VERSION", "\"libzonarosa ${libs.versions.libzonarosa.client.get()}\"")
  }
}

dependencies {
  implementation(project(":lib:device-transfer"))
}
